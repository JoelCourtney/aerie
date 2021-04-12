package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;
import java.util.Objects;

public final class Times implements Expression<LinearProfile> {
  private final Expression<LinearProfile> profile;
  private final double multiplier;

  public Times(final Expression<LinearProfile> profile, final double multiplier) {
    this.profile = profile;
    this.multiplier = multiplier;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, environment).times(this.multiplier);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(* %s %s)",
        prefix,
        this.profile.prettyPrint(prefix + "  "),
        String.format("\n%s  %s", prefix, this.multiplier)
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Times)) return false;
    final var o = (Times)obj;

    return Objects.equals(this.profile, o.profile) &&
           Objects.equals(this.multiplier, o.multiplier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profile, this.multiplier);
  }
}
