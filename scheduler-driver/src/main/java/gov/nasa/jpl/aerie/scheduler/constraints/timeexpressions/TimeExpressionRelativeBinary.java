package gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;

public class TimeExpressionRelativeBinary extends TimeExpressionRelative {

  private final TimeExpressionRelativeSimple lowerBound;
  private final TimeExpressionRelativeSimple upperBound;

  public TimeExpressionRelativeBinary(TimeExpressionRelativeSimple lowerBound, TimeExpressionRelativeSimple upperBound) {
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  @Override
  public Interval computeTime(final SimulationResults simulationResults, final Plan plan, final Interval interval) {
    final var interval1 = lowerBound.computeTime(simulationResults, plan, interval);
    final var interval2 = upperBound.computeTime(simulationResults, plan, interval);
    return Interval.between(interval1.start, interval2.end);
  }

  public TimeAnchor getAnchor(){
    return this.lowerBound.anchor != null ? this.lowerBound.anchor : this.upperBound.anchor;
  }

}
