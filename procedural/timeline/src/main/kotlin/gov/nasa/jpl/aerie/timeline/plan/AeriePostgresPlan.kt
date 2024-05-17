package gov.nasa.jpl.aerie.timeline.plan

import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue
import gov.nasa.jpl.aerie.timeline.BaseTimeline
import gov.nasa.jpl.aerie.timeline.Duration
import gov.nasa.jpl.aerie.timeline.Duration.Companion.minus
import gov.nasa.jpl.aerie.timeline.Duration.Companion.plus
import gov.nasa.jpl.aerie.timeline.Interval.Companion.at
import gov.nasa.jpl.aerie.timeline.Interval.Companion.between
import gov.nasa.jpl.aerie.timeline.Interval.Inclusivity.*
import gov.nasa.jpl.aerie.timeline.payloads.Segment
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyDirective
import gov.nasa.jpl.aerie.timeline.payloads.activities.AnyInstance
import gov.nasa.jpl.aerie.timeline.payloads.activities.Directive
import gov.nasa.jpl.aerie.timeline.payloads.activities.Instance
import gov.nasa.jpl.aerie.timeline.collections.Directives
import gov.nasa.jpl.aerie.timeline.collections.Instances
import gov.nasa.jpl.aerie.timeline.ops.coalesce.CoalesceSegmentsOp
import java.io.StringReader
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant
import javax.json.Json
import kotlin.jvm.optionals.getOrNull

