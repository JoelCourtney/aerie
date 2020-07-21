package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class PlanValidator {
  private final PlanRepository planRepository;
  private final AdaptationService adaptationService;

  private final BreadcrumbCursor breadcrumbCursor = new BreadcrumbCursor();
  private final List<Pair<List<Breadcrumb>, String>> messages = new ArrayList<>();

  public PlanValidator(final PlanRepository planRepository, final AdaptationService adaptationService) {
    this.planRepository = planRepository;
    this.adaptationService = adaptationService;
  }

  public void validateActivity(final String adaptationId, final ActivityInstance activityInstance) {
    final List<String> validationFailures;
    try {
      validationFailures = this.adaptationService
          .areActivityParametersValid(adaptationId, new SerializedActivity(activityInstance.type, activityInstance.parameters));
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      throw new Error("Unexpectedly nonexistent adaptation, when this should have been validated earlier.", ex);
    }

    for (final var failure : validationFailures) addError(failure);

    if (activityInstance.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));
    if (activityInstance.type == null) with("type", () -> addError("must be non-null"));
  }

  public void validateActivityList(final String adaptationId, final Collection<ActivityInstance> activityInstances) {
    int index = 0;
    for (final ActivityInstance activityInstance : activityInstances) {
      with(index++, () -> validateActivity(adaptationId, activityInstance));
    }
  }

  public void validateActivityMap(final String adaptationId, final Map<String, ActivityInstance> activityInstances) {
    for (final var entry : activityInstances.entrySet()) {
      final String activityId = entry.getKey();
      final ActivityInstance activityInstance = entry.getValue();

      with(activityId, () -> validateActivity(adaptationId, activityInstance));
    }
  }

  public void validateNewPlan(final NewPlan plan) {
    if (plan.name == null) with("name", () -> addError("must be non-null"));
    if (plan.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));
    if (plan.endTimestamp == null) with("endTimestamp", () -> addError("must be non-null"));
    if (plan.adaptationId == null) {
      with("adaptationId", () -> addError("must be non-null"));
    } else if (!this.adaptationService.isMissionModelDefined(plan.adaptationId)) {
      with("adaptationId", () -> addError("is not a defined mission model"));
    } else if (plan.activityInstances != null) {
      with("activityInstances", () -> validateActivityList(plan.adaptationId, plan.activityInstances));
    }
  }

  public void validatePlanPatch(final String adaptationId, final String planId, final Plan patch) throws NoSuchPlanException {
    if (patch.adaptationId != null) with("adaptationId", () -> addError("cannot be changed after creation"));

    if (patch.activityInstances != null) {
      final Set<String> validActivityIds = this.planRepository
          .getAllActivitiesInPlan(planId)
          .map(Pair::getKey)
          .collect(Collectors.toSet());

      with("activityInstances", () -> {
        for (final var entry : patch.activityInstances.entrySet()) {
          final var activityId = entry.getKey();
          final var activityInstance = entry.getValue();
          if (!validActivityIds.contains(activityId)) {
            with(activityId, () -> addError("no activity with id in plan"));
          }

          if (activityInstance != null)
            with(activityId, () -> validateActivity(adaptationId, activityInstance));
        }
      });
    }
  }

  public List<Pair<List<Breadcrumb>, String>> getMessages() {
    return List.copyOf(this.messages);
  }

  private void addError(final String message) {
    this.messages.add(Pair.of(unmodifiableList(this.breadcrumbCursor.getPath()), message));
  }

  private void with(final int index, final Runnable block) {
    this.breadcrumbCursor.descend(index);
    try {
      block.run();
    } finally {
      this.breadcrumbCursor.ascend();
    }
  }

  private void with(final String index, final Runnable block) {
    this.breadcrumbCursor.descend(index);
    try {
      block.run();
    } finally {
      this.breadcrumbCursor.ascend();
    }
  }
}
