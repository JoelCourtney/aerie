package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<Model, Specification, Return> {
  String getName();
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  DurationSpecification getDurationSpecification();

  Specification instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException;

  Map<String, SerializedValue> getArguments(Specification taskSpec);
  List<String> getValidationFailures(Specification taskSpec);

  Task<Return> createTask(Model model, Specification taskSpec);
  ValueSchema getReturnValueSchema();
  SerializedValue serializeReturnValue(Return returnValue);

  class UnconstructableTaskSpecException extends Exception {}
}
