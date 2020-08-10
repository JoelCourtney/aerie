package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.http;

import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.json.JsonParseResult.FailureReason;

public class InvalidEntityException extends Exception {

  public final List<String> breadcrumbs;
  public final String message;

  public InvalidEntityException(FailureReason reason) {
    breadcrumbs = reason.breadcrumbs;
    message = reason.reason;
  }
}
