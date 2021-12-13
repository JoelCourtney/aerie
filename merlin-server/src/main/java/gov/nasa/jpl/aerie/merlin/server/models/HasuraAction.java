package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record HasuraAction<I extends HasuraAction.Input>(String name, I input, Session session)
{
  public record Session(String hasuraRole, String hasuraUserId) { }

  public interface Input { }

  public record MissionModelInput(String missionModelId) implements Input { }
  public record PlanInput(String planId) implements Input { }
  public record ActivityInput(String missionModelId,
                              String activityTypeName,
                              Map<String, SerializedValue> arguments) implements Input {}
  public record UploadExternalDatasetInput(String planId,
                                           Timestamp datasetStart,
                                           ProfileSet profileSet) implements Input {}
}
