package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityModelApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.DynamicActivityModelQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.DynamicStateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model.CumulableEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model.CumulableStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model.SettableEffectEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model.SettableStateApplicator;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.model.RegisterState;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.StateQuery;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.DynamicReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell.setDynamic;

public final class BananaQuerier<T> implements MerlinAdaptation.Querier<T, BananaEvent> {
  private static final DynamicCell<ReactionContext<?, BananaEvent, Activity>> reactionContext = DynamicCell.create();
  private static final DynamicCell<BananaQuerier<?>.InnerQuerier> queryContext = DynamicCell.create();

  public static final ReactionContext<?, BananaEvent, Activity> ctx = new DynamicReactionContext<>(reactionContext::get);
  public static final Function<String, StateQuery<SerializedValue>> query = (name) -> new DynamicStateQuery<>(() -> queryContext.get().getRegisterQuery(name));
  public static final ActivityModelQuerier activityQuery = new DynamicActivityModelQuerier(() -> queryContext.get().getActivityQuery());

  private final ActivityMapper activityMapper;

  private final Set<String> stateNames = new HashSet<>();
  private final Map<String, Query<T, BananaEvent, RegisterState<SerializedValue>>> settables = new HashMap<>();
  private final Map<String, Query<T, BananaEvent, RegisterState<Double>>> cumulables = new HashMap<>();
  private final Query<T, BananaEvent, ActivityModel> activityModel;

  public BananaQuerier(final ActivityMapper activityMapper, final SimulationTimeline<T, BananaEvent> timeline) {
    this.activityMapper = activityMapper;

    this.activityModel = timeline.register(
        new ActivityEffectEvaluator().filterContramap(BananaEvent::asActivity),
        new ActivityModelApplicator());

    for (final var entry : BananaStates.factory.getSettableStates().entrySet()) {
      final var name = entry.getKey();
      final var initialValue = entry.getValue();

      if (this.stateNames.contains(name)) throw new RuntimeException("State \"" + name + "\" already defined");
      this.stateNames.add(name);

      this.settables.put(name, timeline.register(
        new SettableEffectEvaluator(name).filterContramap(BananaEvent::asIndependent),
        new SettableStateApplicator(initialValue)));
    }

    for (final var entry : BananaStates.factory.getCumulableStates().entrySet()) {
      final var name = entry.getKey();
      final var initialValue = entry.getValue();

      if (this.stateNames.contains(name)) throw new RuntimeException("State \"" + name + "\" already defined");
      this.stateNames.add(name);

      this.cumulables.put(name, timeline.register(
        new CumulableEffectEvaluator(name).filterContramap(BananaEvent::asIndependent),
        new CumulableStateApplicator(initialValue)));
    }
  }

  @Override
  public void runActivity(final ReactionContext<T, BananaEvent, Activity> ctx, final String activityId, final Activity activity) {
    setDynamic(queryContext, new InnerQuerier(ctx::now), () ->
        setDynamic(reactionContext, ctx, () -> {
          ctx.emit(BananaEvent.activity(ActivityEvent.startActivity(activityId, this.activityMapper.serializeActivity(activity).get())));
          activity.modelEffects();
          ctx.waitForChildren();
          ctx.emit(BananaEvent.activity(ActivityEvent.endActivity(activityId)));
        }));
  }

  @Override
  public Set<String> states() {
    return Collections.unmodifiableSet(this.stateNames);
  }

  @Override
  public SerializedValue getSerializedStateAt(final String name, final History<T, BananaEvent> history) {
    return this.getRegisterQueryAt(name, history).get();
  }

  public StateQuery<SerializedValue> getRegisterQueryAt(final String name, final History<T, BananaEvent> history) {
    if (this.settables.containsKey(name)) return this.settables.get(name).getAt(history);
    else if (this.cumulables.containsKey(name)) return StateQuery.from(this.cumulables.get(name).getAt(history), SerializedValue::of);
    else throw new RuntimeException("State \"" + name + "\" is not defined");
  }

  @Override
  public List<ConstraintViolation> getConstraintViolationsAt(final History<T, BananaEvent> history) {
    return setDynamic(queryContext, new InnerQuerier(() -> history), () -> {
      final var violations = new ArrayList<ConstraintViolation>();

      for (final var violableConstraint : BananaStates.violableConstraints) {
        final var violationWindows = violableConstraint.getWindows();
        if (violationWindows.isEmpty()) continue;

        violations.add(new ConstraintViolation(violationWindows, violableConstraint));
      }

      return violations;
    });
  }

  private final class InnerQuerier {
    private final Supplier<History<T, BananaEvent>> currentHistory;

    private InnerQuerier(final Supplier<History<T, BananaEvent>> currentHistory) {
      this.currentHistory = currentHistory;
    }

    public StateQuery<SerializedValue> getRegisterQuery(final String name) {
      return BananaQuerier.this.getRegisterQueryAt(name, this.currentHistory.get());
    }

    public ActivityModelQuerier getActivityQuery() {
      return BananaQuerier.this.activityModel.getAt(this.currentHistory.get());
    }
  }
}
