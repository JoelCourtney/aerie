package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.scheduler.SimulationUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IncrementalSimulationTest {

  IncrementalSimulationDriver<?> incrementalSimulationDriver;
  Duration endOfLastAct;
  private Schedule schedule;

  @BeforeEach
  public void init() throws InstantiationException {
    final var acts = getActivities();
    final var scheduleBuilder = new HashMap<ActivityInstanceId, Schedule.Directive>();
    for (final var act : acts) {
      scheduleBuilder.put(act.id, new Schedule.Directive(new StartTime.OffsetFromPlanStart(act.start()), act.activity));
    }
    schedule = new Schedule(scheduleBuilder);
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    incrementalSimulationDriver = new IncrementalSimulationDriver<>(fooMissionModel);
    for (final var act : acts) {
      final var start = System.nanoTime();
      incrementalSimulationDriver.simulateActivity(schedule, act.id);
    }
  }
  @Test
  public void simulationResultsTest(){
    final var now = Instant.now();
    //ensures that simulation results are generated until the end of the last act;
    var simResults = incrementalSimulationDriver.getSimulationResults(schedule, now);
    assert(simResults.realProfiles.get("/utcClock").getRight().get(0).extent().isEqualTo(endOfLastAct));
    /* ensures that when current simulation results cover more than the asked period and that nothing has happened
    between two requests, the same results are returned */
    var simResults2 = incrementalSimulationDriver.getSimulationResultsUpTo(schedule, now, Duration.of(7, SECONDS));
    assertEquals(simResults, simResults2);
  }

  @Test
  public void simulationResultsTest2(){
    /* ensures that when the passed start epoch is not equal to the one used for previously computed results, the results are re-computed */
    var simResults = incrementalSimulationDriver.getSimulationResults(schedule, Instant.now());
    assert(simResults.realProfiles.get("/utcClock").getRight().get(0).extent().isEqualTo(endOfLastAct));
    var simResults2 = incrementalSimulationDriver.getSimulationResultsUpTo(schedule, Instant.now(), Duration.of(7, SECONDS));
    assertNotEquals(simResults, simResults2);
  }

  @Test
  public void simulationResultsTest3(){
     /* ensures that when current simulation results cover less than the asked period and that nothing has happened
    between two requests, the results are re-computed */
    final var now = Instant.now();
    var simResults2 = incrementalSimulationDriver.getSimulationResultsUpTo(schedule, now, Duration.of(7, SECONDS));
    var simResults = incrementalSimulationDriver.getSimulationResults(schedule, now);
    assert(simResults.realProfiles.get("/utcClock").getRight().get(0).extent().isEqualTo(endOfLastAct));
    assertNotEquals(simResults, simResults2);
  }

  @Test
  public void durationTest(){
    final var acts = getActivities();
    var act1Dur = incrementalSimulationDriver.getActivityDuration(acts.get(0).id());
    var act2Dur = incrementalSimulationDriver.getActivityDuration(acts.get(1).id());
    assertTrue(act1Dur.isPresent() && act2Dur.isPresent());
    assertTrue(act1Dur.get().isEqualTo(Duration.of(2, SECONDS)));
    assertTrue(act2Dur.get().isEqualTo(Duration.of(2, SECONDS)));
  }

  @Test
  public void testThreadsReleased() throws InstantiationException {
    final var activity = new TestSimulatedActivity(
        Duration.of(0, SECONDS),
        new SerializedActivity("BasicActivity", Map.of()),
        new ActivityInstanceId(1));
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    incrementalSimulationDriver = new IncrementalSimulationDriver<>(fooMissionModel);
    final var executor = unsafeGetExecutor(incrementalSimulationDriver);
    for (var i = 0; i < 20000; i++) {
      incrementalSimulationDriver.initSimulation();
      incrementalSimulationDriver.simulateActivity(schedule, activity.id);
      assertTrue(executor.getActiveCount() < 100, "Threads are not being cleaned up properly - this test shouldn't need more than 2 threads, but it used at least 100");
    }
  }

  private static ThreadPoolExecutor unsafeGetExecutor(final IncrementalSimulationDriver<?> driver) {
    try {
      final var engineField = IncrementalSimulationDriver.class.getDeclaredField("engine");
      engineField.setAccessible(true);

      final var executorField = SimulationEngine.class.getDeclaredField("executor");
      executorField.setAccessible(true);

      return (ThreadPoolExecutor) executorField.get(engineField.get(driver));
    } catch (final ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  private ArrayList<TestSimulatedActivity> getActivities(){
    final var acts = new ArrayList<TestSimulatedActivity>();
    var act1 = new TestSimulatedActivity(
        Duration.of(0, SECONDS),
        new SerializedActivity("BasicActivity", Map.of()),
        new ActivityInstanceId(1));
    acts.add(act1);
    var act2 = new TestSimulatedActivity(
        Duration.of(14, SECONDS),
        new SerializedActivity("BasicActivity", Map.of()),
        new ActivityInstanceId(2));
    acts.add(act2);

    endOfLastAct = Duration.of(16,SECONDS);
    return acts;
  }


  record TestSimulatedActivity(Duration start, SerializedActivity activity, ActivityInstanceId id){}


}
