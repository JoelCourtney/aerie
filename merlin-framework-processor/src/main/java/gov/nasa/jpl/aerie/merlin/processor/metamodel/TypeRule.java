package gov.nasa.jpl.aerie.merlin.processor.metamodel;

import com.squareup.javapoet.ClassName;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;

import javax.lang.model.element.AnnotationMirror;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TypeRule(
    TypePattern head,
    Set<String> enumBoundedTypeParameters,
    List<TypePattern> parameters,
    ClassName factory,
    String method
) {
}
