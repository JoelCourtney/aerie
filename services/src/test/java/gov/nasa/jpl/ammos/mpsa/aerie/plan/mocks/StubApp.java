package gov.nasa.jpl.ammos.mpsa.aerie.plan.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.Breadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers.App;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.SimulationResults;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class StubApp implements App {
  public static final String EXISTENT_PLAN_ID = "abc";
  public static final String NONEXISTENT_PLAN_ID = "def";
  public static final Plan EXISTENT_PLAN;
  public static final NewPlan VALID_NEW_PLAN;
  public static final NewPlan INVALID_NEW_PLAN;
  public static final Plan VALID_PATCH;
  public static final Plan INVALID_PATCH;

  public static final String EXISTENT_ACTIVITY_ID = "activity";
  public static final String NONEXISTENT_ACTIVITY_ID = "no-activity";
  public static final ActivityInstance EXISTENT_ACTIVITY;
  public static final ActivityInstance VALID_ACTIVITY;
  public static final ActivityInstance INVALID_ACTIVITY;

  public static final List<Pair<List<Breadcrumb>, String>> VALIDATION_ERRORS = List.of(
      Pair.of(List.of(Breadcrumb.of("breadcrumb"), Breadcrumb.of(0)), "an error")
  );

  static {
    EXISTENT_ACTIVITY = new ActivityInstance();
    EXISTENT_ACTIVITY.type = "existent activity";
    EXISTENT_ACTIVITY.startTimestamp = "start timestamp";
    EXISTENT_ACTIVITY.parameters = Map.of(
        "abc", SerializedParameter.of("test-param")
    );

    VALID_ACTIVITY = new ActivityInstance();
    VALID_ACTIVITY.type = "valid activity";
    VALID_ACTIVITY.startTimestamp = "start timestamp";
    VALID_ACTIVITY.parameters = Map.of();

    INVALID_ACTIVITY = new  ActivityInstance();
    INVALID_ACTIVITY.type = "invalid activity";

    VALID_NEW_PLAN = new NewPlan();
    VALID_NEW_PLAN.name = "valid";
    VALID_NEW_PLAN.adaptationId = "adaptation id";
    VALID_NEW_PLAN.startTimestamp = "start timestamp";
    VALID_NEW_PLAN.endTimestamp = "end timestamp";

    INVALID_NEW_PLAN = new NewPlan();
    INVALID_NEW_PLAN.name = "invalid";

    EXISTENT_PLAN = new Plan();
    EXISTENT_PLAN.name = "existent";
    EXISTENT_PLAN.activityInstances = Map.of(EXISTENT_ACTIVITY_ID, EXISTENT_ACTIVITY);

    VALID_PATCH = new Plan();
    VALID_PATCH.name = "valid patch";

    INVALID_PATCH = new Plan();
    INVALID_PATCH.name = "invalid patch";
  }


  public Stream<Pair<String, Plan>> getPlans() {
    return Stream.of(Pair.of(EXISTENT_PLAN_ID, EXISTENT_PLAN));
  }

  public Plan getPlanById(final String id) throws NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    }

    return EXISTENT_PLAN;
  }

  public String addPlan(final NewPlan plan) throws ValidationException {
    if (plan.equals(INVALID_NEW_PLAN)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }

    return EXISTENT_PLAN_ID;
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    }
  }

  @Override
  public void updatePlan(final String id, final Plan patch) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    } else if (Objects.equals(patch, INVALID_PATCH)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(id, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(id);
    } else if (plan.equals(INVALID_NEW_PLAN)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public ActivityInstance getActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    }

    return EXISTENT_ACTIVITY;
  }

  @Override
  public List<String> addActivityInstancesToPlan(final String planId, final List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    final List<String> activityIds = new ArrayList<>();
    for (final ActivityInstance activityInstance : activityInstances) {
      if (!Objects.equals(activityInstance, VALID_ACTIVITY)) {
        throw new ValidationException(VALIDATION_ERRORS);
      }

      activityIds.add(EXISTENT_ACTIVITY_ID);
    }

    return activityIds;
  }

  @Override
  public void removeActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    }
  }

  @Override
  public void updateActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(patch, INVALID_ACTIVITY)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public void replaceActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance activityInstance) throws NoSuchPlanException, ValidationException, NoSuchActivityInstanceException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    } else if (!Objects.equals(activityInstanceId, EXISTENT_ACTIVITY_ID)) {
      throw new NoSuchActivityInstanceException(planId, activityInstanceId);
    } else if (Objects.equals(activityInstance, INVALID_ACTIVITY)) {
      throw new ValidationException(VALIDATION_ERRORS);
    }
  }

  @Override
  public SimulationResults getSimulationResultsForPlan(final String planId, final long samplingPeriod) throws NoSuchPlanException {
    if (!Objects.equals(planId, EXISTENT_PLAN_ID)) {
      throw new NoSuchPlanException(planId);
    }

    return new SimulationResults(Instant.EPOCH, List.of(), Map.of());
  }
}
