package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record DiscreteResource(String name) implements DiscreteProfileExpression {

  @Override
  public DiscreteProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    if (results.discreteProfiles.containsKey(this.name)) {
      return results.discreteProfiles.get(this.name);
    } else if (results.realProfiles.containsKey(this.name)) {
      throw new InputMismatchException(String.format("%s is a real resource, cannot interpret as discrete", this.name));
    }

    throw new InputMismatchException(String.format("%s is not a valid resource", this.name));
  }

  @Override
  public void extractResources(final Set<String> names) {
    names.add(this.name);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(resource %s)",
        prefix,
        this.name
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final DiscreteResource o)) return false;

    return Objects.equals(this.name, o.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.name);
  }
}
