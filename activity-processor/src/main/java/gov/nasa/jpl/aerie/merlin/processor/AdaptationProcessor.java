package gov.nasa.jpl.aerie.merlin.processor;

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
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityMapperRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityParameterRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityValidationRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.AdaptationRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdaptationProcessor implements Processor {
  private Messager messager = null;
  private Filer filer = null;
  private Elements elementUtils = null;
  private Types typeUtils = null;

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of();
  }

  // Elements marked by these annotations will be treated as processing roots.
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Adaptation.class.getCanonicalName(),
        ActivityType.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_11;
  }

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    this.messager = processingEnv.getMessager();
    this.filer = processingEnv.getFiler();
    this.elementUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  private final Set<Element> foundActivityTypes = new HashSet<>();
  private final Set<Element> ownedActivityTypes = new HashSet<>();

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    ///Accumulate any information added in this round.
    this.foundActivityTypes.addAll(roundEnv.getElementsAnnotatedWith(ActivityType.class));

    for (final var element : roundEnv.getElementsAnnotatedWith(Adaptation.class)) {
      final var packageElement = (PackageElement) element;

      try {
        final var adaptationRecord = parseAdaptation(packageElement);

        final var generatedFiles = new ArrayList<JavaFile>();
        generatedFiles.add(generateAdaptationFactory(adaptationRecord));
        generatedFiles.add(generateActivityActions(adaptationRecord));
        for (final var activityRecord : adaptationRecord.activityTypes) {
          this.ownedActivityTypes.add(activityRecord.declaration);
          if (!activityRecord.mapper.isCustom) {
            generateActivityMapper(adaptationRecord, activityRecord).ifPresent(generatedFiles::add);
          }
        }

        for (final var generatedFile : generatedFiles) {
          this.messager.printMessage(
              Diagnostic.Kind.NOTE,
              "Generating " + generatedFile.packageName + "." + generatedFile.typeSpec.name);

          generatedFile.writeTo(this.filer);
        }
      } catch (final InvalidAdaptationException ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            ( ex.getMessage()
              + "\n"
              + Arrays
                  .stream(trace)
                  .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                  .map(Object::toString)
                  .collect(Collectors.joining("\n")) ),
            ex.element,
            ex.annotation,
            ex.attribute);
      } catch (final Throwable ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            ( ex.getMessage()
              + "\n"
              + Arrays
                  .stream(trace)
                  .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                  .map(Object::toString)
                  .collect(Collectors.joining("\n")) ));
      }
    }

    if (roundEnv.processingOver()) {
      for (final var foundActivityType : this.foundActivityTypes) {
        if (this.ownedActivityTypes.contains(foundActivityType)) continue;

        this.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "@ActivityType-annotated class is not referenced by any @WithActivity",
            foundActivityType);
      }
    }

    ///Allow other annotation processors to process the framework annotations.
    return false;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(
      final Element element,
      final AnnotationMirror annotation,
      final ExecutableElement member,
      final String userText)
  {
    return Collections::emptyIterator;
  }

  private AdaptationRecord
  parseAdaptation(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var topLevelModel = this.getAdaptationModel(adaptationElement);
    final var activityTypes = new ArrayList<ActivityTypeRecord>();
    final var typeRules = new ArrayList<TypeRule>();

    for (final var factory : this.getAdaptationMapperClasses(adaptationElement)) {
      typeRules.addAll(this.parseValueMappers(factory));
    }

    for (final var activityTypeElement : this.getAdaptationActivityTypes(adaptationElement)) {
      activityTypes.add(this.parseActivityType(adaptationElement, activityTypeElement));
    }

    return new AdaptationRecord(adaptationElement, topLevelModel, typeRules, activityTypes);
  }

  private List<TypeRule>
  parseValueMappers(final TypeElement factory)
  throws InvalidAdaptationException {
    final var valueMappers = new ArrayList<TypeRule>();

    for (final var element : factory.getEnclosedElements()) {
      if (element.getKind().equals(ElementKind.METHOD)) {
        valueMappers.add(this.parseValueMapperMethod((ExecutableElement)element, ClassName.get(factory)));
      }
    }

    return valueMappers;
  }

  private TypeRule
  parseValueMapperMethod(final ExecutableElement element, final ClassName factory)
  throws InvalidAdaptationException {
    if (!element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC))) {
      throw new InvalidAdaptationException(
          "Value Mapper method must be public and static",
          element
      );
    }

    final var head = TypePattern.from(element.getReturnType());
    final var enumBoundedTypeParameters = getEnumBoundedTypeParameters(element);
    final var method = element.getSimpleName().toString();
    final var parameters = new ArrayList<TypePattern>();
    for (final var parameter : element.getParameters()) {
      parameters.add(TypePattern.from(parameter));
    }

    return new TypeRule(head, enumBoundedTypeParameters, parameters, factory, method);
  }

  private Set<String>
  getEnumBoundedTypeParameters(final ExecutableElement element)
  throws InvalidAdaptationException {
    final var enumBoundedTypeParameters = new HashSet<String>();
    // Check type parameters are bounded only by enum type or not at all
    for (final var typeParameter : element.getTypeParameters()) {
      final var bounds = typeParameter.getBounds();
      for (final var bound : bounds) {
        final var erasure = typeUtils.erasure(bound);
        final var objectType = elementUtils.getTypeElement("java.lang.Object").asType();
        final var enumType = typeUtils.erasure(elementUtils.getTypeElement("java.lang.Enum").asType());
        if (typeUtils.isSameType(erasure, objectType)) {
          // Nothing to do
        } else if (typeUtils.isSameType(erasure, enumType)) {
          enumBoundedTypeParameters.add(typeParameter.getSimpleName().toString());
        } else {
          throw new InvalidAdaptationException(
              "Value Mapper method type parameter must be unbounded, or bounded by enum type only",
              element
          );
        }
      }
    }
    return enumBoundedTypeParameters;
  }

  private ActivityTypeRecord
  parseActivityType(final PackageElement adaptationElement, final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var name = this.getActivityTypeName(activityTypeElement);
    final var mapper = this.getActivityMapper(adaptationElement, activityTypeElement);
    final var validations = this.getActivityValidations(activityTypeElement);
    final var parameters = this.getActivityParameters(activityTypeElement);
    final var effectModel = this.getActivityEffectModel(activityTypeElement);

    return new ActivityTypeRecord(activityTypeElement, name, mapper, validations, parameters, effectModel);
  }

  private String
  getActivityTypeName(final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this
        .getAnnotationMirrorByType(activityTypeElement, ActivityType.class)
        .orElseThrow(() -> new InvalidAdaptationException(
            "An activity is somehow missing an @Activity annotation",
            activityTypeElement));

    final var nameAttribute = this
        .getAnnotationAttribute(annotationMirror, "value")
        .orElseThrow(() -> new InvalidAdaptationException(
            "Unable to get value attribute of annotation",
            activityTypeElement,
            annotationMirror));

    return (String) nameAttribute.getValue();
  }

  private ActivityMapperRecord
  getActivityMapper(final PackageElement adaptationElement, final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this.getAnnotationMirrorByType(activityTypeElement, ActivityType.WithMapper.class);
    if (annotationMirror.isEmpty()) {
      return ActivityMapperRecord.generatedFor(
          ClassName.get(activityTypeElement),
          adaptationElement);
    }

    final var mapperType = (DeclaredType) this
        .getAnnotationAttribute(annotationMirror.get(), "value")
        .orElseThrow(() -> new InvalidAdaptationException(
          "Unable to get value attribute of annotation",
          activityTypeElement,
          annotationMirror.get()))
        .getValue();

    return ActivityMapperRecord.custom(
        ClassName.get((TypeElement) mapperType.asElement()));
  }

  private List<ActivityValidationRecord>
  getActivityValidations(final TypeElement activityTypeElement) {
    final var validations = new ArrayList<ActivityValidationRecord>();

    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getAnnotation(ActivityType.Validation.class) == null) continue;

      final var name = element.getSimpleName().toString();
      final var message = element.getAnnotation(ActivityType.Validation.class).value();

      validations.add(new ActivityValidationRecord(name, message));
    }

    return validations;
  }

  private List<ActivityParameterRecord>
  getActivityParameters(final TypeElement activityTypeElement) {
    final var parameters = new ArrayList<ActivityParameterRecord>();

    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) continue;
      if (element.getAnnotation(ActivityType.Parameter.class) == null) continue;

      final var name = element.getSimpleName().toString();
      final var type = element.asType();

      parameters.add(new ActivityParameterRecord(name, type, element));
    }

    return parameters;
  }

  private Optional<Pair<String, ActivityType.Executor>>
  getActivityEffectModel(final TypeElement activityTypeElement) {
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) continue;

      final var executorAnnotation = element.getAnnotation(ActivityType.EffectModel.class);
      if (executorAnnotation == null) continue;

      return Optional.of(Pair.of(element.getSimpleName().toString(), executorAnnotation.value()));
    }

    return Optional.empty();
  }

  private List<TypeElement>
  getAdaptationMapperClasses(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var mapperClassElements = new ArrayList<TypeElement>();

    for (final var withMappersAnnotation : getRepeatableAnnotation(adaptationElement, Adaptation.WithMappers.class)) {
      final var attribute = getAnnotationAttribute(withMappersAnnotation, "value").orElseThrow();

      if (!(attribute.getValue() instanceof DeclaredType)) {
        throw new InvalidAdaptationException(
            "Mappers class not yet defined",
            adaptationElement,
            withMappersAnnotation,
            attribute);
      }

      mapperClassElements.add((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
    }

      return mapperClassElements;
  }

  private List<TypeElement>
  getAdaptationActivityTypes(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var activityTypeElements = new ArrayList<TypeElement>();

    for (final var activityTypeAnnotation : getRepeatableAnnotation(adaptationElement, Adaptation.WithActivityType.class)) {
      final var attribute = getAnnotationAttribute(activityTypeAnnotation, "value").orElseThrow();

      if (!(attribute.getValue() instanceof DeclaredType)) {
        throw new InvalidAdaptationException(
            "Activity type not yet defined",
            adaptationElement,
            activityTypeAnnotation,
            attribute);
      }

      // DeclaredType cast works because we checked above that attribute is indeed a DeclaredType
      // TypeElement cast works because the element of a DeclaredType must be a TypeElement
      activityTypeElements.add((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
    }

    return activityTypeElements;
  }

  private TypeElement
  getAdaptationModel(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this
        .getAnnotationMirrorByType(adaptationElement, Adaptation.class)
        .orElseThrow(() -> new InvalidAdaptationException(
            "The adaptation package is somehow missing an @Adaptation annotation",
            adaptationElement));

    final var modelAttribute = getAnnotationAttribute(annotationMirror, "model").orElseThrow();
    if (!(modelAttribute.getValue() instanceof DeclaredType)) {
      throw new InvalidAdaptationException(
          "The top-level model is not yet defined",
          adaptationElement,
          annotationMirror,
          modelAttribute);
    }

    // TODO: Check that the given model conforms to the expected protocol.
    //   * Has a (1,1) constructor that takes a type $Schema and a Registrar<$Schema>.
    //   It doesn't actually need to subclass Model.
    // TODO: Consider enrolling the given model in a dependency injection framework,
    //   such that the Cursor can be injected like any other constructor argument,
    //   and indeed such that other arguments can flexibly be supported.


    return (TypeElement) ((DeclaredType) modelAttribute.getValue()).asElement();
  }

  private Optional<Map<String, CodeBlock>> buildParameterMapperBlocks(final AdaptationRecord adaptation, final ActivityTypeRecord activityType) {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, adaptation.typeRules);
    var failed = false;
    final var mapperBlocks = new HashMap<String, CodeBlock>();

    for (final var parameter : activityType.parameters) {
      final var mapperBlock = resolver.instantiateMapperFor(parameter.type);
      if (mapperBlock.isPresent()) {
        mapperBlocks.put(parameter.name, mapperBlock.get());
      } else {
        failed = true;
        messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate value mapper for parameter", parameter.element);
      }
    }

    if (failed) return Optional.empty();
    return Optional.of(mapperBlocks);
  }

  private Optional<JavaFile> generateActivityMapper(final AdaptationRecord adaptation, final ActivityTypeRecord activityType) {
    final var maybeMapperBlocks = buildParameterMapperBlocks(adaptation, activityType);
    if (maybeMapperBlocks.isEmpty()) return Optional.empty();
    final var mapperBlocks = maybeMapperBlocks.get();

    final var typeSpec =
        TypeSpec
            .classBuilder(activityType.mapper.name)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            // The fields and methods of the activity determines the overall behavior of this class.
            .addOriginatingElement(activityType.declaration)
            // TODO: Add an originating element for each of the mapper rulesets associated with the adaptation.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ActivityMapper.class),
                    TypeName.get(activityType.declaration.asType())))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addFields(
                activityType.parameters
                    .stream()
                    .map(parameter -> FieldSpec
                        .builder(
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.ValueMapper.class),
                                TypeName.get(parameter.type).box()),
                            "mapper_" + parameter.name)
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                    .collect(Collectors.toList()))
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    // Suppress unchecked warnings because the resolver has to put some big casting in for Class parameters
                    .addAnnotation(
                        AnnotationSpec
                            .builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .addCode(
                        activityType.parameters
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
                    .addStatement("return $S", activityType.name)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getParameters")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.Map.class,
                        String.class,
                        gov.nasa.jpl.aerie.merlin.protocol.ValueSchema.class))
                    .addStatement(
                        "final var $L = new $T()",
                        "parameters",
                        ParameterizedTypeName.get(
                            java.util.HashMap.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.ValueSchema.class))
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "$L.put($S, this.mapper_$L.getValueSchema())",
                                    "parameters",
                                    parameter.name,
                                    parameter.name))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "parameters")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getArguments")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.Map.class,
                        String.class,
                        gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class))
                    .addParameter(
                        TypeName.get(activityType.declaration.asType()),
                        "activity",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "arguments",
                        ParameterizedTypeName.get(
                            java.util.HashMap.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class))
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "$L.put($S, this.mapper_$L.serializeValue($L.$L))",
                                    "arguments",
                                    parameter.name,
                                    parameter.name,
                                    "activity",
                                    parameter.name))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "arguments")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiateDefault")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.get(activityType.declaration.asType()))
                    .addStatement("return new $T()", TypeName.get(activityType.declaration.asType()))
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.get(activityType.declaration.asType()))
                    .addException(gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                    .addParameter(
                        ParameterizedTypeName.get(
                            java.util.Map.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class),
                        "arguments",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "activity",
                        TypeName.get(activityType.declaration.asType()))
                    .beginControlFlow(
                        "for (final var $L : $L.entrySet())",
                        "entry",
                        "arguments")
                    .beginControlFlow(
                        "switch ($L.getKey())",
                        "entry")
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .add("case $S:\n", parameter.name)
                                .indent()
                                .addStatement(
                                    "$L.$L = this.mapper_$L"
                                    + "\n" + ".deserializeValue($L.getValue())"
                                    + "\n" + ".getSuccessOrThrow($$ -> new $T())",
                                    "activity",
                                    parameter.name,
                                    parameter.name,
                                    "entry",
                                    gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                                .addStatement("break")
                                .unindent())
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addCode(
                        CodeBlock
                            .builder()
                            .add("default:\n")
                            .indent()
                            .addStatement(
                                "throw new $T()",
                                gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                            .unindent()
                            .build())
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement(
                        "return $L",
                        "activity")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getValidationFailures")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.List.class,
                        String.class))
                    .addParameter(
                        TypeName.get(activityType.declaration.asType()),
                        "activity",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "failures",
                        ParameterizedTypeName.get(
                            java.util.ArrayList.class,
                            String.class))
                    .addCode(
                        activityType.validations
                            .stream()
                            .map(validation -> CodeBlock
                                .builder()
                                .addStatement(
                                    "if (!$L.$L()) failures.add($S)",
                                    "activity",
                                    validation.methodName,
                                    validation.failureMessage))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "failures")
                    .build())
            .build();

    return Optional.of(JavaFile
        .builder(activityType.mapper.name.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build());
  }

  private JavaFile generateActivityActions(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getActivityActionsName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            // TODO: List found task spec types as dependencies of this generated file.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addMethods(
                adaptation.activityTypes
                    .stream()
                    .flatMap(entry -> List
                        .of(
                            MethodSpec
                                .methodBuilder("spawn")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper.name)
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
                                            gov.nasa.jpl.aerie.time.Duration.class,
                                            "duration")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper.name)
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
                                            gov.nasa.jpl.aerie.time.Duration.class,
                                            "unit")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration),
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
                                    ClassName.get(entry.declaration),
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

  private JavaFile generateAdaptationFactory(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getFactoryName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.Adaptation.class),
                            WildcardTypeName.get(this.typeUtils.getWildcardType(null, null))))
                    .addStatement(
                        "return this.instantiate($T.builder())",
                        gov.nasa.jpl.aerie.merlin.timeline.Schema.class)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("$Schema"))
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.Adaptation.class),
                            TypeVariableName.get("$Schema")))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.timeline.Schema.Builder.class),
                            TypeVariableName.get("$Schema")),
                        "schemaBuilder",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T<>($L)",
                        "builder",
                        gov.nasa.jpl.aerie.merlin.framework.AdaptationBuilder.class,
                        "schemaBuilder")
                    .addStatement(
                        "final var $L = new $T($L)",
                        "registrar",
                        gov.nasa.jpl.aerie.merlin.framework.Registrar.class,
                        "builder")
                    .addStatement(
                        "final var $L = new $T($L)",
                        "model",
                        ClassName.get(adaptation.topLevelModel),
                        "registrar")
                    .addCode("\n")
                    .addCode(
                        adaptation.activityTypes
                            .stream()
                            .map(activityType -> {
                              if (activityType.effectModel.isEmpty()) {
                                return CodeBlock
                                    .builder()
                                    .addStatement(
                                        "$L.noopTask(new $T())",
                                        "builder",
                                        activityType.mapper.name);
                              }
                              final var effectModel = activityType.effectModel.get();

                              if (effectModel.getRight() == ActivityType.Executor.Threaded) {
                                return CodeBlock
                                    .builder()
                                    .addStatement(
                                        "$L.threadedTask("
                                        + "\n" + "new $T(),"
                                        + "\n" + "activity -> activity.$L($L))",
                                        "builder",
                                        activityType.mapper.name,
                                        effectModel.getLeft(),
                                        "model");
                              } else if (effectModel.getRight() == ActivityType.Executor.Replaying) {
                                return CodeBlock
                                    .builder()
                                    .addStatement(
                                        "$L.replayingTask("
                                        + "\n" + "new $T(),"
                                        + "\n" + "activity -> activity.$L($L))",
                                        "builder",
                                        activityType.mapper.name,
                                        effectModel.getLeft(),
                                        "model");
                              } else {
                                messager.printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "Internal error: unknown executor type " + effectModel.getRight());
                                return CodeBlock.builder();
                              }
                            })
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addCode("\n")
                    .addStatement(
                        "return $L.build()",
                        "builder")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }


  private List<AnnotationMirror> getRepeatableAnnotation(final Element element, final Class<? extends Annotation> annotationClass) {
    final var containerClass = annotationClass.getAnnotation(Repeatable.class).value();

    final var annotationType = this.elementUtils.getTypeElement(annotationClass.getCanonicalName()).asType();
    final var containerType = this.elementUtils.getTypeElement(containerClass.getCanonicalName()).asType();

    final var mirrors = new ArrayList<AnnotationMirror>();
    for (final var mirror : element.getAnnotationMirrors()) {
      if (this.typeUtils.isSameType(annotationType, mirror.getAnnotationType())) {
        mirrors.add(mirror);
      } else if (this.typeUtils.isSameType(containerType, mirror.getAnnotationType())) {
        // SAFETY: a container annotation has a value() attribute that is an array of annotations
        @SuppressWarnings("unchecked")
        final var containedMirrors =
            (List<AnnotationMirror>)
                getAnnotationAttribute(mirror, "value")
                    .orElseThrow()
                    .getValue();

        mirrors.addAll(containedMirrors);
      }
    }

    return mirrors;
  }

  private Optional<AnnotationValue>
  getAnnotationAttribute(final AnnotationMirror annotationMirror, final String attributeName) {
    for (final var entry : annotationMirror.getElementValues().entrySet()) {
      if (Objects.equals(attributeName, entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }

    return Optional.empty();
  }

  private Optional<AnnotationMirror>
  getAnnotationMirrorByType(final Element element, final Class<? extends Annotation> annotationClass) {
    final var annotationType = this.elementUtils
        .getTypeElement(annotationClass.getCanonicalName())
        .asType();

    for (final var x : element.getAnnotationMirrors()) {
      if (this.typeUtils.isSameType(annotationType, x.getAnnotationType())) {
        return Optional.of(x);
      }
    }

    return Optional.empty();
  }
}
