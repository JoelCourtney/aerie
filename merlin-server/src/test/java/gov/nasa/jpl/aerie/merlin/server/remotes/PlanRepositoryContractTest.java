package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class PlanRepositoryContractTest {
  protected PlanRepository planRepository = null;

  protected abstract void resetRepository();

  @Before
  public void resetRepositoryBeforeEachTest() {
    this.resetRepository();
  }

  @Test
  public void testCanStorePlan() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final NewPlan newPlan = new NewPlan();
    newPlan.name = "new-plan";

    final String planId = this.planRepository.createPlan(newPlan);

    // THEN
    final Plan plan = this.planRepository.getPlan(planId);
    assertThat(plan.name).isEqualTo("new-plan");
  }

  @Test
  public void testUnsavedPlanTransactionHasNoEffect() throws NoSuchPlanException {
    // GIVEN
    final NewPlan newPlan = new NewPlan();
    newPlan.name = "before";
    final String planId = this.planRepository.createPlan(newPlan);

    // WHEN
    this.planRepository
        .updatePlan(planId)
        .setName("after");
        // no .commit()

    // THEN
    final Plan plan = this.planRepository.getPlan(planId);
    assertThat(plan.name).isEqualTo("before");
  }

  @Test
  public void testCreatePlanWithActivity() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final ActivityInstance activity = new ActivityInstance();
    activity.type = "abc";
    activity.parameters = Map.of("abc", SerializedValue.of(1));

    final NewPlan newPlan = new NewPlan();
    newPlan.name = "new-plan";
    newPlan.activityInstances = List.of();

    final String planId = this.planRepository.createPlan(newPlan);
    this.planRepository.createActivity(planId, activity);

    // THEN
    final Plan plan = this.planRepository.getPlan(planId);
    assertThat(plan.name).isEqualTo("new-plan");
    assertThat(plan.activityInstances.values()).containsExactly(activity);
  }

  @Test
  public void testCreatePlanWithNullActivitiesList() throws NoSuchPlanException {
    // GIVEN

    // WHEN
    final String planId = this.planRepository.createPlan(new NewPlan());

    // THEN
    assertThat(this.planRepository.getPlan(planId).activityInstances).isNotNull().isEmpty();
  }

  @Test
  public void testCanDeletePlan() throws NoSuchPlanException {
    // GIVEN
    this.planRepository.createPlan(new NewPlan());
    final String planId = this.planRepository.createPlan(new NewPlan());
    this.planRepository.createPlan(new NewPlan());
    assertThat(this.planRepository.getAllPlans()).size().isEqualTo(3);

    // WHEN
    this.planRepository.deletePlan(planId);

    // THEN
    assertThat(this.planRepository.getAllPlans()).size().isEqualTo(2);
  }
}
