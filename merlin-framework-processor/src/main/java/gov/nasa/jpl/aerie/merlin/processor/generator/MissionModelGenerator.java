package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor;
import gov.nasa.jpl.aerie.merlin.processor.Resolver;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ConfigurationTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.SpecificationTypeRecord;
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/** Auto-generates Java source files from mission model metamodels. */
public record MissionModelGenerator(Elements elementUtils, Types typeUtils, Messager messager) {

  /** Generate `GeneratedMerlinPlugin` class. */
  public JavaFile generateMerlinPlugin(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getPluginName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(MerlinPlugin.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("getFactory")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(missionModel.getFactoryName())
                    .addStatement(
                        "return new $T()",
                        missionModel.getFactoryName())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `GeneratedMissionModelFactory` class. */
  public Optional<JavaFile> generateMissionModelConfigurationMapper(final MissionModelRecord missionModel, final ConfigurationTypeRecord configType) {
    return generateCommonMapperMethods(missionModel, configType).map(typeSpec -> JavaFile
        .builder(configType.mapper().name.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build());
  }

  /** Generate `GeneratedMissionModelFactory` class. */
  public JavaFile generateMissionModelFactory(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getFactoryName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(MissionModelFactory.class),
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel))))
            .addMethod(
                MethodSpec
                    .methodBuilder("getConfigurationType")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Optional.class),
                        ClassName.get(ConfigurationType.class)))
                    .addStatement("return $T.activityTypes", missionModel.getTypesName())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getTaskSpecTypes")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                ClassName.get(missionModel.topLevelModel)),
                            WildcardTypeName.subtypeOf(Object.class))))
                    .addStatement("return $T.activityTypes", missionModel.getTypesName())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(
                        TypeName.get(gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class),
                        "configuration",
                        Modifier.FINAL)
                    .addParameter(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer.class),
                        "builder",
                        Modifier.FINAL)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                            ClassName.get(missionModel.topLevelModel)))
                    .addStatement(
                        "final var $L = new $T($L)",
                        "registrar",
                        gov.nasa.jpl.aerie.merlin.framework.Registrar.class,
                        "builder")
                    .addStatement(
                        "final var $L = $T.makeExecutorService()",
                        "executor",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class)
                    .addCode("\n")
                    .addCode(
                        missionModel.modelConfigurationType
                            .map(configElem -> CodeBlock // if configuration is provided
                                .builder()
                                .addStatement(
                                    "final var $L = $L",
                                    "configMapper",
                                    generateConfigurationMapperBlock(missionModel, configElem.declaration()))
                                .addStatement(
                                    "final var $L = $L.deserializeValue($L).getSuccessOrThrow()",
                                    "deserializedConfig",
                                    "configMapper",
                                    "configuration")
                                .addStatement(
                                    "final var $L = $T.initializing($L, $L, () -> new $T($L, $L))",
                                    "model",
                                    gov.nasa.jpl.aerie.merlin.framework.InitializationContext.class,
                                    "executor",
                                    "builder",
                                    ClassName.get(missionModel.topLevelModel),
                                    "registrar",
                                    "deserializedConfig")
                                .build())
                            .orElseGet(() -> CodeBlock // if configuration is not provided
                                .builder()
                                .addStatement(
                                    "final var $L = $T.initializing($L, $L, () -> new $T($L))",
                                    "model",
                                    gov.nasa.jpl.aerie.merlin.framework.InitializationContext.class,
                                    "executor",
                                    "builder",
                                    ClassName.get(missionModel.topLevelModel),
                                    "registrar")
                                .build()))
                    .addCode("\n")
                    .addStatement(
                        "return new $T<$T>($L, $L)",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class,
                        ClassName.get(missionModel.topLevelModel),
                        "model",
                        "executor")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getParameters")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(List.class, Parameter.class))
                    .addCode(
                        missionModel.modelConfigurationType
                            .map(configElem -> CodeBlock // if configuration is provided
                                .builder()
                                .addStatement("return $L.getValueSchema().asStruct().map(parameterMap ->\n"+
                                              "parameterMap.keySet().stream().map(name -> new $T(name, parameterMap.get(name))).toList())\n"+
                                              ".orElse($T.of())",
                                    generateConfigurationMapperBlock(missionModel, configElem.declaration()),
                                    Parameter.class,
                                    List.class)
                                .build())
                            .orElse(CodeBlock
                                .builder()
                                .addStatement("return $T.of()", List.class)
                                .build()))
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `ActivityActions` class. */
  public JavaFile generateActivityActions(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getActivityActionsName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the mission model package determines where to put this class.
            .addOriginatingElement(missionModel.$package)
            // TODO: List found task spec types as dependencies of this generated file.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .superclass(gov.nasa.jpl.aerie.merlin.framework.ModelActions.class)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addMethods(
                missionModel.activityTypes
                    .stream()
                    .flatMap(entry -> List
                        .of(
                            MethodSpec
                                .methodBuilder("spawn")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper().name)
                                .addStatement(
                                    "return $T.spawn($L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "duration")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper().name)
                                .addStatement(
                                    "return $T.defer($L, $L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "duration",
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            TypeName.LONG,
                                            "quantity")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "unit")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "return defer($L.times($L), $L)",
                                    "unit",
                                    "quantity",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("call")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(TypeName.VOID)
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "$T.waitFor(spawn($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "activity")
                                .build())
                        .stream())
                    .collect(Collectors.toList()))
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `ActivityTypes` class. */
  public JavaFile generateActivityTypes(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getTypesName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(List.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "activityTypeList",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$T.of($>$>\n$L$<$<)",
                        List.class,
                        missionModel.activityTypes
                            .stream()
                            .map(activityType -> CodeBlock.builder().add("new $T()", activityType.mapper().name))
                            .reduce((x, y) -> x.add(",\n$L", y.build()))
                            .orElse(CodeBlock.builder())
                            .build())
                    .build())
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "activityTypes",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$L.stream().collect($T.toMap($$ -> $$.getName(), $$ -> $$));",
                        "activityTypeList",
                        java.util.stream.Collectors.class)
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate common `${activity_name}Mapper` methods. */
  public Optional<TypeSpec> generateCommonMapperMethods(final MissionModelRecord missionModel, final SpecificationTypeRecord specType) {
    final var maybeMapperBlocks = generateParameterMapperBlocks(missionModel, specType);
    if (maybeMapperBlocks.isEmpty()) return Optional.empty();

    final var mapperBlocks = maybeMapperBlocks.get();
    final var mapperMethodMaker = MapperMethodMaker.make(specType.defaultsStyle());

    // TODO currently only 2 permitted classes (activity and config. type records),
    //  this should be changed to a switch expression once sealed class pattern-matching switch expressions exist
    final TypeName superInterface;
    if (specType instanceof ActivityTypeRecord) {
      superInterface = ParameterizedTypeName.get(
          ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
          ParameterizedTypeName.get(
              ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
              ClassName.get(missionModel.topLevelModel)),
          ClassName.get(specType.declaration()));
    } else { // is instanceof ConfigurationTypeRecord
      superInterface = ParameterizedTypeName.get(
          ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType.class),
          ClassName.get(specType.declaration()));
    }

    return Optional.of(TypeSpec
        .classBuilder(specType.mapper().name)
        // The location of the missionModel package determines where to put this class.
        .addOriginatingElement(missionModel.$package)
        // The fields and methods of the activity determines the overall behavior of this class.
        .addOriginatingElement(specType.declaration())
        // TODO: Add an originating element for each of the mapper rulesets associated with the mission model.
        .addAnnotation(
            AnnotationSpec
                .builder(javax.annotation.processing.Generated.class)
                .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                .build())
        .addSuperinterface(superInterface)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addFields(
            specType.parameters()
                .stream()
                .map(parameter -> FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ValueMapper.class),
                            TypeName.get(parameter.type).box()),
                        "mapper_" + parameter.name)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build())
                .collect(Collectors.toList()))
        .addMethod(
            MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                /* Suppress unchecked warnings because the resolver has to
                    put some big casting in for Class parameters
                 */
                .addAnnotation(
                    AnnotationSpec
                        .builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addCode(
                    specType.parameters()
                        .stream()
                        .map(parameter -> CodeBlock
                            .builder()
                            .addStatement(
                                "this.mapper_$L =\n$L",
                                parameter.name,
                                mapperBlocks.get(parameter.name)))
                        .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                        .build())
                .build())
        .addMethod(
            MethodSpec
                .methodBuilder("getName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $S", specType.name())
                .build())
        .addMethod(mapperMethodMaker.makeGetRequiredParametersMethod(specType))
        .addMethod(mapperMethodMaker.makeGetParametersMethod(specType))
        .addMethod(mapperMethodMaker.makeGetArgumentsMethod(specType))
        .addMethod(mapperMethodMaker.makeInstantiateMethod(specType))
        .addMethod(mapperMethodMaker.makeGetValidationFailures(specType))
        .build());
  }

  /** Generate `${activity_name}Mapper` class. */
  public Optional<JavaFile> generateActivityMapper(final MissionModelRecord missionModel, final ActivityTypeRecord activityType) {
    return generateCommonMapperMethods(missionModel, activityType).map(typeSpec -> typeSpec.toBuilder()
        .addMethod(
            MethodSpec
                .methodBuilder("createTask")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.Task.class))
                .addParameter(
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel)),
                    "model",
                    Modifier.FINAL)
                .addParameter(
                    TypeName.get(activityType.declaration().asType()),
                    "activity",
                    Modifier.FINAL)
                .addCode(
                    activityType.effectModel()
                        .map(effectModel -> switch (effectModel.getRight()) {
                          case Threaded -> CodeBlock
                              .builder()
                              .addStatement(
                                  "return $T.threaded(() -> $L.$L($L.model())).create($L.executor())",
                                  gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                  "activity",
                                  effectModel.getLeft(),
                                  "model",
                                  "model")
                              .build();

                          case Replaying -> CodeBlock
                              .builder()
                              .addStatement(
                                  "return $T.replaying(() -> $L.$L($L.model())).create($L.executor())",
                                  gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                  "activity",
                                  effectModel.getLeft(),
                                  "model",
                                  "model")
                              .build();
                        })
                        .orElseGet(() -> CodeBlock
                            .builder()
                            .addStatement(
                                "return new $T($$ -> {})",
                                gov.nasa.jpl.aerie.merlin.framework.OneShotTask.class)
                            .build()))
                .build())
        .build())
        .map(typeSpec -> JavaFile
            .builder(activityType.mapper().name.packageName(), typeSpec)
            .skipJavaLangImports(true)
            .build());
  }

  private Optional<Map<String, CodeBlock>> generateParameterMapperBlocks(final MissionModelRecord missionModel, final SpecificationTypeRecord specType)
  {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules);
    var failed = false;
    final var mapperBlocks = new HashMap<String, CodeBlock>();

    for (final var parameter : specType.parameters()) {
      final var mapperBlock = resolver.instantiateNullableMapperFor(parameter.type);
      if (mapperBlock.isPresent()) {
        mapperBlocks.put(parameter.name, mapperBlock.get());
      } else {
        failed = true;
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to generate value mapper for parameter",
            parameter.element);
      }
    }

    return failed ? Optional.empty() : Optional.of(mapperBlocks);
  }

  private CodeBlock generateConfigurationMapperBlock(final MissionModelRecord missionModel, final TypeElement typeElem) {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules);
    final var mapperBlock = resolver.instantiateMapperFor(typeElem.asType());
    if (mapperBlock.isEmpty()) {
      messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate value mapper for configuration", typeElem);
    }
    return mapperBlock.get();
  }
}
