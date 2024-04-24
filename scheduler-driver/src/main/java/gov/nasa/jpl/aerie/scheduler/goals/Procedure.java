package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.scheduler.ProcedureLoader;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;
import gov.nasa.jpl.aerie.timeline.CollectOptions;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.timeline.Interval;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class Procedure extends Goal {
//  private final gov.nasa.jpl.aerie.scheduling.Procedure procedure;
  private final String jarPath;

  public Procedure(final PlanningHorizon planningHorizon, String jarPath) {
    this.simulateAfter = true;
    this.planHorizon = planningHorizon;
    this.jarPath = jarPath;
  }

  public void run(Evaluation eval, Plan plan, MissionModel<?> missionModel, Function<String, ActivityType> lookupActivityType) {
    final gov.nasa.jpl.aerie.scheduling.Procedure procedure;
    try {
        procedure = ProcedureLoader.loadProcedure(Path.of(jarPath));
    } catch (ProcedureLoader.ProcedureLoadException e) {
        throw new RuntimeException(e);
    }

    List<SchedulingActivityDirective> newActivities = new ArrayList<>();

    final var editablePlan = EditablePlanImpl.init(missionModel, new Interval(planHorizon.getStartAerie(), planHorizon.getEndAerie()), null);

    /*
     TODO

     Comments from Joel:
     - Part of the intent of editablePlan was to be able to re-use it across procedures.
       - Could be done by initializing EditablePlanImpl with simulation results

     Duration construction and arithmetic can be less awkward
     */

    final var options = new CollectOptions(
        new Interval(Duration.ZERO,
                     Duration.microseconds(planHorizon.getEndAerie().in(gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS))));

    procedure.run(editablePlan, options);

    for (final var newDirective : editablePlan.committed()) {
      newActivities.add(SchedulingActivityDirective.of(lookupActivityType.apply(newDirective.getType()), newDirective.getStartTime(), Duration.ZERO, newDirective.getInner().arguments, null, null, true));
    }

    final var evaluation = eval.forGoal(this);
    for (final var activity : newActivities) {
      plan.add(activity);
      evaluation.associate(activity, true);
    }
    evaluation.setScore(0.0);
  }
}
