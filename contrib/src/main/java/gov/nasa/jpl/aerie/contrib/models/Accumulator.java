package gov.nasa.jpl.aerie.contrib.models;

import gov.nasa.jpl.aerie.contrib.cells.linear.LinearAccumulationEffect;
import gov.nasa.jpl.aerie.contrib.cells.linear.LinearIntegrationCell;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.resources.real.RealResource;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;

public final class Accumulator implements RealResource {
  private final CellRef<LinearAccumulationEffect, LinearIntegrationCell> ref;

  public final Rate rate = new Rate();

  public Accumulator() {
    this(0.0, 0.0);
  }

  public Accumulator(final double initialVolume, final double initialRate) {
    this.ref = new CellRef<>(new LinearIntegrationCell(initialVolume, initialRate));
  }

  @Override
  public RealDynamics getDynamics() {
    return this.ref.get().getVolume();
  }


  public final class Rate implements RealResource {
    @Override
    public RealDynamics getDynamics() {
      return Accumulator.this.ref.get().getRate();
    }

    public void add(final double delta) {
      Accumulator.this.ref.emit(LinearAccumulationEffect.addRate(delta));
    }
  }
}
