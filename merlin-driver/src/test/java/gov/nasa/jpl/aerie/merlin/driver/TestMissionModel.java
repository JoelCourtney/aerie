package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.DirectiveType;
import gov.nasa.jpl.aerie.merlin.protocol.model.InputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.model.OutputType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestMissionModel {
  private final static Duration oneMinute = Duration.of(60, Duration.SECONDS);
  private static final Topic<Object> delayedActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> delayedActivityDirectiveOutputTopic = new Topic<>();

  static List<Triple<Integer, String, ValueSchema>> getModelTopicList() {
    return Arrays.asList(
      Triple.of(0, "ActivityType.Input.DelayActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(1, "ActivityType.Output.DelayActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(2, "ActivityType.Input.DecomposingActivityDirective", new ValueSchema.StructSchema(Map.of())),
      Triple.of(3, "ActivityType.Output.DecomposingActivityDirective", new ValueSchema.StructSchema(Map.of())));
  }

  /* package-private*/ static final DirectiveType<Object, Object, Object> delayedActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> new OneStepTask<>($ -> {
        $.startActivity(this, delayedActivityDirectiveInputTopic);
        return TaskStatus.delayed(oneMinute, new OneStepTask<>($$ -> {
          $$.endActivity(Unit.UNIT, delayedActivityDirectiveOutputTopic);
          return TaskStatus.completed(Unit.UNIT);
        }));
      });
    }
  };

  private static final Topic<Object> decomposingActivityDirectiveInputTopic = new Topic<>();
  private static final Topic<Object> decomposingActivityDirectiveOutputTopic = new Topic<>();
  /* package-private */  static final DirectiveType<Object, Object, Object> decomposingActivityDirective = new DirectiveType<>() {
    @Override
    public InputType<Object> getInputType() {
      return testModelInputType;
    }

    @Override
    public OutputType<Object> getOutputType() {
      return testModelOutputType;
    }

    @Override
    public TaskFactory<Object> getTaskFactory(final Object o, final Object o2) {
      return executor -> new OneStepTask<>(scheduler -> {
        scheduler.startActivity(this, decomposingActivityDirectiveInputTopic);
        return TaskStatus.delayed(
            Duration.ZERO,
            new OneStepTask<>($ -> {
              try {
                $.spawn(InSpan.Fresh, delayedActivityDirective.getTaskFactory(null, null));
              } catch (final InstantiationException ex) {
                throw new Error("Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                    ex.toString()));
              }
              return TaskStatus.delayed(Duration.of(120, Duration.SECOND), new OneStepTask<>($$ -> {
                try {
                  $$.spawn(InSpan.Fresh, delayedActivityDirective.getTaskFactory(null, null));
                } catch (final InstantiationException ex) {
                  throw new Error(
                      "Unexpected state: activity instantiation of DelayedActivityDirective failed with: %s".formatted(
                          ex.toString()));
                }
                $$.endActivity(Unit.UNIT, decomposingActivityDirectiveOutputTopic);
                return TaskStatus.completed(Unit.UNIT);
              }));
            }));
      });
    }
  };

  private static final InputType<Object> testModelInputType = new InputType<>() {
    @Override
    public List<Parameter> getParameters() {
      return List.of();
    }

    @Override
    public List<String> getRequiredParameters() {
      return List.of();
    }

    @Override
    public Object instantiate(final Map arguments) {
      return new Object();
    }

    @Override
    public Map<String, SerializedValue> getArguments(final Object value) {
      return Map.of();
    }

    @Override
    public List<ValidationNotice> getValidationFailures(final Object value) {
      return List.of();
    }
  };

  private static final OutputType<Object> testModelOutputType = new OutputType<>() {
    @Override
    public ValueSchema getSchema() {
      return ValueSchema.ofStruct(Map.of());
    }

    @Override
    public SerializedValue serialize(final Object value) {
      return SerializedValue.of(Map.of());
    }
  };

  private static final LinkedHashMap<Topic<?>, MissionModel.SerializableTopic<?>> _topics = new LinkedHashMap<>();
  static {
    _topics.put(delayedActivityDirectiveInputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Input.DelayActivityDirective",
                    delayedActivityDirectiveInputTopic,
                    testModelOutputType));
    _topics.put(delayedActivityDirectiveOutputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Output.DelayActivityDirective",
                    delayedActivityDirectiveOutputTopic,
                    testModelOutputType));
    _topics.put(decomposingActivityDirectiveInputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Input.DecomposingActivityDirective",
                    decomposingActivityDirectiveInputTopic,
                    testModelOutputType));
    _topics.put(decomposingActivityDirectiveOutputTopic,
                new MissionModel.SerializableTopic<>(
                    "ActivityType.Output.DecomposingActivityDirective",
                    decomposingActivityDirectiveOutputTopic,
                    testModelOutputType));
  }

  public static MissionModel<Object> missionModel() {
    return new MissionModel<>(
        new Object(),
        new LiveCells(new TemporalEventSource()),
        Map.of(),
        _topics,
        Map.of(),
        DirectiveTypeRegistry.extract(
            new ModelType<>() {

              @Override
              public Map<String, ? extends DirectiveType<Object, ?, ?>> getDirectiveTypes() {
                return Map.of(
                    "DelayActivityDirective",
                    delayedActivityDirective,
                    "DecomposingActivityDirective",
                    decomposingActivityDirective);
              }

              @Override
              public InputType<Object> getConfigurationType() {
                return testModelInputType;
              }

              @Override
              public Object instantiate(
                  final Instant planStart,
                  final Object configuration,
                  final Initializer builder)
              {
                return new Object();
              }
            }
        )
    );
  }
}
