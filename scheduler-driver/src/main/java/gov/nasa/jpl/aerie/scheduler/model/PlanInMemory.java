package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * an in-memory solution to a planning problem including a schedule of activities
 *
 * may only be a partial solution to the whole planning problem, ie some
 * goals may be left unsatisfied
 */
public class PlanInMemory implements Plan {

  /**
   * the set of all evaluations posted to the plan
   *
   * note that different solvers may evaluate the same plan differently
   */
  protected Evaluation evaluation;

  /**
   * container of all activity instances in plan, indexed by start time
   */
  private final TreeMap<Duration, List<SchedulingActivityDirective>> actsByTime;

  /**
   * ctor creates a new empty solution plan
   *
   */
  public PlanInMemory() {
    this.actsByTime = new TreeMap<>();
  }

  @Override
  public void replace(final SchedulingActivityDirective toBeReplaced, final SchedulingActivityDirective replacement) {
    if(evaluation != null) evaluation.replace(toBeReplaced, replacement);
    remove(toBeReplaced);
    add(replacement);
  }

  public PlanInMemory(final PlanInMemory other){
    if(other.evaluation != null) this.evaluation = other.evaluation.duplicate();
    this.actsByTime = new TreeMap<>();
    for(final var entry: other.actsByTime.entrySet()){
      this.actsByTime.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
  }

  @Override
  public Plan duplicate() {
    return new PlanInMemory(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Collection<SchedulingActivityDirective> acts) {
    for (final var act : acts) {
      add(act);
    }
  }

  public int size(){
    int size = 0;
    for(final var entry: this.actsByTime.entrySet()){
      size += entry.getValue().size();
    }
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(final SchedulingActivityDirective act) {
    if (act == null) {
      throw new IllegalArgumentException(
          "adding null activity to plan");
    }
    final var startT = act.startOffset();
    if (startT == null) {
      throw new IllegalArgumentException(
          "adding activity with null start time to plan");
    }
    final var id = act.getId();
    assert id != null;
    actsByTime.computeIfAbsent(startT, k -> new LinkedList<>())
              .add(act);
  }

  @Override
  public void remove(Collection<SchedulingActivityDirective> acts) {
    for (var act : acts) {
      remove(act);
    }
  }

  @Override
  public void remove(SchedulingActivityDirective act) {
    var acts = actsByTime.get(act.startOffset());
    if (acts != null) acts.remove(act);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SchedulingActivityDirective> getActivitiesByTime() {
    //REVIEW: could probably do something tricky with streams to avoid new
    final var orderedActs = new LinkedList<SchedulingActivityDirective>();

    //NB: tree map ensures that values are in key order, but still need to flatten
    for (final var actsAtT : actsByTime.values()) {
      assert actsAtT != null;
      orderedActs.addAll(actsAtT);
    }

    return Collections.unmodifiableList(orderedActs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<ActivityType, List<SchedulingActivityDirective>> getActivitiesByType() {
    final var map = new HashMap<ActivityType, List<SchedulingActivityDirective>>();
    for(final var entry: this.actsByTime.entrySet()){
      for(final var activity : entry.getValue()){
        map.computeIfAbsent(activity.type(), t -> new ArrayList<>()).add(activity);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public Map<SchedulingActivityDirectiveId, SchedulingActivityDirective> getActivitiesById() {
    final var map = new HashMap<SchedulingActivityDirectiveId, SchedulingActivityDirective>();
    for(final var entry: this.actsByTime.entrySet()){
      for(final var activity : entry.getValue()){
        map.put(activity.id(), activity);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<SchedulingActivityDirective> getActivities() {
    final var set = new HashSet<SchedulingActivityDirective>();
    for(final var entry: this.actsByTime.entrySet()){
      set.addAll(entry.getValue());
    }
    return Collections.unmodifiableSet(set);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<SchedulingActivityDirective> find(
      ActivityExpression template, SimulationResults simulationResults,
      EvaluationEnvironment evaluationEnvironment)
  {
    //REVIEW: could do something clever with returning streams to prevent wasted work
    //REVIEW: something more clever for time-based queries using time index
    LinkedList<SchedulingActivityDirective> matched = new LinkedList<>();
    for (final var actsAtTime : actsByTime.values()) {
      for (final var act : actsAtTime) {
        if (template.matches(act, simulationResults, evaluationEnvironment, true)) {
          matched.add(act);
        }
      }
    }
    return matched;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addEvaluation(Evaluation eval) {
    evaluation = eval;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Evaluation getEvaluation() {
    return evaluation;
  }

}
