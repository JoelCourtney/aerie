package gov.nasa.ammos.aerie.procedural.remote

import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.ammos.aerie.procedural.timeline.BaseTimeline
import gov.nasa.ammos.aerie.procedural.timeline.BoundsTransformer
import gov.nasa.ammos.aerie.procedural.timeline.Interval.Companion.between
import gov.nasa.ammos.aerie.procedural.timeline.collections.Directives
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.Directive
import gov.nasa.ammos.aerie.procedural.timeline.payloads.activities.DirectiveStart
import gov.nasa.ammos.aerie.procedural.timeline.plan.Plan
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.plus
import gov.nasa.ammos.aerie.procedural.timeline.util.duration.minus
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId
import java.io.StringReader
import java.sql.Connection
import java.time.Instant
import javax.json.Json

/**
 * A connection to Aerie's database for a particular simulation result.
 *
 * @param c A connection to Aerie's database
 * @param planId The ID of the plan as specified by in the postgres database
 */
data class AeriePostgresPlan(
    private val c: Connection,
    private val planId: Int
): Plan {

  private val planInfo by lazy {
    val statement = c.prepareStatement("select start_time, duration from merlin.plan where id = ?;")
    statement.setInt(1, planId)
    intervalStyleStatement(c).execute()
    val response = statement.executeQuery()
    if (!response.next()) throw DatabaseError("Expected exactly one result for query, found none: $statement")
    val result = object {
      val startTime = response.getTimestamp(1).toInstant()
      val duration = Duration.parseISO8601(response.getString(2))
      val id = this@AeriePostgresPlan.planId
    }
    if (response.next()) throw DatabaseError("Expected exactly one result for query, found more than one: $statement")
    result
  }

  override fun totalBounds() = between(Duration.ZERO, planInfo.duration)

  override fun toRelative(abs: Instant) = abs - planInfo.startTime
  override fun toAbsolute(rel: Duration) = planInfo.startTime + rel


  /***/ class DatabaseError(message: String): Error(message)

  private fun parseJson(jsonStr: String): SerializedValue = Json.createReader(StringReader(jsonStr)).use { reader ->
    val requestJson = reader.readValue()
    val result = serializedValueP.parse(requestJson)
    return result.getSuccessOrThrow { DatabaseError(it.toString()) }
  }

  private val allDirectivesStatement = c.prepareStatement(
      "select name, start_offset, type, arguments, id, anchor_id, anchored_to_start from merlin.activity_directive where plan_id = ?" +
          " and start_offset > ?::interval and start_offset < ?::interval;"
  )
  override fun <A: Any> directives(type: String?, deserializer: (SerializedValue) -> A) =
      Directives(
          (
              if (type == null) allDirectives
              else allDirectives.filter { it.type == type }
          )
      ).unsafeMap(::Directives, BoundsTransformer.IDENTITY, false) {
        it.mapInner(deserializer)
      }

  private val allDirectives by lazy {
    BaseTimeline(::Directives) { opts ->
      allDirectivesStatement.clearParameters()
      allDirectivesStatement.setInt(1, planInfo.id)
      allDirectivesStatement.setString(2, opts.bounds.start.toISO8601())
      allDirectivesStatement.setString(3, opts.bounds.end.toISO8601())
      intervalStyleStatement(c).execute()
      val response = allDirectivesStatement.executeQuery()
      var unresolved = mutableListOf<Directive<SerializedValue>>()
      while (response.next()) {
        val anchorId = response.getLong(6)
        val offset = Duration.parseISO8601(response.getString(2))
        val start = if (anchorId != 0L) { // this means SQL null. Terrible interface imo
          val anchoredToStart = response.getBoolean(7)
          DirectiveStart.Anchor(ActivityDirectiveId(anchorId), offset, if (anchoredToStart) DirectiveStart.Anchor.AnchorPoint.Start else DirectiveStart.Anchor.AnchorPoint.End, Duration.ZERO)
        } else DirectiveStart.Absolute(offset)
        unresolved.add(Directive(
            parseJson(response.getString(4)),
            response.getString(1),
            ActivityDirectiveId(response.getLong(5)),
            response.getString(3),
            start
        ))
      }
      val result = mutableListOf<Directive<SerializedValue>>()
      while (unresolved.size != 0) {
        val sizeAtStartOfStep = unresolved.size
        unresolved = unresolved.filterNot {
          when (val s = it.start) {
            is DirectiveStart.Absolute -> {
              result.add(it)
            }
            is DirectiveStart.Anchor -> {
              val index = result.binarySearch { a -> a.id.id.compareTo(s.parentId.id) }
              if (index >= 0) {
                val parent = result[index]
                s.estimatedStart = parent.startTime + s.offset
                result.add(it)
              } else false
            }
          }
        }.toMutableList()
        if (sizeAtStartOfStep == unresolved.size) throw Error("Cannot resolve anchors: $unresolved")
        result.sortBy { it.id.id }
      }
      result
    }.specialize()
        .collect()
  }
}
