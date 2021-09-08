package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SimulationUtility {

  public static SimulationResults
  simulate(final Map<String, Pair<Duration, SerializedActivity>> schedule, final Duration simulationDuration)
  throws SimulationDriver.TaskSpecInstantiationException
  {
    final var factory = new GeneratedAdaptationFactory();

    final var builder = new AdaptationBuilder<>(Schema.builder());

    final var dataPath = Path.of(SimulationUtility.class.getClassLoader().getResource("data/lorem_ipsum.txt").getPath());
    final var config = new Configuration(Configuration.DEFAULT_PLANT_COUNT, Configuration.DEFAULT_PRODUCER, dataPath);
    final var serializedConfig = new ConfigurationValueMapper().serializeValue(config);
    factory.instantiate(serializedConfig, builder);
    final var adaptation = builder.build();
    final var startTime = Instant.now();

    return SimulationDriver.simulate(
        adaptation,
        schedule,
        startTime,
        simulationDuration);
  }

  @SafeVarargs
  public static Map<String, Pair<Duration, SerializedActivity>> buildSchedule(final Pair<Duration, SerializedActivity>... activitySpecs) {
    final var schedule = new HashMap<String, Pair<Duration, SerializedActivity>>();

    for (final var activitySpec : activitySpecs) {
      schedule.put(UUID.randomUUID().toString(), activitySpec);
    }

    return schedule;
  }
}
