package gov.nasa.jpl.aerie.services.plan.http;

import gov.nasa.jpl.aerie.json.JsonParseResult.FailureReason;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.services.plan.controllers.Breadcrumb;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.services.plan.exceptions.ValidationException;
import gov.nasa.jpl.aerie.services.plan.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.plan.models.CreatedEntity;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.models.SimulationResults;
import gov.nasa.jpl.aerie.services.plan.models.Timestamp;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ResponseSerializers {
  public static <T> JsonValue serializeList(final Function<T, JsonValue> elementSerializer, final List<T> elements) {
    if (elements == null) return JsonValue.NULL;

    final var builder = Json.createArrayBuilder();
    for (final var element : elements) builder.add(elementSerializer.apply(element));
    return builder.build();
  }

  public static <T> JsonValue serializeMap(final Function<T, JsonValue> fieldSerializer, final Map<String, T> fields) {
    if (fields == null) return JsonValue.NULL;

    final var builder = Json.createObjectBuilder();
    for (final var entry : fields.entrySet()) builder.add(entry.getKey(), fieldSerializer.apply(entry.getValue()));
    return builder.build();
  }

  public static JsonValue serializeSample(final Pair<Duration, SerializedValue> element) {
    if (element == null) return JsonValue.NULL;
    return Json
        .createObjectBuilder()
        .add("x", serializeDuration(element.getLeft()))
        .add("y", serializeActivityParameter(element.getRight()))
        .build();
  }

  public static JsonValue serializeString(final String value) {
    if (value == null) return JsonValue.NULL;
    return Json.createValue(value);
  }

  public static JsonValue serializeTimestamp(final Timestamp timestamp) {
    if (timestamp == null) return JsonValue.NULL;
    return Json.createValue(timestamp.toString());
  }

  public static JsonValue serializeStringList(final List<String> elements) {
    return serializeList(x -> serializeString(x), elements);
  }

  public static JsonValue serializeActivityParameter(final SerializedValue parameter) {
    if (parameter == null) return JsonValue.NULL;
    return parameter.match(new ParameterSerializationVisitor());
  }

  public static JsonValue serializeActivityParameterMap(final Map<String, SerializedValue> fields) {
    return serializeMap(x -> serializeActivityParameter(x), fields);
  }

  public static JsonValue serializeActivityInstance(final ActivityInstance activityInstance) {
    if (activityInstance == null) return JsonValue.NULL;

    return Json.createObjectBuilder()
        .add("type", serializeString(activityInstance.type))
        .add("startTimestamp", serializeTimestamp(activityInstance.startTimestamp))
        .add("parameters", serializeActivityParameterMap(activityInstance.parameters))
        .build();
  }

  public static JsonValue serializeActivityInstanceMap(final Map<String, ActivityInstance> fields) {
    return serializeMap(x -> serializeActivityInstance(x), fields);
  }

  public static JsonValue serializePlan(final Plan plan) {
    return Json.createObjectBuilder()
        .add("name", serializeString(plan.name))
        .add("adaptationId", serializeString(plan.adaptationId))
        .add("startTimestamp", serializeTimestamp(plan.startTimestamp))
        .add("endTimestamp", serializeTimestamp(plan.endTimestamp))
        .add("activityInstances", serializeActivityInstanceMap(plan.activityInstances))
        .build();
  }

  public static JsonValue serializePlanMap(final Map<String, Plan> fields) {
    return serializeMap(x -> serializePlan(x), fields);
  }

  public static JsonValue serializeCreatedEntity(final CreatedEntity entity) {
    return Json.createObjectBuilder()
        .add("id", serializeString(entity.id))
        .build();
  }

  public static JsonValue serializeTimestamp(final Instant instant) {
    final var formattedTimestamp = DateTimeFormatter
        .ofPattern("uuuu-DDD'T'HH:mm:ss.SSSSSS")
        .withZone(ZoneOffset.UTC)
        .format(instant);

    return Json.createValue(formattedTimestamp);
  }

  public static JsonValue serializeDuration(final Duration timestamp) {
    return Json.createValue(timestamp.in(Duration.MICROSECONDS));
  }

  public static JsonValue serializeSimulationResults(final SimulationResults results) {
    if (results == null) return JsonValue.NULL;

    return Json.createObjectBuilder()
        .add("start", serializeTimestamp(results.startTime))
        .add("resources", serializeMap(
            elements -> serializeList(element -> serializeSample(element), elements),
            results.resourceSamples))
        .add("constraints", results.constraints)
        .add("activities", results.activities)
        .build();
  }

  private static JsonValue serializeBreadcrumb(final Breadcrumb breadcrumb) {
    return breadcrumb.match(new Breadcrumb.Visitor<>() {
      @Override
      public JsonValue onListIndex(final int index) {
        return Json.createValue(index);
      }

      @Override
      public JsonValue onMapIndex(final String index) {
        return Json.createValue(index);
      }
    });
  }

  public static JsonValue serializeValidationMessage(final List<Breadcrumb> breadcrumbs, final String message) {
    return Json.createObjectBuilder()
        .add("breadcrumbs", serializeList(ResponseSerializers::serializeBreadcrumb, breadcrumbs))
        .add("message", message)
        .build();
  }

  public static JsonValue serializeValidationMessages(final List<Pair<List<Breadcrumb>, String>> messages) {
    return serializeList(x -> serializeValidationMessage(x.getKey(), x.getValue()), messages);
  }

  public static JsonValue serializeValidationException(final ValidationException ex) {
    return serializeValidationMessages(ex.getValidationErrors());
  }

  public static JsonValue serializeJsonParsingException(final JsonParsingException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "invalid json")
        .build();
  }

  public static JsonValue serializeInvalidJsonException(final InvalidJsonException ex) {
    return Json.createObjectBuilder()
               .add("kind", "invalid-entity")
               .add("message", "invalid json")
               .build();
  }

  public static JsonValue serializeInvalidEntityException(final InvalidEntityException ex) {
    return Json.createObjectBuilder()
               .add("kind", "invalid-entity")
               .add("failures", serializeList(ResponseSerializers::serializeFailureReason, ex.failures))
               .build();
  }

  public static JsonValue serializeFailureReason(final FailureReason failure) {
    return Json.createObjectBuilder()
               .add("breadcrumbs", serializeList(ResponseSerializers::serializeParseFailureBreadcrumb, failure.breadcrumbs))
               .add("message", failure.reason)
               .build();
  }

  public static JsonValue serializeParseFailureBreadcrumb(final gov.nasa.jpl.aerie.json.Breadcrumb breadcrumb) {
    return breadcrumb.visit(new gov.nasa.jpl.aerie.json.Breadcrumb.BreadcrumbVisitor<>() {
      @Override
      public JsonValue onString(final String s) {
        return Json.createValue(s);
      }

      @Override
      public JsonValue onInteger(final Integer i) {
        return Json.createValue(i);
      }
    });
  }

  public static JsonValue serializeNoSuchPlanException(final NoSuchPlanException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such plan")
        .build();
  }

  public static JsonValue serializeNoSuchActivityInstanceException(final NoSuchActivityInstanceException ex) {
    // TODO: Improve diagnostic information
    return Json.createObjectBuilder()
        .add("message", "no such activity instance")
        .build();
  }

  static private class ParameterSerializationVisitor implements SerializedValue.Visitor<JsonValue> {
    @Override
    public JsonValue onNull() {
      return JsonValue.NULL;
    }

    @Override
    public JsonValue onReal(final double value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onInt(final long value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onBoolean(final boolean value) {
      return (value) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    @Override
    public JsonValue onString(final String value) {
      return Json.createValue(value);
    }

    @Override
    public JsonValue onMap(final Map<String, SerializedValue> fields) {
      return serializeMap(x -> x.match(this), fields);
    }

    @Override
    public JsonValue onList(final List<SerializedValue> elements) {
      return serializeList(x -> x.match(this), elements);
    }
  }
}
