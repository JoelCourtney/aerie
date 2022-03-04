package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.json.JsonParser;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidEntityException;
import gov.nasa.jpl.aerie.scheduler.server.http.InvalidJsonException;
import gov.nasa.jpl.aerie.scheduler.server.models.SchedulingDSL;
import org.json.JSONObject;

import javax.json.Json;
import javax.json.stream.JsonParsingException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class SchedulingDSLCompilationService {

  private final Process nodeProcess;

  public SchedulingDSLCompilationService() throws SchedulingDSLCompilationException, IOException {
    final var schedulingDslCompilerRoot = System.getenv("SCHEDULING_DSL_COMPILER_ROOT");
    final var schedulingDslCompilerCommand = System.getenv("SCHEDULING_DSL_COMPILER_COMMAND");
    this.nodeProcess = Runtime.getRuntime().exec(new String[]{"node", schedulingDslCompilerCommand}, null, new File(schedulingDslCompilerRoot));

    final var inputStream = this.nodeProcess.outputWriter();
    inputStream.write("ping\n");
    inputStream.flush();
    if (!"pong".equals(this.nodeProcess.inputReader().readLine())) {
      throw new SchedulingDSLCompilationException("Could not create node subprocess");
    }
  }

  public void close() {
    this.nodeProcess.destroy();
  }

  /**
   * NOTE: This method is not re-entrant (assumes only one call to this method is running at any given time)
   */
  public SchedulingDSL.GoalSpecifier compileSchedulingGoalDSL(final String goalTypescript, final String goalName)
  throws SchedulingDSLCompilationException
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
    try {
      inputWriter.write("{ \"source\": " + quotedGoalTypescript + ", \"filename\": \"" + goalName + "\" }\n");
      inputWriter.flush();
      final var status = outputReader.readLine();
      if ("error".equals(status)) {
        throw new SchedulingDSLCompilationException(outputReader.readLine());
      }
      if ("success".equals(status)) {
        final var output = outputReader.readLine();
        try {
          return parseJson(output, SchedulingDSL.schedulingJsonP);
        } catch (InvalidJsonException | InvalidEntityException e) {
          throw new SchedulingDSLCompilationException("Could not parse JSON returned from typescript: ", e);
        }
      }
      // Status was neither failure nor success, the protocol has been violated.
      throw new Error("scheduling dsl compiler returned unexpected status: " + status);
    } catch (IOException e) {
      throw new Error(e);
    }
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

  public static class SchedulingDSLCompilationException extends Exception {
    SchedulingDSLCompilationException(final String message, final Exception e) {
      super(message, e);
    }
    SchedulingDSLCompilationException(final String message) {
      super(message);
    }
  }
}
