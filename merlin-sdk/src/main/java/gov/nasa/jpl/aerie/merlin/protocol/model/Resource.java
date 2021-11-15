package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface Resource<Dynamics> {
  String getType();
  ValueSchema getSchema();
  Dynamics getDynamics(Querier querier);
  SerializedValue serialize(Dynamics dynamics);
}
