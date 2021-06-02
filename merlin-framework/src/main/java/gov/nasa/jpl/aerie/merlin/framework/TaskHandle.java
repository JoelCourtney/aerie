package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;

public interface TaskHandle<$Timeline> {
  Scheduler<$Timeline> yield(TaskStatus<$Timeline> status);
}
