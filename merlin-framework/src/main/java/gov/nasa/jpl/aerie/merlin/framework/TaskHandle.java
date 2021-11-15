package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

public interface TaskHandle {
  Scheduler yield(TaskStatus status);
}
