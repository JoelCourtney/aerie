package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * Filter keeping windows with a duration superior or equal to a defined minimum duration
 */
public class FilterMinDuration extends FilterFunctional {
  private final Duration minDuration;

  public FilterMinDuration(Duration filterByDuration) {
    this.minDuration = filterByDuration;
  }

  @Override
  public Windows filter(SimulationResults simulationResults, Plan plan, Windows windows) {
    Windows result = new Windows(windows);
    result = result.filterByDuration(this.minDuration, Duration.MAX_VALUE);
    return result;
  }

  @Override
  public boolean shouldKeep(SimulationResults simulationResults, Plan plan, Window range) {
    return range.duration().noShorterThan(minDuration);
  }

}
