package gov.nasa.jpl.aerie.timeline.collections.profiles

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration.seconds
import gov.nasa.jpl.aerie.timeline.Interval
import gov.nasa.jpl.aerie.timeline.Interval.Companion.at
import gov.nasa.jpl.aerie.timeline.Interval.Companion.between
import gov.nasa.jpl.aerie.timeline.payloads.LinearEquation
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import org.junit.jupiter.api.Assertions.assertIterableEquals
import gov.nasa.jpl.aerie.timeline.util.duration.rangeTo
import gov.nasa.jpl.aerie.timeline.util.duration.rangeUntil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RealTest {
  @Test
  fun plusShiftsInitialTime() {
    val result = (Real(
        Segment(seconds(0)..seconds(2), LinearEquation(seconds(0), 1.0, 1.0))
    ) + Real(
        Segment(seconds(1)..seconds(3), LinearEquation(seconds(-2), -1.0, 3.0))
    )).collect()
    assertIterableEquals(
        listOf(Segment(seconds(1)..seconds(2), LinearEquation(seconds(1), 10.0, 4.0))),
        result
    )
  }

  @Test
  fun increases() {
    val result = Real(
      Segment(seconds(0) ..< seconds(1), LinearEquation(0)),
      Segment(seconds(1) ..< seconds(2), LinearEquation(seconds(1), 2.0, -1.0)),
      Segment(seconds(2) .. seconds(3), LinearEquation(seconds(2), 1.0, 1.0))
    ).increases()

    assertIterableEquals(
      listOf(
        Segment(between(seconds(0), seconds(1), Interval.Inclusivity.Exclusive), false),
        Segment(at(seconds(1)), true),
        Segment(between(seconds(1), seconds(2), Interval.Inclusivity.Exclusive, Interval.Inclusivity.Inclusive), false),
        Segment(between(seconds(2), seconds(3), Interval.Inclusivity.Exclusive), true)
      ),
      result.collect()
    )
  }

}
