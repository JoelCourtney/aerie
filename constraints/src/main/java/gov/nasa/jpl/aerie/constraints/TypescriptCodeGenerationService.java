package gov.nasa.jpl.aerie.constraints;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TypescriptCodeGenerationService {

  private static final ValueSchema LINEAR_RESOURCE_SCHEMA = ValueSchema.ofStruct(Map.of(
      "initial", ValueSchema.REAL,
      "rate", ValueSchema.REAL));

  public record Parameter(String name, ValueSchema schema) {}
  public record ActivityType(List<Parameter> parameters) {}

  public static String generateTypescriptTypes(
      final Map<String, ActivityType> activityTypes,
      final Map<String, ValueSchema> resources
  ) {
    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");
    result.add("import * as AST from './constraints-ast.js';");
    result.add("import { Discrete, Real, Windows } from './constraints-edsl-fluent-api.js';");

    result.add("export enum ActivityType {");
    for (String activityType: activityTypes.keySet()) {
      result.add(indent(activityType + " = \"" + activityType + "\","));
    }
    result.add("}");

    result.add("export type ResourceName = " + String.join(" | ", resources.keySet().stream().map($ -> "\"" + $ + "\"").toList()) + ";");
    result.add("export type DiscreteResourceSchema<R extends ResourceName> =");
    for (var resource: resources.keySet()) {
      result.add(indent("R extends \"" + resource + "\" ? " + valueSchemaToTypescript(resources.get(resource)) + " :"));
    }
    result.add(indent("never;"));

    result.add("export type RealResourceName = " + String.join(
        " | ",
        resources.entrySet().stream().filter($ -> valueSchemaIsNumeric($.getValue())).map($ -> ("\"" + $.getKey() + "\"")).toList()
    ) + ";");

    // ActivityParameters

    result.add("export type ActivityParameters<A extends ActivityType> =");
    for (String activityType: activityTypes.keySet()) {
      StringBuilder parameterSchema = new StringBuilder("{");
      for (Parameter parameter: activityTypes.get(activityType).parameters()) {
        parameterSchema
            .append(parameter.name())
            .append(": ")
            .append(valueSchemaToTypescriptProfile(parameter.schema()))
            .append(", ");
      }
      parameterSchema.append("}");
      result.add(indent("A extends ActivityType." + activityType + " ? " + parameterSchema + " :"));
    }
    result.add(indent("never;"));

    // ActivityInstance

    result.add("export class ActivityInstance<A extends ActivityType> {");
    result.add(indent("""
                          private readonly __activityType: A;
                          private readonly __alias: string;
                          constructor(activityType: A, alias: string) {
                          """));

    result.add(indent(indent("""
                                 this.__activityType = activityType;
                                 this.__alias = alias;
                                 """)));
    result.add(indent("""
                          }
                          public get parameters(): ActivityParameters<A> {"""));

    result.add(indent(indent("let result = (")));
    for (String activityType: activityTypes.keySet()) {
      final var profileObject = new StringBuilder("{");
      for (var parameter: activityTypes.get(activityType).parameters()) {
        var parameterProfile = valueSchemaToTypescriptProfile(parameter.schema());
        String nodeKind;
        if (parameterProfile.equals("Real")) {
          nodeKind = "RealProfileParameter";
        } else {
          nodeKind = "DiscreteProfileParameter";
        }
        profileObject
            .append(parameter.name())
            .append(": new ")
            .append(parameterProfile)
            .append("({ kind: AST.NodeKind.")
            .append(nodeKind)
            .append(", activityAlias: this.__alias, parameterName: \"")
            .append(parameter.name())
            .append("\"}")
            .append("), ");
      }
      profileObject.append("}");
      result.add(indent(indent(indent("this.__activityType === ActivityType." + activityType + " ? " + profileObject + " :"))));
    }
    result.add(indent(indent(indent("undefined) as ActivityParameters<A>;"))));
    result.add(indent(indent("""
                                 if (result === undefined) {
                                   throw new TypeError("Unreachable state. Activity type was unexpected string in ActivityInstance.parameters(): " + this.__activityType);
                                 } else {
                                   return result;
                                 }""")));

    result.add(indent("""
                          }
                          /**
                           * Produces a window for the duration of the activity.
                           */
                          public window(): Windows {
                            return new Windows({
                              kind: AST.NodeKind.WindowsExpressionActivityWindow,
                              activityAlias: this.__alias
                            });
                          }
                          /**
                           * Produces an instantaneous window at the start of the activity.
                           */
                          public start(): Windows {
                            return new Windows({
                              kind: AST.NodeKind.WindowsExpressionStartOf,
                              activityAlias: this.__alias
                            });
                          }
                          /**
                           * Produces an instantaneous window at the end of the activity.
                           */
                          public end(): Windows {
                            return new Windows({
                              kind: AST.NodeKind.WindowsExpressionEndOf,
                              activityAlias: this.__alias
                            });
                          }"""));

    result.add("}");

    result.add("""
                   declare global {""");
    result.add(indent("enum ActivityType {"));
    for (String activityType: activityTypes.keySet()) {
      result.add(indent(indent(activityType + " = \"" + activityType + "\",")));
    }
    result.add(indent("}"));
    result.add("""
                   }
                   Object.assign(globalThis, {
                     ActivityType
                   });""");

    result.add("/** End Codegen */");
    return joinLines(result);
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }


  private static String valueSchemaToTypescript(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.Visitor<>() {
      @Override
      public String onReal() {
        return "number";
      }

      @Override
      public String onInt() {
        return "number";
      }

      @Override
      public String onBoolean() {
        return "boolean";
      }

      @Override
      public String onString() {
        return "string";
      }

      @Override
      public String onDuration() {
        return "Duration";
      }

      @Override
      public String onPath() {
        return "string";
      }

      @Override
      public String onSeries(final ValueSchema value) {
        return valueSchemaToTypescript(value) + "[]";
      }

      @Override
      public String onStruct(final Map<String, ValueSchema> values) {
        final var result = new StringBuilder("{");

        for (final var member: values.keySet().stream().sorted().toList()) {
          result
              .append(member)
              .append(": ")
              .append(valueSchemaToTypescript(values.get(member)))
              .append(", ");
        }

        result.append("}");
        return result.toString();
      }

      @Override
      public String onVariant(final List<ValueSchema.Variant> variants) {
        final var result = new StringBuilder("(");

        for (final var variant: variants) {
          result
              .append(" | \"")
              .append(variant.label())
              .append("\"");
        }

        result.append(")");
        return result.toString();
      }
    });
  }

  private static String valueSchemaToTypescriptProfile(final ValueSchema valueSchema) {
    var basicTypescript = valueSchemaToTypescript(valueSchema);
    if (basicTypescript.equals("number")) {
      return "Real";
    } else {
      return "Discrete<" + basicTypescript + ">";
    }
  }

  private static boolean valueSchemaIsNumeric(final ValueSchema valueSchema) {
    return valueSchema.equals(ValueSchema.INT)
        || valueSchema.equals(ValueSchema.REAL)
        || valueSchema.equals(LINEAR_RESOURCE_SCHEMA);
  }
}
