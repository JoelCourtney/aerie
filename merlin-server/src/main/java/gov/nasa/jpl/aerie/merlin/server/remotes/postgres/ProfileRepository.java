package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.merlin.server.http.ProfileParsers.realDynamicsP;
import static gov.nasa.jpl.aerie.merlin.server.http.SerializedValueJsonParser.serializedValueP;

/*package-local*/ final class ProfileRepository {
  static ProfileSet getProfiles(
      final Connection connection,
      final long datasetId,
      final Window simulationWindow
  ) throws SQLException {
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      switch (record.type().getLeft()) {
        case "real" -> realProfiles.put(record.name(),
                                        Pair.of(
                                            record.type().getRight(),
                                            getRealProfileSegments(connection, record.datasetId(), record.id(), simulationWindow)));
        case "discrete" -> discreteProfiles.put(record.name(),
                                                Pair.of(
                                                    record.type().getRight(),
                                                    getDiscreteProfileSegments(connection, record.datasetId(), record.id(), simulationWindow)));
        default -> throw new Error("Unrecognized profile type");
      }
    }

    return new ProfileSet(realProfiles, discreteProfiles);
  }

  static Map<String, ValueSchema> getProfileSchemas(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    final var results = new HashMap<String, ValueSchema>();

    final var profileRecords = getProfileRecords(connection, datasetId);
    for (final var record : profileRecords) {
      results.put(record.name(), record.type().getRight());
    }

    return results;
  }

  static List<PlanDatasetRecord> getAllPlanDatasetsForPlan(final Connection connection, final PlanId planId, final Timestamp planStartTime) throws SQLException {
    try (final var getPlanDatasetsAction = new GetPlanDatasetsAction(connection)) {
      return getPlanDatasetsAction.get(planId, planStartTime);
    }
  }

  static List<ProfileRecord> getProfileRecords(
      final Connection connection,
      final long datasetId
  ) throws SQLException {
    try (final var getProfilesAction = new GetProfilesAction(connection)) {
      return getProfilesAction.get(datasetId);
    }
  }

  static List<Pair<Duration, RealDynamics>> getRealProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Window simulationWindow
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationWindow, realDynamicsP);
    }
  }

  static List<Pair<Duration, SerializedValue>> getDiscreteProfileSegments(
      final Connection connection,
      final long datasetId,
      final long profileId,
      final Window simulationWindow
  ) throws SQLException {
    try (final var getProfileSegmentsAction = new GetProfileSegmentsAction(connection)) {
      return getProfileSegmentsAction.get(datasetId, profileId, simulationWindow, serializedValueP);
    }
  }

  static void postResourceProfiles(
      final ParallelInserter parallelInserter,
      final long datasetId,
      final ProfileSet profileSet,
      final Timestamp simulationStart
  ) throws SQLException
  {
    parallelInserter.declareAction(connection -> {
      try (final var postProfilesAction = new PostProfilesAction(connection)) {
        final var profileRecords = postProfilesAction.apply(
            datasetId,
            profileSet.realProfiles(),
            profileSet.discreteProfiles());
        postProfileSegments(
            parallelInserter,
            datasetId,
            profileRecords,
            profileSet,
            simulationStart);
      }
    });
  }

  public static void postProfileSegments(
      final ParallelInserter parallelInserter,
      final long datasetId,
      final Map<String, ProfileRecord> records,
      final ProfileSet profileSet,
      final Timestamp simulationStart
  ) throws SQLException {
    final var realProfiles = profileSet.realProfiles();
    final var discreteProfiles = profileSet.discreteProfiles();
    for (final var entry : records.entrySet()) {
      final ProfileRecord record =  entry.getValue();
      final var resource =  entry.getKey();
      switch (record.type().getLeft()) {
        case "real" -> postRealProfileSegments(
            parallelInserter,
            datasetId,
            record,
            realProfiles.get(resource).getRight(),
            simulationStart);
        case "discrete" -> postDiscreteProfileSegments(
            parallelInserter,
            datasetId,
            record,
            discreteProfiles.get(resource).getRight(),
            simulationStart);
        default -> throw new Error("Unrecognized profile type " + record.type().getLeft());
      }
    }
  }

  private static void postRealProfileSegments(
      final ParallelInserter parallelInserter,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, RealDynamics>> segments,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction()) {
      postProfileSegmentsAction.apply(parallelInserter, datasetId, profileRecord, segments, simulationStart, realDynamicsP);
    }
  }

  private static void postDiscreteProfileSegments(
      final ParallelInserter parallelInserter,
      final long datasetId,
      final ProfileRecord profileRecord,
      final List<Pair<Duration, SerializedValue>> segments,
      final Timestamp simulationStart
  ) throws SQLException {
    try (final var postProfileSegmentsAction = new PostProfileSegmentsAction()) {
      postProfileSegmentsAction.apply(parallelInserter, datasetId, profileRecord, segments, simulationStart, serializedValueP);
    }
  }
}
