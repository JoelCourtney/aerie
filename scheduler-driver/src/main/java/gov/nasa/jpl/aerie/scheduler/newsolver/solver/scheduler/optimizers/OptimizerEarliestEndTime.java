package gov.nasa.jpl.aerie.scheduler.newsolver.solver.scheduler.optimizers;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;

import java.util.List;

public class OptimizerEarliestEndTime extends Optimizer {

  Duration currentEarliestEndTime = null;

  @Override
  public boolean isBetterThanCurrent(List<SchedulingActivityDirective> candidateGoalSolution) {
    SchedulingActivityDirective act = SchedulingActivityDirective.getActWithEarliestEndTime(candidateGoalSolution);
    if(act == null || act.duration() != null) {
      throw new IllegalStateException("Cannot optimize on uninstantiated activities");
    }
    if (currentEarliestEndTime == null || act.getEndTime().shorterThan(currentEarliestEndTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentEarliestEndTime = act.getEndTime();
      return true;
    }
    return false;
  }


}
