package gov.nasa.jpl.aerie.contrib.models.counters;

import gov.nasa.jpl.aerie.contrib.cells.counters.CounterCell;
import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.function.BinaryOperator;


public final class Counter<T> implements DiscreteResource<T> {
  private final CellRef<T, CounterCell<T>> ref;

  public Counter(final T initialValue, final T zero, final BinaryOperator<T> adder) {
    final var trait = new CommutativeMonoid<>(zero, adder);

    this.ref = CellRef.allocate(new CounterCell<>(initialValue, trait::sequentially), trait);
  }

  public static Counter<Integer> ofInteger(final Integer initialValue) {
    return new Counter<>(initialValue, 0, Integer::sum);
  }

  public static Counter<Integer> ofInteger() {
    return ofInteger(0);
  }

  public static Counter<Double> ofDouble(final Double initialValue) {
    return new Counter<>(initialValue, 0.0, Double::sum);
  }

  public static Counter<Double> ofDouble() {
    return ofDouble(0.0);
  }


  @Override
  public T getDynamics() {
    return this.ref.get().getValue();
  }

  public void add(final T change) {
    this.ref.emit(change);
  }


  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
