package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.Query;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.Task;

import java.util.Map;

public interface Context {
  // Usable during both initialization & simulation
  <CellType> CellType ask(Query<?, ?, CellType> query);

  // Usable during initialization
  <Event, Effect, CellType>
  Query<?, Event, CellType>
  allocate(
      final Projection<Event, Effect> projection,
      final Applicator<Effect, CellType> applicator);

  // Usable during simulation
  <Event> void emit(Event event, Query<?, Event, ?> query);

  interface TaskFactory { <$Timeline> Task<$Timeline> create(); }

  String spawn(TaskFactory task);
  String spawn(String type, Map<String, SerializedValue> arguments);

  String defer(Duration duration, TaskFactory task);
  String defer(Duration duration, String type, Map<String, SerializedValue> arguments);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(Condition condition);
}
