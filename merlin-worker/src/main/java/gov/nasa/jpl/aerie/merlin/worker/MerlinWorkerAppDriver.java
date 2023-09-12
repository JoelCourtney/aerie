package gov.nasa.jpl.aerie.merlin.worker;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.config.PostgresStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresMissionModelRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresPlanRevisionData;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.services.LocalMissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import gov.nasa.jpl.aerie.merlin.worker.postgres.PostgresSimulationNotificationPayload;
import io.javalin.Javalin;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

public final class MerlinWorkerAppDriver {
  public static boolean defaultUseResourceTracker = false;

  public static void main(String[] args) throws InterruptedException {
    final var configuration = loadConfiguration();
    final var store = configuration.store();

    if (!(store instanceof final PostgresStore postgresStore)) {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
    final var hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
    hikariConfig.addDataSourceProperty("serverName", postgresStore.server());
    hikariConfig.addDataSourceProperty("portNumber", postgresStore.port());
    hikariConfig.addDataSourceProperty("databaseName", postgresStore.database());
    hikariConfig.addDataSourceProperty("applicationName", "Merlin Server");
    hikariConfig.setUsername(postgresStore.user());
    hikariConfig.setPassword(postgresStore.password());
    hikariConfig.setMaximumPoolSize(2);

    hikariConfig.setConnectionInitSql("set time zone 'UTC'");

    final var hikariDataSource = new HikariDataSource(hikariConfig);

    final var stores = new Stores(
        new PostgresPlanRepository(hikariDataSource),
        new PostgresMissionModelRepository(hikariDataSource),
        new PostgresResultsCellRepository(hikariDataSource));

    final var missionModelController = new LocalMissionModelService(
        configuration.merlinFileStore(),
        stores.missionModels(),
        configuration.untruePlanStart()
    );
    final var planController = new LocalPlanService(stores.plans());
    final var simulationAgent = new SynchronousSimulationAgent(planController, missionModelController, configuration.simulationProgressPollPeriodMillis(), configuration.useResourceTracker());

    final var notificationQueue = new LinkedBlockingQueue<PostgresSimulationNotificationPayload>();
    final var listenAction = new ListenSimulationCapability(hikariDataSource, notificationQueue);
    listenAction.registerListener();

    final var app = Javalin.create().start(8080);
    app.get("/health", ctx -> ctx.status(200));

    while (true) {
      final var notification = notificationQueue.take();
      final var planId = new PlanId(notification.planId());
      final var datasetId = notification.datasetId();

      final Optional<ResultsProtocol.OwnerRole> owner = stores.results().claim(planId, datasetId);
      if (owner.isEmpty()) continue;

      final var revisionData = new PostgresPlanRevisionData(
          notification.modelRevision(),
          notification.planRevision(),
          notification.simulationRevision(),
          notification.simulationTemplateRevision());
      final ResultsProtocol.WriterRole writer = owner.get();
      try {
        simulationAgent.simulate(planId, revisionData, writer);
      } catch (final Throwable ex) {
        ex.printStackTrace(System.err);
        writer.failWith(b -> b
            .type("UNEXPECTED_SIMULATION_EXCEPTION")
            .message("Something went wrong while simulating")
            .trace(ex));
      }
    }
  }

  private static String getEnv(final String key, final String fallback){
    final var env = System.getenv(key);
    return env == null ? fallback : env;
  }

  private static WorkerAppConfiguration loadConfiguration() {
    return new WorkerAppConfiguration(
        Path.of(getEnv("MERLIN_WORKER_LOCAL_STORE", "/usr/src/app/merlin_file_store")),
        new PostgresStore(getEnv("MERLIN_WORKER_DB_SERVER", "postgres"),
                          getEnv("MERLIN_WORKER_DB_USER", ""),
                          Integer.parseInt(getEnv("MERLIN_WORKER_DB_PORT", "5432")),
                          getEnv("MERLIN_WORKER_DB_PASSWORD", ""),
                          getEnv("MERLIN_WORKER_DB", "aerie_merlin")),
        Integer.parseInt(getEnv("SIMULATION_PROGRESS_POLL_PERIOD_MILLIS", "5000")),
        Instant.parse(getEnv("UNTRUE_PLAN_START", "")),
        Boolean.parseBoolean(getEnv("USE_RESOURCE_TRACKER", defaultUseResourceTracker ? "true" : "false"))
    );
  }
}
