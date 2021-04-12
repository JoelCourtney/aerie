package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.NewAdaptation;

import java.util.List;
import java.util.Map;

public interface AdaptationService {
  Map<String, AdaptationJar> getAdaptations();

  String addAdaptation(NewAdaptation adaptation)
  throws AdaptationRejectedException;
  AdaptationJar getAdaptationById(String adaptationId)
  throws NoSuchAdaptationException;
  void removeAdaptation(String adaptationId)
  throws NoSuchAdaptationException;

  Map<String, String> getConstraints(String adaptationId)
  throws NoSuchAdaptationException;
  void replaceConstraints(String adaptationId, Map<String, String> constraints)
  throws NoSuchAdaptationException;
  void deleteConstraint(String adaptationId, String constraintName)
  throws NoSuchAdaptationException;

  Map<String, ValueSchema> getStatesSchemas(String adaptationId)
  throws NoSuchAdaptationException;
  Map<String, ActivityType> getActivityTypes(String adaptationId)
  throws NoSuchAdaptationException;
  ActivityType getActivityType(String adaptationId, String activityTypeId)
  throws NoSuchAdaptationException, NoSuchActivityTypeException;
  // TODO: Provide a finer-scoped validation return type. Mere strings make all validations equally severe.
  List<String> validateActivityParameters(String adaptationId, SerializedActivity activityParameters)
  throws NoSuchAdaptationException;

  SimulationResults runSimulation(CreateSimulationMessage message)
          throws NoSuchAdaptationException, AdaptationFacade.NoSuchActivityTypeException,
                 SimulationDriver.TaskSpecInstantiationException;

  class AdaptationRejectedException extends Exception {
    public AdaptationRejectedException(final String message) { super(message); }

    public AdaptationRejectedException(final Throwable cause) { super(cause); }
  }

  class NoSuchAdaptationException extends Exception {
    private final String id;

    public NoSuchAdaptationException(final String id, final Throwable cause) {
      super("No adaptation exists with id `" + id + "`", cause);
      this.id = id;
    }

    public NoSuchAdaptationException(final String id) { this(id, null); }

    public String getInvalidAdaptationId() { return this.id; }
  }

  class NoSuchActivityTypeException extends Exception {
    private final String activityTypeId;

    public NoSuchActivityTypeException(final String activityTypeId, final Throwable cause) {
      super(cause);
      this.activityTypeId = activityTypeId;
    }

    public NoSuchActivityTypeException(final String activityTypeId) { this(activityTypeId, null); }

    public String getInvalidActivityTypeId() { return activityTypeId; }
  }
}
