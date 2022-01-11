package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.util.List;
import java.util.Map;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

public interface ConfigurationType<Config> {
  String getName();
  List<Parameter> getParameters();
  List<String> getRequiredParameters();

  Config instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableConfigurationException;

  Map<String, SerializedValue> getArguments(Config configuration);
  List<String> getValidationFailures(Config configuration);

  class UnconstructableConfigurationException extends Exception {}
}
