package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InvalidArgumentsException extends Exception {

  public final String metaName, containerName;
  public final List<ExtraneousArgument> extraneousArguments;
  public final List<UnconstructableArgument> unconstructableArguments;
  public final List<MissingArgument> missingArguments;
  public final List<ValidArgument> validArguments;

  public InvalidArgumentsException(
      final String metaName,
      final String containerName,
      final List<ExtraneousArgument> extraneousArguments,
      final List<UnconstructableArgument> unconstructableArguments,
      final List<MissingArgument> missingArguments,
      final List<ValidArgument> validArguments)
  {
    super(("Invalid arguments for %s \"%s\": "+
          "extraneous arguments: %s, "+
          "unconstructable arguments: %s, "+
          "missing arguments: %s, "+
          "valid arguments: %s"
          ).formatted(metaName, containerName,
              missingArguments,
              extraneousArguments,
              unconstructableArguments,
              validArguments));

    this.metaName = metaName;
    this.containerName = containerName;
    this.extraneousArguments = Collections.unmodifiableList(extraneousArguments);
    this.unconstructableArguments = Collections.unmodifiableList(unconstructableArguments);
    this.missingArguments = Collections.unmodifiableList(missingArguments);
    this.validArguments = Collections.unmodifiableList(validArguments);
  }

  public record ExtraneousArgument(String parameterName) { }

  public record UnconstructableArgument(String parameterName, String failure) { }

  public record MissingArgument(String parameterName, ValueSchema schema) { } // TODO remove schema

  public record ValidArgument(String parameterName, SerializedValue serializedValue) { }

  public static final class Builder {

    private final String metaName, containerName;
    private final List<ExtraneousArgument> extraneousArguments = new ArrayList<>();
    private final List<UnconstructableArgument> unconstructableArguments = new ArrayList<>();
    private final List<MissingArgument> missingArguments = new ArrayList<>();
    private final List<ValidArgument> validArguments = new ArrayList<>();

    public Builder(final String metaName, final String containerName) {
      this.metaName = metaName;
      this.containerName = containerName;
    }

    public Builder withExtraneousArgument(final String parameterName) {
      extraneousArguments.add(new ExtraneousArgument(parameterName));
      return this;
    }

    public Builder withUnconstructableArgument(final String parameterName, final String failure) {
      unconstructableArguments.add(new UnconstructableArgument(parameterName, failure));
      return this;
    }

    public Builder withMissingArgument(final String parameterName, final ValueSchema schema) {
      missingArguments.add(new MissingArgument(parameterName, schema));
      return this;
    }

    public Builder withValidArgument(final String parameterName, final SerializedValue serializedValue) {
      validArguments.add(new ValidArgument(parameterName, serializedValue));
      return this;
    }

    public void throwIfAny() throws InvalidArgumentsException {
      if (!(extraneousArguments.isEmpty() &&
            unconstructableArguments.isEmpty() &&
            missingArguments.isEmpty()))
      {
        throw new InvalidArgumentsException(metaName, containerName,
            extraneousArguments,
            unconstructableArguments,
            missingArguments,
            validArguments);
      }
    }
  }
}
