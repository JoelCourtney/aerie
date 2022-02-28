package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.Iso;
import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.json.Uncurry.tuple;
import static gov.nasa.jpl.aerie.json.Uncurry.untuple;
import static gov.nasa.jpl.aerie.scheduler.server.http.SerializedValueJsonParser.serializedValueP;

public class SchedulingGoalDSLCompilationService {

  private final Process nodeProcess;

  public SchedulingGoalDSLCompilationService() throws SchedulingGoalDSLCompilationException, IOException {
    final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
    final var schedulingDslCompilerCommand = System.getenv("SCHEDULING_DSL_COMPILER_COMMAND");
    this.nodeProcess = Runtime.getRuntime().exec(new String[]{"node", schedulingDslCompilerCommand}, null, new File(schedulingDslCompilerRoot));

    try {
      this.nodeProcess.getInputStream();
      this.nodeProcess.getOutputStream();
    } catch (Exception e) {
      throw new SchedulingGoalDSLCompilationException("Could not create node subprocess: ", e);
    }
  }

  public void close() {
    this.nodeProcess.destroy();
  }

  record GoalDefinition(String kind, WindowExpression windowExpression, ActivityTemplate activityTemplate, List<Integer> rangeToGenerate) {}

  record WindowExpression(String kind) {}
  record ActivityTemplate(String name, String activityType, Map<String, SerializedValue> arguments) {}

  private static final JsonParser<WindowExpression> windowsP =
      productP
          .field("kind", stringP)
          .map(Iso.of(WindowExpression::new, WindowExpression::kind));

  private static final JsonParser<ActivityTemplate> activityTemplateP =
      productP
          .field("name", stringP)
          .field("activityType", stringP)
          .field("arguments", mapP(serializedValueP))
              .map(Iso.of(untuple(ActivityTemplate::new),
                          $ -> tuple($.name(), $.activityType(), $.arguments())));

  private static final JsonParser<GoalDefinition> schedulingJsonP =
      productP
          .field("kind", stringP)
          .field("windows", windowsP)
          .field("activityTemplate", activityTemplateP)
          .field("rangeToGenerate", listP(intP))
          .map(Iso.of(untuple(GoalDefinition::new),
              goalDefinition -> tuple(goalDefinition.kind(), goalDefinition.windowExpression(), goalDefinition.activityTemplate(), goalDefinition.rangeToGenerate())));

  // {"kind":"ActivityRecurrenceGoal","windows":{"kind":"ConstraintOperatorEntirePlanWindow"},"activityTemplate":{"name":"some goal","activityType":"PeelBanana","arguments":{"peelDirection":"fromStem"}},"rangeToGenerate":[1,1]}

  /**
   * NOTE: This method is not re-entrant (assumes only one call to this method is running at any given time)
   */
  public GoalDefinition compileSchedulingGoalDSL(final String goalTypescript, final String goalName) //, /* Something about misson model*/)
  throws SchedulingGoalDSLCompilationException, IOException
  {
    /*
    * PROTOCOL:
    *   denote this java program as JAVA, and the node subprocess as NODE
    *
    *   JAVA -- stdin --> NODE: { "source": "sourcecode", "filename": "goalname" } \n
    *   NODE -- stdout --> JAVA: one of "success\n" or "error\n"
    *   NODE -- stdout --> JAVA: payload associated with success or failure, must be exactly one line terminated with \n
    * */
    final var inputWriter = this.nodeProcess.outputWriter();
    final var outputReader = this.nodeProcess.inputReader();

    final var quotedGoalTypescript = JSONObject.quote(goalTypescript); // adds extra quotes to start and end
    inputWriter.write("{ \"source\": " + quotedGoalTypescript + ", \"filename\": \"" + goalName + "\" }\n");
    inputWriter.flush();

    final var status = outputReader.readLine();
    if (status.equals("error")) {
      throw new SchedulingGoalDSLCompilationException(outputReader.readLine());
    }

    if (status.equals("success")) {
      final var output = outputReader.readLine();
      try {
        return parseJson(output, schedulingJsonP);
      } catch (InvalidJsonException | InvalidEntityException e) {
        throw new SchedulingGoalDSLCompilationException("Could not parse JSON returned from typescript: ", e);
      }
    }

    // Status was neither failure nor success, the protocol has been violated.
    throw new Error("scheduling dsl compiler returned unexpected status: " + status);
  }

  record ActivityType(String name, Map<String, ValueSchema> parameters) {}
  record ResourceType(String name, String type, ValueSchema schema) {}
  record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}

  // /subsystem/field/id

//  public MissionModelTypes queryActivityTypesAndResourceTypes(long missionModelId) {
//
//  }

  public static String generateTypescriptTypesFromMissionModel(MissionModelTypes missionModelTypes) {
    // Generate stuff
    final var activityTypeCodes = new ArrayList<ActivityTypeCode>();
    for (final var activityType : missionModelTypes.activityTypes()) {
      activityTypeCodes.add(getActivityTypeInformation(activityType));
    }
    var result = "/** Start Codegen */\n";
    for (final var activityTypeCode : activityTypeCodes) {
      result += activityTypeCode.declaration();
    }
    result += "export const ActivityTemplates = {\n";
    for (final var activityTypeCode : activityTypeCodes) {
      result += activityTypeCode.implementation();
    }
    result += "}\n/** End Codegen */";
    return result;
  }

  private record ActivityTypeCode(String declaration, String implementation) {}

  private static ActivityTypeCode getActivityTypeInformation(ActivityType activityType) {
    // TODO handle args

    return new ActivityTypeCode(
        String.format("interface %s extends ActivityTemplate {}\n", activityType.name()),
        String.format("""
          %s: function %s(
            name: string,
            parameters: {
            %s
            }): %s {
            return {
              name,
              activityType: '%s',
              parameters: args,
            };
          },
        """, activityType.name(), activityType.name(), generateActivityParameterTypes(activityType).indent(2), activityType.name(), activityType.name())
    );
  }

  @NotNull
  private static String generateActivityParameterTypes(final ActivityType activityType) {
    var result = new ArrayList<>();
    for (final var param : activityType.parameters().entrySet()) {
      final var name = param.getKey();
      final var valueSchema = param.getValue();
      result.add(String.format("%s: %s,", name, valueSchemaToTypescripType(valueSchema)));
    }
    return String.join("\n", result.toArray(new String[0]));
  }

  private static String valueSchemaToTypescripType(ValueSchema valueSchema) {
    return "SOMESTATICVALUE";
  }

  private static <T> T parseJson(final String jsonStr, final JsonParser<T> parser) throws InvalidJsonException, InvalidEntityException {
    try (final var reader = Json.createReader(new StringReader(jsonStr))) {
      final var requestJson = reader.readValue();
      final var result = parser.parse(requestJson);
      return result.getSuccessOrThrow(reason -> new InvalidEntityException(List.of(reason)));
    } catch (JsonParsingException e) {
      throw new InvalidJsonException(e);
    }
  }

  public static class SchedulingGoalDSLCompilationException extends Exception {
    SchedulingGoalDSLCompilationException(final String message, final Exception e) {
      super(message, e);
    }
    SchedulingGoalDSLCompilationException(final String message) {
      super(message);
    }
  }
}
