package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.apache.commons.collections4.BidiMap;

import java.util.Collection;
import java.util.Optional;

public record SimulationData(
    SimulationResults driverResults,
    gov.nasa.jpl.aerie.constraints.model.SimulationResults constraintsResults,
    Optional<BidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId>> mapSchedulingIdsToActivityIds
){}
