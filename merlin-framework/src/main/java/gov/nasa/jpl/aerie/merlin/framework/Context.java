package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface Context {
  enum ContextType { Initializing, Reacting, Querying }

  // Usable in all contexts
  ContextType getContextType();

  // Usable during both initialization & simulation
  <State> State ask(Query<State> query);

  // Usable during initialization
  <Event, Effect, State>
  Query<State>
  allocate(
      State initialState,
      CellType<Effect, State> cellType,
      Function<Event, Effect> interpretation,
      Topic<Event> topic);

  // Usable during simulation
  <Event> void emit(Event event, Topic<Event> topic);

  interface TaskFactory<Return> { Task<Return> create(ExecutorService executor); }

  <Return> void spawn(TaskFactory<Return> task);
  <Return> void call(TaskFactory<Return> task);

  void delay(Duration duration);
  void waitUntil(Condition condition);
}
