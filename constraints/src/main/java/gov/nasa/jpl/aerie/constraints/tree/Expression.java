package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;
import java.util.Set;

public interface Expression<T> {
  T evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment);
  String prettyPrint(final String prefix);
  /** Add the resources referenced by this expression to the given set. **/
  void extractResources(Set<String> names);

  default T evaluate(final SimulationResults results) {
    return this.evaluate(results, Map.of());
  }
  default String prettyPrint() {
    return this.prettyPrint("");
  }
}
