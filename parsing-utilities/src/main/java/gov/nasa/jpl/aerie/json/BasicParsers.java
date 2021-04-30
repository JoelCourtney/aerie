package gov.nasa.jpl.aerie.json;

import gov.nasa.jpl.aerie.json.ProductParsers.EmptyProductParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public abstract class BasicParsers {
  private BasicParsers() {}

  public static JsonObject getCachedSchema(final Map<Object, String> anchors, final JsonParser<?> parser) {
    if (anchors.containsKey(parser)) {
      return Json.createObjectBuilder().add("$ref", "#" + anchors.get(parser)).build();
    } else {
      final var anchor = "_" + UUID.randomUUID();
      anchors.put(parser, anchor);

      return Json
          .createObjectBuilder()
          .add("$anchor", anchor)
          .addAll(Json.createObjectBuilder(parser.getSchema(anchors)))
          .build();
    }
  }

  public static final JsonParser<JsonValue> anyP = new JsonParser<>() {
    @Override
    public JsonParseResult<JsonValue> parse(final JsonValue json) {
      return JsonParseResult.success(json);
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      // The empty object represents no constraints on JSON to be parsed.
      return Json.createObjectBuilder().build();
    }
  };

  public static final JsonParser<Boolean> boolP = new JsonParser<>() {
    @Override
    public JsonParseResult<Boolean> parse(final JsonValue json) {
      if (Objects.equals(json, JsonValue.TRUE)) return JsonParseResult.success(true);
      if (Objects.equals(json, JsonValue.FALSE)) return JsonParseResult.success(false);
      return JsonParseResult.failure("expected boolean");
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          .add("type", "boolean")
          .build();
    }
  };

  public static final JsonParser<String> stringP = new JsonParser<>() {
    @Override
    public JsonParseResult<String> parse(final JsonValue json) {
      if (!(json instanceof JsonString)) return JsonParseResult.failure("expected string");

      return JsonParseResult.success(((JsonString) json).getString());
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          .add("type", "string")
          .build();
    }
  };

  public static final JsonParser<Long> longP = new JsonParser<>() {
    @Override
    public JsonParseResult<Long> parse(final JsonValue json) {
      if (!(json instanceof JsonNumber)) return JsonParseResult.failure("expected long");
      if (!((JsonNumber) json).isIntegral()) return JsonParseResult.failure("expected integral number");

      return JsonParseResult.success(((JsonNumber) json).longValue());
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          // must be an integer
          .add("type", "integer")
          // within the range of Java longs
          .add("minimum", Long.MIN_VALUE)
          .add("maximum", Long.MAX_VALUE)
          .build();
    }
  };

  public static final JsonParser<Double> doubleP = new JsonParser<>() {
    @Override
    public JsonParseResult<Double> parse(final JsonValue json) {
      if (!(json instanceof JsonNumber)) return JsonParseResult.failure("expected double");

      return JsonParseResult.success(((JsonNumber) json).doubleValue());
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          // must be a number
          .add("type", "number")
          // within the range of Java doubles
          // (don't use MIN_VALUE; it's actually the _minimum magnitude_, i.e. the positive value closest to zero.)
          .add("minimum", -Double.MAX_VALUE)
          .add("maximum", +Double.MAX_VALUE)
          .build();
    }
  };

  public static final JsonParser<Long> nullP = new JsonParser<>() {
    @Override
    public JsonParseResult<Long> parse(final JsonValue json) {
      if (!Objects.equals(json, JsonValue.NULL)) return JsonParseResult.failure("expected null");

      return JsonParseResult.success(null);
    }

    @Override
    public JsonObject getSchema(final Map<Object, String> anchors) {
      return Json
          .createObjectBuilder()
          .add("type", "null")
          .build();
    }
  };


  public static JsonParser<String> literalP(final String x) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<String> parse(final JsonValue json) {
        if (!Objects.equals(json, Json.createValue(x))) {
          return JsonParseResult.failure("string literal does not match expected value: \"" + x + "\"");
        }

        return JsonParseResult.success(x);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("const", Json.createValue(x))
            .build();
      }
    };
  }

  public static JsonParser<Long> literalP(final long x) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<Long> parse(final JsonValue json) {
        if (!Objects.equals(json, Json.createValue(x))) {
          return JsonParseResult.failure("long literal does not match expected value: \"" + x + "\"");
        }

        return JsonParseResult.success(x);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("const", Json.createValue(x))
            .build();
      }
    };
  }

  public static JsonParser<Double> literalP(final double x) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<Double> parse(final JsonValue json) {
        if (!Objects.equals(json, Json.createValue(x))) {
          return JsonParseResult.failure("double literal does not match expected value: \"" + x + "\"");
        }

        return JsonParseResult.success(x);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("const", Json.createValue(x))
            .build();
      }
    };
  }

  public static JsonParser<Boolean> literalP(final boolean x) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<Boolean> parse(final JsonValue json) {
        if (!Objects.equals(json, (x) ? JsonValue.TRUE : JsonValue.FALSE)) {
          return JsonParseResult.failure("boolean literal does not match expected value: \"" + x + "\"");
        }

        return JsonParseResult.success(x);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("const", (x) ? JsonValue.TRUE : JsonValue.FALSE)
            .build();
      }
    };
  }

  public static <T> JsonParser<List<T>> listP(final JsonParser<T> elementParser) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<List<T>> parse(final JsonValue json) {
        if (!(json instanceof JsonArray)) return JsonParseResult.failure("expected list");

        final var jsonArray = json.asJsonArray();
        final var list = new ArrayList<T>(jsonArray.size());
        for (int index = 0; index < jsonArray.size(); index++) {
          final var element = jsonArray.get(index);
          final var result = elementParser.parse(element);

          if (result.isFailure()) {
            return JsonParseResult.failure(
                result
                    .failureReason()
                    .prependBreadcrumb(
                        Breadcrumb.ofInteger(index)
                    ));
          }
          list.add(result.getSuccessOrThrow());
        }

        return JsonParseResult.success(list);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("type", "array")
            .add("items", getCachedSchema(anchors, elementParser))
            .build();
      }
    };
  }

  public static <S> JsonParser<Map<String, S>> mapP(final JsonParser<S> fieldParser) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<Map<String, S>> parse(final JsonValue json) {
        if (!(json instanceof JsonObject)) return JsonParseResult.failure("expected object");

        final var map = new HashMap<String, S>(json.asJsonObject().size());
        for (final var field : json.asJsonObject().entrySet()) {
          final var result = fieldParser.parse(field.getValue());

          if (result.isFailure()) {
            return JsonParseResult.failure(
                result
                    .failureReason()
                    .prependBreadcrumb(
                        Breadcrumb.ofString(field.getKey())
                    ));
          }
          map.put(field.getKey(), result.getSuccessOrThrow());
        }

        return JsonParseResult.success(map);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return Json
            .createObjectBuilder()
            .add("type", "object")
            .add("additionalProperties", getCachedSchema(anchors, fieldParser))
            .build();
      }
    };
  }

  public static <S> JsonParser<S> recursiveP(final Function<JsonParser<S>, JsonParser<S>> scope) {
    return new JsonParser<>() {
      private final JsonParser<S> target = scope.apply(this);

      @Override
      public JsonParseResult<S> parse(final JsonValue json) {
        return this.target.parse(json);
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        return this.target.getSchema(anchors);
      }
    };
  }

  @SafeVarargs
  public static <T> JsonParser<T> chooseP(final JsonParser<? extends T>... options) {
    return new JsonParser<>() {
      @Override
      public JsonParseResult<T> parse(final JsonValue json) {
        for (final var option : options) {
          final var result = option.parse(json);
          if (result.isFailure()) continue;
          return result.mapSuccess(x -> (T) x);
        }

        return JsonParseResult.failure("not parsable into acceptable type");
      }

      @Override
      public JsonObject getSchema(final Map<Object, String> anchors) {
        final var optionSchemas = Json.createArrayBuilder();
        for (final var option : options) {
          optionSchemas.add(getCachedSchema(anchors, option));
        }

        return Json
            .createObjectBuilder()
            .add("oneOf", optionSchemas)
            .build();
      }
    };
  }

  public static <T> SumParsers.VariantJsonParser<T> sumP() {
    return SumParsers.sumP();
  }

  public static EmptyProductParser productP = ProductParsers.productP;
}
