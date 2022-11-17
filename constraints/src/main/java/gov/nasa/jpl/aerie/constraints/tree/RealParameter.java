package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.LinearEquation;
import gov.nasa.jpl.aerie.constraints.profile.LinearProfile;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public record RealParameter(
    String activityAlias,
    String parameterName) implements Expression<Profile<LinearEquation>> {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var activity = environment.activityInstances().get(this.activityAlias);
    return LinearProfile.fromDiscrete(Profile.from(activity.parameters.get(parameterName)));
  }

  @Override
  public void extractResources(final Set<String> names) {
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(parameter %s %s)",
        prefix,
        this.activityAlias,
        this.parameterName
    );
  }
}
