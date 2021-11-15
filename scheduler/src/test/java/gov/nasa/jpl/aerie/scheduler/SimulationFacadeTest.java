package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.google.common.truth.Truth.assertThat;

public class SimulationFacadeTest {

  MissionModel<?> missionModel;
  MissionModelWrapper wrappedMissionModel;
  SimulationFacade facade;

  //concrete named time points used to setup tests and validate expectations
  private static final Time t0 = Time.fromMilli(0);
  private static final Time t1 = Time.fromMilli(1000);
  private static final Time t1_5 = Time.fromMilli(1500);
  private static final Time t2 = Time.fromMilli(2000);
  private static final Time tEnd = Time.fromMilli(5000);

  //hard-coded range of scheduling/simulation operations
  private static final Range<Time> horizon = new Range<>(t0, tEnd);
  private static final TimeWindows entireHorizon = TimeWindows.of(horizon);

  //scheduler-side mirrors of test activity types used
  //TODO: should eventually mirror these from the mission model itself
  private static final ActivityType actTypeBite = new ActivityType("BiteBanana");
  private static final ActivityType actTypePeel = new ActivityType("PeelBanana");

  /** fetch reference to the fruit resource in the mission model **/
  private SimResource<Double> getFruitRes() {
    return facade.getDoubleResource("/fruit");
  }

  /** fetch reference to the conflicted marker on the flag resource in the mission model **/
  private SimResource<Boolean> getFlagConflictedRes() {
    return facade.getBooleanResource("/flag/conflicted");
  }

  /** fetch reference to the flag resource in the mission model **/
  private SimResource<String> getFlagRes() {
    return facade.getStringResource("/flag");
  }

  /** fetch reference to the plant resource in the mission model **/
  private SimResource<Integer> getPlantRes() {
    return facade.getIntResource("/plant");
  }

  @BeforeEach
  public void setUp() {
    missionModel = SimulationUtility.getMissionModel();

    wrappedMissionModel = new MissionModelWrapper(missionModel, horizon);
    wrappedMissionModel.add(actTypeBite);
    wrappedMissionModel.add(actTypePeel);

    facade = new SimulationFacade(horizon, missionModel);
  }

  @AfterEach
  public void tearDown() {
    missionModel = null;
    wrappedMissionModel = null;
    facade = null;
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory(wrappedMissionModel);
  }

  @Test
  public void constraintEvalWithoutSimulationThrowsIAE() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), 2.9);
    final var plan = makeEmptyPlan();
    assertThrows(IllegalArgumentException.class, () -> constraint.findWindows(plan, entireHorizon));
  }

  @Test
  public void doubleConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), 2.9);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void boolConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagConflictedRes(), false);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void stringConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagRes(), "A");
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void intConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getPlantRes(), 200);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  /**
   * constructs a simple test plan with one peel and bite
   *
   * expected activity/resource histories:
   * <pre>
   * time: |t0--------|t1--------|t2-------->
   * acts:            |peel(fS)  |bite(0.1)
   * peel: |4.0-------|3.0------------------>
   * fruit:|4.0-------|3.0-------|2.9------->
   * </pre>
   **/
  private PlanInMemory makeTestPlanP0B1() {
    final var plan = makeEmptyPlan();

    var act1 = new ActivityInstance("PeelBanana1", actTypePeel, t1);
    act1.setParameters(Map.of("peelDirection", "fromStem"));
    plan.add(act1);

    var act2 = new ActivityInstance("BiteBanana1", actTypeBite, t2);
    act2.setParameters(Map.of("biteSize", 0.1));
    plan.add(act2);

    return plan;
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlanMidpoint() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().getValueAtTime(t1_5);
    assertThat(actual).isEqualTo(3.0);
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().getValueAtTime(t2);
    assertThat(actual).isEqualTo(2.9);
  }

  @Test
  public void whenValueAboveDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueAbove(2.9, entireHorizon);
    var expected = TimeWindows.of(t0, t2);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBelowDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueBelow(3.0, entireHorizon);
    var expected = TimeWindows.of(t2, tEnd);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBetweenDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueBetween(3.00, 3.99, entireHorizon);
    var expected = TimeWindows.of(t1, t2);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueEqualDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueEqual(3.00, entireHorizon);
    var expected = TimeWindows.of(t1, t2);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueNotEqualDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueNotEqual(3.00, entireHorizon);
    var expected = TimeWindows.of(List.of(Range.of(t0, t1), Range.of(t2, tEnd)));
    assertThat(actual).isEqualTo(expected);
  }

}
