package gov.nasa.jpl.aerie.timeline.payloads.activities

import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Interval

/** A wrapper of any type of activity directive containing common data. */
data class Directive<A: Any>(
    /** The inner payload, typically either [AnyDirective] or a mission model activity type. */
    @JvmField val inner: A,

    /** The name of this specific directive. */
    @JvmField val name: String,

    /** The directive id. */
    @JvmField val id: Long,

    override val type: String,
    override val startTime: Duration
): Activity<Directive<A>> {
  override val interval: Interval
    get() = Interval.at(startTime)

  override fun withNewInterval(i: Interval): Directive<A> {
    if (i.isPoint()) return Directive(inner, name, id, type, i.start)
    else throw Exception("Cannot change directive time to a non-instantaneous interval.")
  }
}
