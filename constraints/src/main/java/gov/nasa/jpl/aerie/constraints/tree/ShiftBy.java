package gov.nasa.jpl.aerie.constraints.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ShiftBy implements WindowsExpression {
  public final Expression<Windows> windows;
  public final Duration fromStart;
  public final Duration fromEnd;

  @JsonCreator
  public ShiftBy(@JsonProperty("windows") final Expression<Windows> windows, @JsonProperty("fromStart") final Duration fromStart, @JsonProperty("fromEnd") final Duration fromEnd) {
    this.windows = windows;
    this.fromStart = fromStart;
    this.fromEnd = fromEnd;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    final var windows = this.windows.evaluate(results, bounds, environment);
    return windows.shiftBy(this.fromStart, this.fromEnd);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.windows.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(shift %s by %s %s)",
        prefix,
        this.windows.prettyPrint(prefix + "  "),
        this.fromStart.toString(),
        this.fromEnd.toString()
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final ShiftBy o)) return false;

    return Objects.equals(this.windows, o.windows) &&
           Objects.equals(this.fromStart, o.fromStart) &&
           Objects.equals(this.fromEnd, o.fromEnd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows, this.fromStart, this.fromEnd);
  }
}
