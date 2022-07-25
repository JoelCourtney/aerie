package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ParameterRecord;
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Mapper method generator for all export types (activities and configurations).
 * Generates common methods between all export types.
 */
public abstract sealed class MapperMethodMaker permits
    AllDefinedMethodMaker,
    AllStaticallyDefinedMethodMaker,
    NoneDefinedMethodMaker,
    SomeStaticallyDefinedMethodMaker
{
  /*package-private*/ final ExportTypeRecord exportType;
  /*package-private*/ final String metaName;
  /*package-private*/ final Class<?> unconstructableInstantiateException;

  public MapperMethodMaker(final ExportTypeRecord exportType) {
    this.exportType = exportType;

    // TODO currently only 2 permitted classes (activity and config. type records),
    //  this should be changed to a switch expression once sealed class pattern-matching switch expressions exist
    if (exportType instanceof ActivityTypeRecord) {
      this.metaName = "activity";
      this.unconstructableInstantiateException = TaskSpecType.UnconstructableTaskSpecException.class;
    } else { // is instanceof ConfigurationTypeRecord
      this.metaName = "configuration";
      this.unconstructableInstantiateException = ConfigurationType.UnconstructableConfigurationException.class;
    }
  }

  public abstract MethodSpec makeInstantiateMethod();

  public /*non-final*/ List<String> getParametersWithDefaults() {
    return exportType.parameters().stream().map(p -> p.name).toList();
  }

  public /*non-final*/ MethodSpec makeGetParametersMethod() {
    return MethodSpec.methodBuilder("getParameters")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.ArrayList.class,
            Parameter.class))
        .addStatement(
            "final var $L = new $T()",
            "parameters",
            ParameterizedTypeName.get(
                java.util.ArrayList.class,
                Parameter.class))
        .addCode(
            exportType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.add(new $T($S, this.mapper_$L.getValueSchema()))",
                        "parameters",
                        Parameter.class,
                        parameter.name,
                        parameter.name))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "parameters")
        .build();
  }

  public /*non-final*/ MethodSpec makeGetArgumentsMethod() {
    return MethodSpec
        .methodBuilder("getArguments")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.Map.class,
            String.class,
            gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addParameter(
            TypeName.get(exportType.declaration().asType()),
            metaName,
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "arguments",
            ParameterizedTypeName.get(
                java.util.HashMap.class,
                String.class,
                gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addCode(
            exportType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L()))",
                        "arguments",
                        parameter.name,
                        parameter.name,
                        metaName,
                        parameter.name
                    ))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "arguments")
        .build();
  }

  public final MethodSpec makeGetRequiredParametersMethod() {
    final var optionalParams = getParametersWithDefaults();
    final var requiredParams = exportType.parameters().stream().filter(p -> !optionalParams.contains(p.name)).toList();

    return MethodSpec.methodBuilder("getRequiredParameters")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.List.class,
            String.class))
        .addStatement(
            "return $T.of($L)",
            List.class,
            requiredParams.stream().map(p -> "\"%s\"".formatted(p.name)).collect(Collectors.joining(", ")))
        .build();
  }

  public final MethodSpec makeGetValidationFailuresMethod() {
    return MethodSpec
        .methodBuilder("getValidationFailures")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.List.class,
            String.class))
        .addParameter(
            TypeName.get(exportType.declaration().asType()),
            metaName,
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "failures",
            ParameterizedTypeName.get(
                java.util.ArrayList.class,
                String.class))
        .addCode(
            exportType.validations()
                .stream()
                .map(validation -> CodeBlock
                    .builder()
                    .addStatement(
                        "if (!$L.$L()) failures.add($S)",
                        metaName,
                        validation.methodName,
                        validation.failureMessage))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "failures")
        .build();
  }

  protected final MethodSpec.Builder makeArgumentAssignments(
      final MethodSpec.Builder methodBuilder,
      final BiFunction<CodeBlock.Builder, ParameterRecord, CodeBlock.Builder> makeArgumentAssignment)
  {
    var mb = methodBuilder
        .beginControlFlow("for (final var $L : $L.entrySet())", "entry", "arguments")
        .beginControlFlow("switch ($L.getKey())", "entry")
        .addCode(
            exportType.parameters()
                .stream()
                .map(parameter -> {
                    final var caseBuilder = CodeBlock.builder()
                        .add("case $S:\n", parameter.name)
                        .indent();
                    return makeArgumentAssignment.apply(caseBuilder, parameter)
                        .addStatement("break")
                        .unindent();
                })
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addCode(
            CodeBlock
                .builder()
                .add("default:\n")
                .indent()
                .addStatement(
                    "throw $T.extraneousParameter($L.getKey())",
                    unconstructableInstantiateException,
                    "entry")
                .unindent()
                .build())
        .endControlFlow()
        .endControlFlow()
        .addCode("\n");

    return makeMissingArgumentsCheck(mb).addCode("\n");
  }

  private MethodSpec.Builder makeMissingArgumentsCheck(final MethodSpec.Builder methodBuilder) {
    // Ensure all parameters are non-null
    return methodBuilder
        .addStatement(
            "final var $L = new $T(\"$L\", \"$L\")",
            "missingArgsExBuilder",
            MissingArgumentsException.Builder.class,
            exportType.name(),
            metaName)
        .addCode(
            exportType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        // Re-serialize value since provided `arguments` map may not contain the value (when using `@WithDefaults` templates)
                        "$L.ifPresentOrElse($Wvalue -> $L.withProvidedArgument(\"$L\", this.mapper_$L.serializeValue(value)),$W() -> $L.withMissingArgument(\"$L\", this.mapper_$L.getValueSchema()))",
                        parameter.name,
                        "missingArgsExBuilder",
                        parameter.name,
                        parameter.name,
                        "missingArgsExBuilder",
                        parameter.name,
                        parameter.name))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
            .build())
        .addStatement(
            "final var $L = $L.build()",
            "missingArgsEx",
            "missingArgsExBuilder")
        .addStatement(
            "if (!$L.$L.isEmpty()) throw $L",
            "missingArgsEx",
            "missingArguments",
            "missingArgsEx");
  }

  static MapperMethodMaker make(final ExportTypeRecord exportType) {
    return switch (exportType.defaultsStyle()) {
      case AllStaticallyDefined -> new AllStaticallyDefinedMethodMaker(exportType);
      case NoneDefined -> new NoneDefinedMethodMaker(exportType);
      case AllDefined -> new AllDefinedMethodMaker(exportType);
      case SomeStaticallyDefined -> new SomeStaticallyDefinedMethodMaker(exportType);
    };
  }
}
