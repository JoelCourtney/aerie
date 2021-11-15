package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.MissionModelLoader;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;

import java.util.List;
import java.util.Map;

public interface MissionModelService {
  Map<String, MissionModelJar> getMissionModels();

  MissionModelJar getMissionModelById(String missionModelId)
  throws NoSuchMissionModelException;

  Map<String, Constraint> getConstraints(String missionModelId)
  throws NoSuchMissionModelException;

  Map<String, ValueSchema> getStatesSchemas(String missionModelId)
  throws NoSuchMissionModelException;
  Map<String, ActivityType> getActivityTypes(String missionModelId)
  throws NoSuchMissionModelException;
  // TODO: Provide a finer-scoped validation return type. Mere strings make all validations equally severe.
  List<String> validateActivityParameters(String missionModelId, SerializedActivity activityParameters)
  throws NoSuchMissionModelException;

  Map<String, SerializedValue> getActivityEffectiveArguments(String missionModelId, SerializedActivity activity)
  throws NoSuchMissionModelException,
    NoSuchActivityTypeException,
    UnconstructableActivityInstanceException,
    MissingArgumentException;

  List<Parameter> getModelParameters(String missionModelId)
  throws NoSuchMissionModelException, MissionModelLoader.MissionModelLoadException;

  SimulationResults runSimulation(CreateSimulationMessage message)
          throws NoSuchMissionModelException, MissionModelFacade.NoSuchActivityTypeException;

  void refreshModelParameters(String missionModelId) throws NoSuchMissionModelException;
  void refreshActivityTypes(String missionModelId) throws NoSuchMissionModelException;

  class MissionModelRejectedException extends Exception {
    public MissionModelRejectedException(final String message) { super(message); }
  }

  class NoSuchMissionModelException extends Exception {
    private final String id;

    public NoSuchMissionModelException(final String id, final Throwable cause) {
      super("No mission model exists with id `" + id + "`", cause);
      this.id = id;
    }

    public NoSuchMissionModelException(final String id) { this(id, null); }

    public String getInvalidMissionModelId() { return this.id; }
  }

  class NoSuchActivityTypeException extends Exception {
    public final String activityTypeId;

    public NoSuchActivityTypeException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }

    public NoSuchActivityTypeException(final String activityTypeId) { this(activityTypeId, null); }
  }

  class UnconstructableActivityInstanceException extends Exception {
    public final String activityTypeId;

    public UnconstructableActivityInstanceException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }
  }
}
