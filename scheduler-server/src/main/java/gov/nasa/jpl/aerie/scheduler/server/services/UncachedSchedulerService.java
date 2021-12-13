package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.remotes.InMemoryResultsCell;

import java.util.Objects;

/**
 * scheduler service that operates on local data objects in memory
 *
 * @param agent the agent that can be used to carry out scheduling requests
 */
public record UncachedSchedulerService(SchedulerAgent agent) implements SchedulerService {
  public UncachedSchedulerService {
    Objects.requireNonNull(agent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResultsProtocol.State scheduleActivities(final ScheduleRequest request) {
    //NB: closely following UncachedSimulationService to facilitate similar caching/threading in the future
    final var cell = new InMemoryResultsCell();
    try {
      this.agent.schedule(request, cell);
    } catch (final InterruptedException e) {
      //interruption means the result will probably be an Incomplete, but that's ok
    }

    //collect the result (whether success/failure/incomplete) and discard the cell
    final var result = cell.get();
    cell.cancel();
    return result;
  }
}
