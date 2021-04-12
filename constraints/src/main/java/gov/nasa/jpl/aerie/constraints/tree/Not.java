package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Map;
import java.util.Objects;

public final class Not implements Expression<Windows> {
  public final Expression<Windows> expression;

  public Not(final Expression<Windows> expression) {
    this.expression = expression;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return Windows.minus(
        new Windows(results.bounds),
        this.expression.evaluate(results, environment));
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(not %s)",
        prefix,
        this.expression.prettyPrint(prefix + "  ")
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Not)) return false;
    final var o = (Not)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
