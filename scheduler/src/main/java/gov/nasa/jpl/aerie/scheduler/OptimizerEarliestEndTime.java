package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

public class OptimizerEarliestEndTime extends Optimizer {

  Duration currentEarliestEndTime = null;

  @Override
  public boolean isBetterThanCurrent(List<ActivityInstance> candidateGoalSolution) {
    ActivityInstance act = ActivityInstance.getActWithEarliestEndTtime(candidateGoalSolution);

    if (currentEarliestEndTime == null || act.getEndTime().shorterThan(currentEarliestEndTime)) {
      currentGoalSolution = candidateGoalSolution;
      currentEarliestEndTime = act.getEndTime();
      return true;
    }
    return false;
  }


}
