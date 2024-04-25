package gov.nasa.jpl.aerie.procedural.scheduling

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema

interface ProcedureMapper<T: Procedure> {
  fun valueSchema(): ValueSchema
  fun serialize(procedure: T): SerializedValue
  fun deserialize(arguments: SerializedValue): T
}
