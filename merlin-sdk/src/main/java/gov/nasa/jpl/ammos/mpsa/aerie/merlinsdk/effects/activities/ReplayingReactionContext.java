package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.pcollections.ConsPStack;
import org.pcollections.PStack;
import org.pcollections.PVector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ReplayingReactionContext<T, Activity, Event> implements ReactionContext<T, Activity, Event> {
  private PStack<ActivityContinuation<T, Event, Activity>> spawns = ConsPStack.empty();
  private PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;
  private int nextBreadcrumbIndex;

  private final ReplayingActivityReactor<T, Event, Activity> reactor;
  private final Consumer<ScheduleItem<T, Event>> scheduler;
  private History<T, Event> currentHistory;
  private final Set<String> children = new HashSet<>();

  public ReplayingReactionContext(
      final ReplayingActivityReactor<T, Event, Activity> reactor,
      final Consumer<ScheduleItem<T, Event>> scheduler,
      final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs)
  {
    this.reactor = reactor;
    this.scheduler = scheduler;
    this.breadcrumbs = breadcrumbs;

    this.nextBreadcrumbIndex = 0;
    this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumbs.get(this.nextBreadcrumbIndex++)).next;
  }

  public final History<T, Event> getCurrentHistory() {
    return this.currentHistory;
  }

  public final PVector<ActivityBreadcrumb<T, Event>> getBreadcrumbs() {
    return this.breadcrumbs;
  }

  public final PStack<ActivityContinuation<T, Event, Activity>> getSpawns() {
    return this.spawns;
  }

  @Override
  public History<T, Event> now() {
    return this.currentHistory;
  }

  @Override
  public final void emit(final Event event) {
    this.currentHistory = this.currentHistory.emit(event);
  }

  public final void delay(final Duration duration) {
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      throw new Defer(duration);
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on delay(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  @Override
  public final void waitForActivity(final String activityId) {
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      throw new Await(activityId);
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
        throw new RuntimeException("Unexpected breadcrumb on waitForActivity(): " + breadcrumb.getClass().getName());
      }

      this.currentHistory = ((ActivityBreadcrumb.Advance<T, Event>) breadcrumb).next;
    }
  }

  public final void waitForChildren() {
    for (final var child : this.children) this.waitForActivity(child);
  }

  @Override
  public final String spawn(final Activity child) {
    final String childId;
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      this.currentHistory = this.currentHistory.fork();

      final var continuation = this.reactor.createSimulationTask(child).advancedTo(this.currentHistory);
      childId = continuation.getId();

      this.spawns = this.spawns.plus(continuation);
      this.breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new RuntimeException("Unexpected breadcrumb; expected spawn, got " + breadcrumb.getClass().getName());
      }

      childId = ((ActivityBreadcrumb.Spawn<T, Event>) breadcrumb).activityId;
    }

    this.children.add(childId);
    return childId;
  }

  @Override
  public String spawnAfter(final Duration delay, final Activity child) {
    final String childId;
    if (this.nextBreadcrumbIndex >= breadcrumbs.size()) {
      final var continuation = this.reactor.createSimulationTask(child);
      childId = continuation.getId();

      this.scheduler.accept(new ScheduleItem.Defer<>(delay, continuation));
      this.breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
      this.nextBreadcrumbIndex += 1;
    } else {
      final var breadcrumb = this.breadcrumbs.get(this.nextBreadcrumbIndex++);
      if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
        throw new RuntimeException("Unexpected breadcrumb; expected spawn, got " + breadcrumb.getClass().getName());
      }

      childId = ((ActivityBreadcrumb.Spawn<T, Event>) breadcrumb).activityId;
    }

    this.children.add(childId);
    return childId;
  }

  public static final class Defer extends RuntimeException {
    public final Duration duration;

    private Defer(final Duration duration) {
      this.duration = duration;
    }
  }

  public static final class Await extends RuntimeException {
    public final String activityId;

    private Await(final String activityId) {
      this.activityId = activityId;
    }
  }
}
