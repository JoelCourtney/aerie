package gov.nasa.jpl.aerie.timeline.ops

import gov.nasa.jpl.aerie.timeline.payloads.activities.Activity

/**
 * Operations mixin for timelines of activities.
 */
interface ActivityOps<A: Activity<A>, THIS: ActivityOps<A, THIS>>: ParallelOps<A, THIS> {
  /** Filters out all activities except those of a given vararg list of types. */
  fun filterByType(vararg types: String) = filterByType(types.asList())

  /** Filters out all activities except those of a given list of types. */
  fun filterByType(types: List<String>) = filter { it.type in types }
}
