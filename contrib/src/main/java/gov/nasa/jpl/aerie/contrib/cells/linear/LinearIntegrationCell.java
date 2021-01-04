package gov.nasa.jpl.aerie.contrib.cells.linear;

import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.effects.EffectTrait;
import gov.nasa.jpl.aerie.time.Duration;

import static gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics.persistent;

public final class LinearIntegrationCell implements Cell<Double, LinearIntegrationCell> {
  private double _volume;
  private double _rate;

  public LinearIntegrationCell(final double volume, final double rate) {
    this._volume = volume;
    this._rate = rate;
  }

  @Override
  public LinearIntegrationCell duplicate() {
    return new LinearIntegrationCell(this._volume, this._rate);
  }

  @Override
  public EffectTrait<Double> effectTrait() {
    return new SumEffectTrait();
  }

  @Override
  public void react(final Double delta) {
    this._rate += delta;
  }

  @Override
  public void step(final Duration elapsedTime) {
    // Law: The passage of time shall not alter a valid dynamics.
    this._volume += this._rate * elapsedTime.ratioOver(Duration.SECOND);
  }


  /// Resources
  public DelimitedDynamics<RealDynamics> getVolume() {
    return persistent(RealDynamics.linear(this._volume, this._rate));
  }

  public DelimitedDynamics<RealDynamics> getRate() {
    return persistent(RealDynamics.constant(this._rate));
  }
}
