package gov.nasa.jpl.aerie.merlin.server;

import com.mongodb.client.MongoClients;
import gov.nasa.jpl.aerie.merlin.driver.json.JsonEncoding;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfiguration;
import gov.nasa.jpl.aerie.merlin.server.config.AppConfigurationJsonMapper;
import gov.nasa.jpl.aerie.merlin.server.config.MongoStore;
import gov.nasa.jpl.aerie.merlin.server.config.Store;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.AdaptationRepositoryExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.LocalAppExceptionBindings;
import gov.nasa.jpl.aerie.merlin.server.http.MerlinBindings;
import gov.nasa.jpl.aerie.merlin.server.remotes.AdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoAdaptationRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoPlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.MongoResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.merlin.server.services.CachedSimulationService;
import gov.nasa.jpl.aerie.merlin.server.services.GetSimulationResultsAction;
import gov.nasa.jpl.aerie.merlin.server.services.LocalAdaptationService;
import gov.nasa.jpl.aerie.merlin.server.services.LocalPlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SynchronousSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.ThreadedSimulationAgent;
import gov.nasa.jpl.aerie.merlin.server.services.UnexpectedSubtypeError;
import io.javalin.Javalin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class AerieAppDriver {
  private static final Logger log = Logger.getLogger(AerieAppDriver.class.getName());

  public static void main(final String[] args) {
    // Fetch application configuration properties.
    final var configuration = loadConfiguration(args);
    final var stores = loadStores(configuration);

    // Assemble the core non-web object graph.
    final var missionModelDataPath = makeMissionModelDataPath(configuration);
    final var adaptationController = new LocalAdaptationService(missionModelDataPath, stores.adaptations());
    final var planController = new LocalPlanService(stores.plans(), adaptationController);
    final var simulationAgent = ThreadedSimulationAgent.spawn(
        "simulation-agent",
        new SynchronousSimulationAgent(planController, adaptationController));
    final var simulationController = new CachedSimulationService(stores.results(), simulationAgent);
    final var simulationAction = new GetSimulationResultsAction(planController, adaptationController, simulationController);
    final var merlinBindings = new MerlinBindings(planController, adaptationController, simulationAction);

    // Configure an HTTP server.
    final var javalin = Javalin.create(config -> {
      config.showJavalinBanner = false;
      if (configuration.javalinLogging().isEnabled()) config.enableDevLogging();
      config
          .enableCorsForAllOrigins()
          .registerPlugin(merlinBindings)
          .registerPlugin(new LocalAppExceptionBindings())
          .registerPlugin(new AdaptationRepositoryExceptionBindings())
          .registerPlugin(new AdaptationExceptionBindings());
    });

    // Start the HTTP server.
    javalin.start(configuration.httpPort());
  }

  private record Stores (PlanRepository plans, AdaptationRepository adaptations, ResultsCellRepository results) {}

  private static Stores loadStores(final AppConfiguration config) {
    final var store = config.store();
    if (store instanceof MongoStore c) {
      final var mongoDatabase = MongoClients
          .create(c.uri().toString())
          .getDatabase(c.database());

      return new Stores(
          new MongoPlanRepository(
              mongoDatabase,
              c.planCollection(),
              c.activityCollection()),
          new MongoAdaptationRepository(
              makeJarsPath(config),
              mongoDatabase,
              c.adaptationCollection()),
          new MongoResultsCellRepository(
              mongoDatabase,
              c.simulationResultsCollection()));
    } else {
      throw new UnexpectedSubtypeError(Store.class, store);
    }
  }

  private static Path makeMissionModelDataPath(final AppConfiguration configuration) {
    try {
      return Files.createDirectories(configuration.merlinFilesPath());
    } catch (final IOException ex) {
      throw new Error("Error creating merlin file store files directory", ex);
    }
  }

  private static Path makeJarsPath(final AppConfiguration configuration) {
    try {
      return Files.createDirectories(configuration.merlinJarsPath());
    } catch (final IOException ex) {
      throw new Error("Error creating merlin file store jars directory", ex);
    }
  }

  private static AppConfiguration loadConfiguration(final String[] args) {
    // Determine where we're getting our configuration from.
    final InputStream configStream;
    if (args.length > 0) {
      try {
        configStream = Files.newInputStream(Path.of(args[0]));
      } catch (final IOException ex) {
        log.warning("Configuration file \"%s\" could not be loaded: %s".formatted(args[0], ex.getMessage()));
        System.exit(1);
        throw new Error(ex);
      }
    } else {
      configStream = AerieAppDriver.class.getResourceAsStream("config.json");
    }

    // Read and process the configuration source.
    final var config = (JsonObject)(Json.createReader(configStream).readValue());
    return AppConfigurationJsonMapper.fromJson(config);
  }
}
