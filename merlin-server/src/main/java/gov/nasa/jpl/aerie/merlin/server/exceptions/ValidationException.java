package gov.nasa.jpl.aerie.merlin.server.exceptions;

import gov.nasa.jpl.aerie.merlin.server.services.Breadcrumb;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

public class ValidationException extends Exception {
  private final List<Pair<List<Breadcrumb>, String>> errors;

  public ValidationException(final List<Pair<List<Breadcrumb>, String>> errors) {
    super(errors.toString());
    this.errors = Collections.unmodifiableList(errors);
  }

  public List<Pair<List<Breadcrumb>, String>> getValidationErrors() {
    return this.errors;
  }
}
