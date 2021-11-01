package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AdaptationRecord {
  public final PackageElement $package;
  public final TypeElement topLevelModel;
  public final List<TypeRule> typeRules;
  public final List<ActivityTypeRecord> activityTypes;
  public final Optional<TypeElement> modelConfiguration;
  public final ActivityType.Executor defaultExecutor;

  public AdaptationRecord(
      final PackageElement $package,
      final TypeElement topLevelModel,
      final Optional<TypeElement> modelConfiguration,
      final ActivityType.Executor defaultExecutor,
      final List<TypeRule> typeRules,
      final List<ActivityTypeRecord> activityTypes)
  {
    this.$package = Objects.requireNonNull($package);
    this.topLevelModel = Objects.requireNonNull(topLevelModel);
    this.modelConfiguration = Objects.requireNonNull(modelConfiguration);
    this.defaultExecutor = Objects.requireNonNull(defaultExecutor);
    this.typeRules = Objects.requireNonNull(typeRules);
    this.activityTypes = Objects.requireNonNull(activityTypes);
  }

  public ClassName getPluginName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedMerlinPlugin");
  }

  public ClassName getFactoryName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "GeneratedAdaptationFactory");
  }

  public ClassName getActivityActionsName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityActions");
  }

  public ClassName getTypesName() {
    return ClassName.get(this.$package.getQualifiedName() + ".generated", "ActivityTypes");
  }
}
