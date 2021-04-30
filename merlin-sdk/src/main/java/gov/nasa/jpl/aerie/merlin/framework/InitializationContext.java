package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Applicator;
import gov.nasa.jpl.aerie.merlin.timeline.effects.Projection;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class InitializationContext implements Context {
  private final AdaptationBuilder<?> builder;

  public InitializationContext(final AdaptationBuilder<?> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public static <T> T initializing(final AdaptationBuilder<?> builder, final Supplier<T> initializer) {
    return ModelActions.context.setWithin(new InitializationContext(builder), initializer::get);
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    return query.getInitialValue();
  }

  @Override
  public <Event, Effect, CellType>
  Query<?, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
    return this.builder.allocate(projection, applicator);
  }

  @Override
  public <Event> void emit(final Event event, final Query<?, Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state during initialization");
  }

  @Override
  public String spawn(final TaskFactory task) {
    this.builder.daemon(task);
    return null;  // TODO: get some way to refer to the daemon task
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities during initialization");
  }

  @Override
  public String defer(final Duration duration, final TaskFactory task) {
    throw new IllegalStateException("Cannot schedule tasks during initialization");
  }

  @Override
  public String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities during initialization");
  }

  @Override
  public void delay(final Duration duration) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void waitFor(final String id) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void waitUntil(final Condition condition) {
    throw new IllegalStateException("Cannot yield during initialization");
  }
}
