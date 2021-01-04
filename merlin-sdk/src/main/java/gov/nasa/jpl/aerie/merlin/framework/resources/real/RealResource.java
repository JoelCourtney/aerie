package gov.nasa.jpl.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.aerie.merlin.framework.Property;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.timeline.History;

import java.util.Objects;

public abstract class RealResource<$Schema> {
  private RealResource() {}

  public abstract DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now);

  public static <$Schema>
  RealResource<$Schema>
  atom(final Property<History<? extends $Schema>, RealDynamics> property) {
    Objects.requireNonNull(property);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return property.ask(now);
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  scaleBy(final double scalar, final RealResource<$Schema> resource) {
    Objects.requireNonNull(resource);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return resource.getDynamics(now).map($ -> $.scaledBy(scalar));
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  add(final RealResource<$Schema> left, final RealResource<$Schema> right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).parWith(right.getDynamics(now), RealDynamics::plus);
      }
    };
  }

  public static <$Schema>
  RealResource<$Schema>
  subtract(final RealResource<$Schema> left, final RealResource<$Schema> right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);

    return new RealResource<>() {
      @Override
      public DelimitedDynamics<RealDynamics> getDynamics(final History<? extends $Schema> now) {
        return left.getDynamics(now).parWith(right.getDynamics(now), RealDynamics::minus);
      }
    };
  }


  public RealResource<$Schema> plus(final RealResource<$Schema> other) {
    return RealResource.add(this, other);
  }

  public RealResource<$Schema> minus(final RealResource<$Schema> other) {
    return RealResource.subtract(this, other);
  }

  public RealResource<$Schema> scaledBy(final double scalar) {
    return RealResource.scaleBy(scalar, this);
  }


  public final double ask(final History<? extends $Schema> now) {
    return this.getDynamics(now).getDynamics().initial;
  }

  public Condition<$Schema> isBetween(final double lower, final double upper) {
    return Condition.atom(
          new RealResourceSolver<>(),
          this,
          new RealCondition(ClosedInterval.between(lower, upper)));
  }
}
