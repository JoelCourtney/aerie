package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityConflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;

import java.util.List;

/**
 * describes the desired recurrence of an activity every time period
 */
public class RecurrenceGoal extends ActivityTemplateGoal {

  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public static class Builder extends ActivityTemplateGoal.Builder<Builder> {

    /**
     * specifies the fixed interval over which the activity should repeat
     *
     * this interval is understood to be a minimum (ie more frequent repetition
     * does not impede the goal's satisfaction)
     *
     * this specifier is required. it replaces any previous specification.
     *
     * @param interval IN the duration over which a matching activity instance
     *     must exist in order to satisfy the goal
     * @return this builder, ready for additional specification
     */
    public Builder repeatingEvery(Duration interval) {
      this.every = Window.at(interval);
      return getThis();
    }

    protected Window every;

    /**
     * {@inheritDoc}
     */
    @Override
    public RecurrenceGoal build() { return fill(new RecurrenceGoal()); }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Builder getThis() { return this; }
    /**
     * populates the provided goal with specifiers from this builder and above
     *
     * typically called by any derived builder classes to fill in the
     * specifiers managed at this builder level and above
     *
     * @param goal IN/OUT a goal object to be filled with specifiers from this
     *     level of builder and above
     * @return the provided object, with details filled in
     */
    protected RecurrenceGoal fill(RecurrenceGoal goal) {
      //first fill in any general specifiers from parents
      super.fill(goal);

      if (every == null) {
        throw new IllegalArgumentException(
            "creating recurrence goal requires non-null \"every\" duration interval");
      }
      goal.recurrenceInterval = every;

      return goal;
    }

  }//Builder


  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching target activity instance does not
   * exist over a timespan longer than the allowed range (and one should
   * probably be created!)
   */
  public java.util.Collection<Conflict> getConflicts(Plan plan, final SimulationResults simulationResults) {
    final var conflicts = new java.util.LinkedList<Conflict>();

    //collect all matching target acts ordered by start time
    //REVIEW: could collapse with prior template start time query too?
    final var satisfyingActSearch = new ActivityExpression.Builder()
        .basedOn(desiredActTemplate)
        .startsIn(getTemporalContext()).build();
    final var acts = new java.util.LinkedList<>(plan.find(satisfyingActSearch, simulationResults));
    acts.sort(java.util.Comparator.comparing(ActivityInstance::getStartTime));

    //walk through existing matching activities to find too-large gaps,
    //starting from the goal's own start time
    //REVIEW: some clever implementation with stream reduce / combine?
    final var actI = acts.iterator();
    final var lastStartT = getTemporalContext().end;
    var prevStartT = getTemporalContext().start;
    while (actI.hasNext() && prevStartT.compareTo(lastStartT) < 0) {
      final var act = actI.next();
      final var actStartT = act.getStartTime();

      //check if the inter-activity gap is too large
      //REVIEW: should do any check based on min gap duration?
      final var strideDur = actStartT.minus(prevStartT);
      if (strideDur.compareTo(recurrenceInterval.end) > 0) {
        //fill conflicts for all the missing activities in that long span
        conflicts.addAll(makeRecurrenceConflicts(prevStartT, actStartT));

      } else {
        /*TODO: right now, we associate with all the activities that are satisfying but we should aim for the minimum
        set which itself is a combinatoric problem */
        var planEval = plan.getEvaluation();
        if(!planEval.forGoal(this).getAssociatedActivities().contains(act) && planEval.canAssociateMoreToCreatorOf(act)) {
          conflicts.add(new MissingAssociationConflict(this, List.of(act)));
        }
      }

      prevStartT = actStartT;
    }

    //fill in conflicts for all missing activities in the last span up to the
    //goal's own end time (also handles case of no matching acts at all)
    conflicts.addAll(makeRecurrenceConflicts(prevStartT, lastStartT));

    return conflicts;
  }

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected RecurrenceGoal() { }

  /**
   * the allowable range of durations over which a target activity must repeat
   *
   * REVIEW: need to work out semantics of min on recurrenceInterval
   */
  protected Window recurrenceInterval;

  /**
   * creates conflicts for missing activities in the provided span
   *
   * there may be more than one conflict if the span is larger than
   * the maximum allowed recurrence interval. there may be no conflicts
   * if the span is less than the maximum interval.
   *
   * @param start IN the start time of the span to fill with conflicts (inclusive)
   * @param end IN the end time of the span to fill with conflicts (exclusive)
   */
  private java.util.Collection<MissingActivityConflict> makeRecurrenceConflicts(
      Duration start, Duration end)
  {
    final var conflicts = new java.util.LinkedList<MissingActivityConflict>();

    if(end.minus(start).noLongerThan(recurrenceInterval.end)){
      return conflicts;
    }

    for (var intervalT = start.plus(recurrenceInterval.end);
         ;
         intervalT = intervalT.plus(recurrenceInterval.end)
    ) {
      var interval = Window.between(intervalT.minus(recurrenceInterval.end), Duration.min(intervalT, end));
        conflicts.add(new MissingActivityTemplateConflict(
            this, new Windows(
            interval), this.getActTemplate()));
      if(intervalT.compareTo(end) >= 0){
        break;
      }
    }

    return conflicts;
  }
}
