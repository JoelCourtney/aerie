package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ParameterType;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;

/**
 * A serializable representation of an adaptation-specific activity domain object.
 *
 * Implementors of the {@link ActivityType} protocol may be constructed from parameters (which are
 * themselves implementors of the {@link ParameterType} protocol). A SerializedActivity is an adaptation-
 * agnostic representation of the data in an activity, structured as serializable primitives
 * composed using sequences and maps.
 *
 * For instance, if a FooActivity accepts two parameters, each of which is a 3D point in
 * space, then the serialized activity may look something like:
 *
 *     { "name": "Foo", "parameters": { "source": [1, 2, 3], "target": [4, 5, 6] } }
 *
 * This allows mission-agnostic treatment of activity data for persistence, editing, and
 * inspection, while allowing mission-specific adaptation to work with a domain-relevant
 * object via (de)serialization.
 */
public final class SerializedActivity {
  private final String typeName;
  private final Map<String, SerializedValue> parameters;

  public SerializedActivity(final String typeName, final Map<String, SerializedValue> parameters) {
    this.typeName = Objects.requireNonNull(typeName);
    this.parameters = Objects.requireNonNull(parameters);
  }

  /**
   * Gets the name of the activity type associated with this serialized data.
   *
   * @return A string identifying the activity type this object may be deserialized with.
   */
  public String getTypeName() {
    return this.typeName;
  }

  /**
   * Gets the serialized parameters associated with this serialized activity.
   *
   * @return A map of serialized parameters keyed by parameter name.
   */
  public Map<String, SerializedValue> getParameters() {
    return unmodifiableMap(this.parameters);
  }

  // SAFETY: If equals is overridden, then hashCode must also be overridden.
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SerializedActivity)) return false;

    final SerializedActivity other = (SerializedActivity)o;
    return
        (  Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.parameters, other.parameters)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.typeName, this.parameters);
  }

  @Override
  public String toString() {
    return "SerializedActivity { typeName = " + this.typeName + ", parameters = " + this.parameters.toString() + " }";
  }
}