/** A connection to Aerie's database for a particular simulation result. */
data class AeriePostgresPlan(
    /** A connection to Aerie's database. */
    val c: Connection,
    /** The particular simulation dataset to query. */
    val simDatasetId: Int
): Plan {

  private val datasetId by lazy {
    val statement = c.prepareStatement("select dataset_id from merlin.simulation_dataset where id = ?;")
    statement.setInt(1, simDatasetId)
    getSingleIntQueryResult(statement)
  }

  private val simulationId by lazy {
    val statement = c.prepareStatement("select simulation_id from merlin.simulation_dataset where id = ?;")
    statement.setInt(1, simDatasetId)
    getSingleIntQueryResult(statement)
  }

  private val simulationInfo by lazy {
    val statement = c.prepareStatement("select plan_id, simulation_start_time, simulation_end_time from merlin.simulation where id = ?;")
    statement.setInt(1, simulationId)
    val response = statement.executeQuery()
    if (!response.next()) throw DatabaseError("Expected exactly one result for query, found none: $statement")
    val result = object {
      val planId = response.getInt(1)
      val startTime = response.getTimestamp(2).toInstant()
      val endTime = response.getTimestamp(3).toInstant()
    }
    if (response.next()) throw DatabaseError("Expected exactly one result for query, found more than one: $statement")
    result
  }

  private fun getSingleIntQueryResult(statement: PreparedStatement): Int {
    val result = statement.executeQuery()
    if (!result.next()) throw DatabaseError("Expected exactly one result for query, found none: $statement")
    val int = result.getInt(1)
    if (result.next()) throw DatabaseError("Expected exactly one result for query, found more than one: $statement")
    return int
  }

  private val planInfo by lazy {
    val statement = c.prepareStatement("select start_time, duration from merlin.plan where id = ?;")
    statement.setInt(1, simulationInfo.planId)
    intervalStyleStatement.execute()
    val response = statement.executeQuery()
    if (!response.next()) throw DatabaseError("Expected exactly one result for query, found none: $statement")
    val result = object {
      val startTime = response.getTimestamp(1).toInstant()
      val duration = Duration.parseISO8601(response.getString(2))
      val id = simulationInfo.planId
    }
    if (response.next()) throw DatabaseError("Expected exactly one result for query, found more than one: $statement")
    result
  }

  override fun totalBounds() = between(Duration.ZERO, planInfo.duration)
  override fun simBounds() = between(
      toRelative(simulationInfo.startTime),
      toRelative(simulationInfo.endTime),
  )

  override fun toRelative(abs: Instant) = abs - planInfo.startTime
  override fun toAbsolute(rel: Duration) = planInfo.startTime + rel

  private val intervalStyleStatement = c.prepareStatement("set intervalstyle = 'iso_8601';")
  private val profileInfoStatement = c.prepareStatement(
      "select id, duration from merlin.profile where dataset_id = ? and name = ?;"
  )
  private data class ProfileInfo(val id: Int, val duration: Duration)

  private val segmentsStatement = c.prepareStatement(
      "select start_offset, dynamics, is_gap from merlin.profile_segment where profile_id = ? and dataset_id = ? order by start_offset asc;"
  )

  /***/ class DatabaseError(message: String): Error(message)

  override fun <V : Any, TL : CoalesceSegmentsOp<V, TL>> resource(name: String, ctor: (List<Segment<SerializedValue>>) -> TL): TL {
    val profileInfo = getProfileInfo(name)

    segmentsStatement.clearParameters()
    segmentsStatement.setInt(1, profileInfo.id)
    segmentsStatement.setInt(2, datasetId)
    intervalStyleStatement.execute()
    val response = segmentsStatement.executeQuery()

    val result = mutableListOf<Segment<SerializedValue>>()

    var previousValue: SerializedValue? = null
    var previousStart: Duration? = null

    while (response.next()) {
      val thisStart = Duration.parseISO8601(response.getString(1))
      if (previousStart !== null) {
        val interval = between(previousStart, thisStart, Inclusive, Exclusive)
        val newSegment = Segment(
            interval,
            previousValue!!
        )
        result.add(newSegment)
      }
      if (!response.getBoolean(3)) { // if not gap
        val serializedValue = parseJson(response.getString(2))
        previousValue = serializedValue
        previousStart = thisStart
      } else {
        previousValue = null
        previousStart = null
      }
    }
    if (previousStart !== null) {
      val interval = between(previousStart, profileInfo.duration, Inclusive, Exclusive)
      result.add(
          Segment(
              interval,
              previousValue!!
          )
      )
    }
    return ctor(result)
  }

  private fun getProfileInfo(name: String): ProfileInfo {
    profileInfoStatement.clearParameters()
    profileInfoStatement.setInt(1, datasetId)
    profileInfoStatement.setString(2, name)
    intervalStyleStatement.execute()
    val profileResult = profileInfoStatement.executeQuery()
    if (!profileResult.next()) throw DatabaseError("Profile $name not found in database")
    val id = profileResult.getInt(1)
    val duration = Duration.parseISO8601(profileResult.getString(2))
    if (profileResult.next()) throw DatabaseError("Multiple profiles named $name found in one simulation dataset")
    return ProfileInfo(id, duration)
  }

  private fun parseJson(jsonStr: String): SerializedValue = Json.createReader(StringReader(jsonStr)).use { reader ->
      val requestJson = reader.readValue()
      val result = SerializedValueJsonParser.serializedValueP.parse(requestJson)
      return result.getSuccessOrThrow { DatabaseError(it.toString()) }
  }

  private val allInstancesStatement = c.prepareStatement(
      "select start_offset, duration, attributes, activity_type_name, id from merlin.simulated_activity" +
          " where simulation_dataset_id = ?;"
  )
  private val filteredInstancesStatement = c.prepareStatement(
      "select start_offset, duration, attributes, activity_type_name, id from merlin.simulated_activity" +
          " where simulation_dataset_id = ? and activity_type_name = ?;"
  )
  override fun <A: Any> instances(type: String?, deserializer: (SerializedValue) -> A): Instances<A> {
    val statement = if (type == null) allInstancesStatement else filteredInstancesStatement
    statement.clearParameters()
    statement.setInt(1, simDatasetId)
    if (type != null) statement.setString(2, type);
    intervalStyleStatement.execute()
    val response = statement.executeQuery()
    val result = mutableListOf<Instance<A>>()
    while (response.next()) {
      val start = Duration.parseISO8601(response.getString(1))
      val id = response.getLong(5)
      val attributesString = response.getString(3)
      val attributes = parseJson(attributesString)
      val directiveId = attributes.asMap().getOrNull()?.get("directiveId")?.asInt()?.getOrNull()
      result.add(Instance(
          deserializer(attributes),
          response.getString(4),
          id,
          directiveId,
          between(start, start.plus(Duration.parseISO8601(response.getString(2))))
      ))
    }
    return Instances(result)
  }

  private val allDirectivesStatement = c.prepareStatement(
      "select name, start_offset, type, arguments, id from merlin.activity_directive where plan_id = ?" +
        " and start_offset > ?::interval and start_offset < ?::interval;"
  )
  private val filteredDirectivesStatement = c.prepareStatement(
      "select name, start_offset, type, arguments, id from merlin.activity_directive where plan_id = ?" +
          " and start_offset > ?::interval and start_offset < ?::interval and type = ?;"
  )
  override fun <A: Any> directives(type: String?, deserializer: (SerializedValue) -> A) = BaseTimeline(::Directives) { opts ->
    val statement = if (type == null) allDirectivesStatement else filteredDirectivesStatement
    statement.clearParameters()
    statement.setInt(1, planInfo.id)
    statement.setString(2, opts.bounds.start.toISO8601())
    statement.setString(3, opts.bounds.end.toISO8601())
    if (type != null) statement.setString(4, type)
    intervalStyleStatement.execute()
    val response = statement.executeQuery()
    val result = mutableListOf<Directive<A>>()
    while (response.next()) {
      result.add(Directive(
          deserializer(parseJson(response.getString(4))),
          response.getString(1),
          response.getLong(5),
          response.getString(3),
          Duration.parseISO8601(response.getString(2))
      ))
    }
    result
  }.specialize()
}
