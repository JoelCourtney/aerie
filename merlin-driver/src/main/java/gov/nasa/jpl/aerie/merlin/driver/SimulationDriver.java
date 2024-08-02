package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanException;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskEntryPoint;
import gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class SimulationDriver {
  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Supplier<Boolean> simulationCanceled
  )
  {
    return simulate(
        missionModel,
        schedule,
        simulationStartTime,
        simulationDuration,
        planStartTime,
        planDuration,
        simulationCanceled,
        $ -> {});
  }

  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Supplier<Boolean> simulationCanceled,
      final Consumer<Duration> simulationExtentConsumer
  ) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();

      final LiveCells cells;

      final String inconsFile = "fincons.json";
      if (new File(inconsFile).exists()) {
        try {
          cells = new LiveCells(timeline);
          final var incons = new SerializedValueJsonParser().parse(
              Json
                  .createReader(new StringReader(Files.readString(Path.of(inconsFile))))
                  .readValue()).getSuccessOrThrow();
          final var serializedCells = incons.asMap().get().get("cells").asList().get();
          final var queries = missionModel.getInitialCells().queries();

          for (int i = 0; i < serializedCells.size(); i++) {
            final var serializedCell = serializedCells.get(i);
            final var query = queries.get(i);

            putCell(cells, query, serializedCell, missionModel.getInitialCells());
          }

          for (final var serializedTask : incons.asMap().get().get("tasks").asList().get()) {
            final var entrypoint = serializedTask.asMap().get().get("entrypoint").asMap().get();
            final var directive = entrypoint.get("directive").asMap().get();
            final var startOffset = Duration.of(directive.get("startOffset").asInt().get(), Duration.MICROSECONDS);
            final var type = directive.get("type").asString().get();
            final var args = directive.get("args").asMap().get();

            final var steps = serializedTask.asMap().get().get("steps").asInt().get();
            final var reads = serializedTask.asMap().get().get("reads").asList().get();

            final var readIterator = reads.iterator();

            final var lastStepTime = Duration.of(entrypoint.get("lastStepTime").asInt().get(), Duration.MICROSECONDS);

            final var taskFactory = deserializeActivity(missionModel, new SerializedActivity(type, args));
            final var task = taskFactory.create(engine.executor);
            final var scheduler = new Scheduler() {
              @Override
              public <State> State get(final CellId<State> cellId) {
                final var readValue = readIterator.next();

              }

              @Override
              public <Event> void emit(final Event event, final Topic<Event> topic) {

              }

              @Override
              public void spawn(final InSpan taskSpan, final TaskFactory<?> task) {

              }
            };
            TaskStatus<?> status = null;
            for (var i = 0; i < steps; i++) {
              status = task.step(scheduler);
            }
            final TaskStatus<?> $status =
                switch (status) {
                  case TaskStatus.AwaitingCondition<?> s -> s;
                  case TaskStatus.CallingTask<?> s -> s;
                  case TaskStatus.Completed<?> s -> s;
                  case TaskStatus.Delayed<?> s -> TaskStatus.delayed(s.delay().minus(lastStepTime), s.continuation());
                  case null -> null;
                };
            if ($status == null) {
              engine.scheduleTask(ZERO, $ -> task, new TaskEntryPoint.Directive(new SerializedActivity(type, args)));
            } else {
              engine.scheduleTask(ZERO, $ -> Task.of($$ -> $status).andThen(task), new TaskEntryPoint.Directive(new SerializedActivity(type, args)));
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        cells = new LiveCells(timeline, missionModel.getInitialCells());
      }

      /* The current real time. */
      var elapsedTime = ZERO;

      simulationExtentConsumer.accept(elapsedTime);

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityDirectiveId>();

      try {
        // Start daemon task(s) immediately, before anything else happens.
        engine.scheduleTask(ZERO, missionModel.getDaemon(), new TaskEntryPoint.Daemon());
        {
          final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
          timeline.add(commit.getLeft());
          if(commit.getRight().isPresent()) {
            throw commit.getRight().get();
          }
        }

        // Get all activities as close as possible to absolute time
        // Schedule all activities.
        // Using HashMap explicitly because it allows `null` as a key.
        // `null` key means that an activity is not waiting on another activity to finish to know its start time
        HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, schedule).compute();
        if (!resolved.isEmpty()) {
          resolved.put(
              null,
              StartOffsetReducer.adjustStartOffset(
                  resolved.get(null),
                  Duration.of(
                      planStartTime.until(simulationStartTime, ChronoUnit.MICROS),
                      Duration.MICROSECONDS)));
        }
        // Filter out activities that are before simulationStartTime
        resolved = StartOffsetReducer.filterOutNegativeStartOffset(resolved);

        scheduleActivities(
            schedule,
            resolved,
            missionModel,
            engine,
            activityTopic
        );

        // Drive the engine until we're out of time or until simulation is canceled.
        // TERMINATION: Actually, we might never break if real time never progresses forward.
        while (!simulationCanceled.get()) {
          final var batch = engine.extractNextJobs(simulationDuration);

          // Increment real time, if necessary.
          final var delta = batch.offsetFromStart().minus(elapsedTime);
          elapsedTime = batch.offsetFromStart();
          timeline.add(delta);
          // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
          //   even if they occur at the same real time.

          simulationExtentConsumer.accept(elapsedTime);

          if (simulationCanceled.get() ||
              (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration))) {
            break;
          }

          // Run the jobs in this batch.
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
          timeline.add(commit.getLeft());
          if (commit.getRight().isPresent()) {
            throw commit.getRight().get();
          }
        }
      } catch (SpanException ex) {
        // Swallowing the spanException as the internal `spanId` is not user meaningful info.
        final var topics = missionModel.getTopics();
        final var directiveId = SimulationEngine.getDirectiveIdFromSpan(engine, activityTopic, timeline, topics, ex.spanId);
        if(directiveId.isPresent()) {
          throw new SimulationException(elapsedTime, simulationStartTime, directiveId.get(), ex.cause);
        }
        throw new SimulationException(elapsedTime, simulationStartTime, ex.cause);
      } catch (Throwable ex) {
        throw new SimulationException(elapsedTime, simulationStartTime, ex);
      }

      System.out.println("-----------------------------------------");
      final var serializedCells = new ArrayList<SerializedValue>();
      for (final var query : missionModel.getInitialCells().queries()) {
        final var cell = cells.getCell(query);
        serializedCells.add(cell.get().serialize());
      }
      final var serializedTasks = new ArrayList<SerializedValue>();

      // Task needs:
      // - entry point
      // - reads
      // The entry point is either:
      //   - An activity directive
      //   - An activity directive with a path to a particular anonymous subtask. Enumerate calls and spawns. E.g:
      //           Parent()->2->1 is the first child of the second child of Parent
      // The list of reads is linear and contains parent and child reads combined???
      //    Can we dedupe? Later...
      // Number of steps

      try {
        // get unfinished tasks
        // for each unfinished task, get its entrypoint

        for (final var task : engine.tasks.keySet()) {
          final var entrypoint = engine.entrypoints.get(task);
          if (!(entrypoint instanceof TaskEntryPoint.Directive e)) continue;
          serializedTasks.add(SerializedValue.of(Map.of(
              "entrypoint", SerializedValue.of(Map.of(
                  "directive", SerializedValue.of(Map.of(
                      "startOffset",
                      SerializedValue.of(Duration
                                             .of(295, Duration.HOURS)
                                             .plus(Duration.of(10, Duration.MINUTES))
                                             .in(Duration.MICROSECONDS)),
                      "type",
                      SerializedValue.of(e.directive().getTypeName()),
                      "args",
                      SerializedValue.of(e.directive().getArguments()))),
                  "lastStepTime", SerializedValue.of(Duration
                                                         .of(14 * 24, Duration.HOURS)
                                                         .minus(Duration
                                                                    .of(295, Duration.HOURS)
                                                                    .plus(Duration.of(10, Duration.MINUTES)))
                                                         .in(Duration.MICROSECONDS))
              )),
              "reads", SerializedValue.of(List.of()),
              "steps", SerializedValue.of(1)
          )));
        }

        Files.write(Path.of(inconsFile), List.of(new SerializedValueJsonParser().unparse(SerializedValue.of(Map.of(
            "cells", SerializedValue.of(serializedCells),
            "tasks", SerializedValue.of(serializedTasks)
        ))).toString()), StandardOpenOption.CREATE);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      final var topics = missionModel.getTopics();
      return SimulationEngine.computeResults(engine, simulationStartTime, elapsedTime, activityTopic, timeline, topics);
    }
  }

  private static <State> void putCell(
      LiveCells liveCells,
      Query<State> query,
      SerializedValue serializedCell,
      LiveCells cell)
  {
    liveCells.put(query, cell.getCell(query).get().deserialize(serializedCell));
  }

  // This method is used as a helper method for executing unit tests
  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final TaskFactory<Return> task) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(ZERO, missionModel.getDaemon(), new TaskEntryPoint.Daemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
          timeline.add(commit.getLeft());
          if(commit.getRight().isPresent()) {
             throw new RuntimeException("Exception thrown while starting daemon tasks", commit.getRight().get());
          }
      }

      // Schedule all activities.
      final var spanId = engine.scheduleTask(elapsedTime, task, new TaskEntryPoint.Daemon());

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.getSpan(spanId).isComplete()) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit.getLeft());
        if(commit.getRight().isPresent()) {
          throw new RuntimeException("Exception thrown while simulating tasks", commit.getRight().get());
        }
      }
    }
  }

  private static <Model> void scheduleActivities(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final SimulationEngine engine,
      final Topic<ActivityDirectiveId> activityTopic
  ) {
    if (resolved.get(null) == null) {
      // Nothing to simulate
      return;
    }
    for (final Pair<ActivityDirectiveId, Duration> directivePair : resolved.get(null)) {
      final var directiveId = directivePair.getLeft();
      final var startOffset = directivePair.getRight();
      final var serializedDirective = schedule.get(directiveId).serializedActivity();

      final TaskFactory<?> task = deserializeActivity(missionModel, serializedDirective);

      engine.scheduleTask(startOffset, makeTaskFactory(
          directiveId,
          task,
          schedule,
          resolved,
          missionModel,
          activityTopic
      ), new TaskEntryPoint.Directive(schedule.get(directiveId).serializedActivity()));
    }
  }

  private static <Model, Output> TaskFactory<Unit> makeTaskFactory(
      final ActivityDirectiveId directiveId,
      final TaskFactory<Output> taskFactory,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved,
      final MissionModel<Model> missionModel,
      final Topic<ActivityDirectiveId> activityTopic
  ) {
    record Dependent(Duration offset, TaskFactory<?> task) {}

    final List<Dependent> dependents = new ArrayList<>();
    for (final var pair : resolved.getOrDefault(directiveId, List.of())) {
      dependents.add(new Dependent(
          pair.getRight(),
          makeTaskFactory(
              pair.getLeft(),
              deserializeActivity(missionModel, schedule.get(pair.getLeft()).serializedActivity()),
              schedule,
              resolved,
              missionModel,
              activityTopic)));
    }

    return executor -> {
      final var task = taskFactory.create(executor);
      return Task
          .callingWithSpan(
              Task.emitting(directiveId, activityTopic)
                  .andThen(task))
          .andThen(
              Task.spawning(
                  dependents
                      .stream()
                      .map(
                          dependent ->
                              TaskFactory.delaying(dependent.offset())
                                         .andThen(dependent.task()))
                      .toList()));
    };
  }

  private static <Model> TaskFactory<?> deserializeActivity(MissionModel<Model> missionModel, SerializedActivity serializedDirective) {
    final TaskFactory<?> task;
    try {
      task = missionModel.getTaskFactory(serializedDirective);
    } catch (final InstantiationException ex) {
      // All activity instantiations are assumed to be validated by this point
      throw new Error("Unexpected state: activity instantiation %s failed with: %s"
                          .formatted(serializedDirective.getTypeName(), ex.toString()));
    }
    return task;
  }
}
