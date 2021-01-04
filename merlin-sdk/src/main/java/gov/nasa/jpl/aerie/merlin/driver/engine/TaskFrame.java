package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Deque;

/*package-local*/
final class TaskFrame<$Timeline> {
  public History<$Timeline> tip;
  public Deque<Triple<History<$Timeline>, String, Task<$Timeline>>> branches;

  public TaskFrame(
      final History<$Timeline> tip,
      final Deque<Triple<History<$Timeline>, String, Task<$Timeline>>> branches)
  {
    this.tip = tip;
    this.branches = branches;
  }

  public boolean hasBranches() {
    return !this.branches.isEmpty();
  }

  public Triple<History<$Timeline>, String, Task<$Timeline>> popBranch() {
    return this.branches.pop();
  }

  public void pushBranch(final History<$Timeline> startTime, final String id, final Task<$Timeline> task) {
    this.branches.push(Triple.of(startTime, id, task));
  }
}
