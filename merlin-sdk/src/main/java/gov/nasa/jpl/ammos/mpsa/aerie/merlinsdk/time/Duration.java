package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;

import java.util.Collections;
import java.util.List;

public final class Duration implements Comparable<Duration> {
  public static Duration ZERO = Duration.fromQuantity(0, TimeUnit.MICROSECONDS);

  // Range of -2^63 to 2^63 - 1.
  public final long durationInMicroseconds;

  private Duration(final long durationInMicroseconds) {
    this.durationInMicroseconds = durationInMicroseconds;
  }

  public static Duration fromQuantity(final long quantity, final TimeUnit units) {
    switch (units) {
      case MICROSECONDS: return new Duration(quantity);
      case MILLISECONDS: return new Duration(quantity * 1000L);
      case SECONDS:      return new Duration(quantity * 1000000L);
      case MINUTES:      return new Duration(quantity * 1000000L * 60L);
      case HOURS:        return new Duration(quantity * 1000000L * 60L * 60L);
      case DAYS:         return new Duration(quantity * 1000000L * 60L * 60L * 24L);
      case WEEKS:        return new Duration(quantity * 1000000L * 60L * 60L * 24L * 7L);
      default: throw new Error("Unknown TimeUnit value: " + units);
    }
  }

  public long asIntegerQuantity(final TimeUnit units) {
    switch (units) {
      case MICROSECONDS: return this.durationInMicroseconds;
      case MILLISECONDS: return this.durationInMicroseconds / 1000L;
      case SECONDS:      return this.durationInMicroseconds / 1000000L;
      case MINUTES:      return this.durationInMicroseconds / 1000000L / 60L;
      case HOURS:        return this.durationInMicroseconds / 1000000L / 60L / 60L;
      case DAYS:         return this.durationInMicroseconds / 1000000L / 60L / 60L / 24L;
      case WEEKS:        return this.durationInMicroseconds / 1000000L / 60L / 60L / 24L / 7L;
      default: throw new Error("Unknown TimeUnit value: " + units);
    }
  }

  public static Duration add(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.addExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration subtract(final Duration left, final Duration right) throws ArithmeticException {
    return new Duration(Math.subtractExact(left.durationInMicroseconds, right.durationInMicroseconds));
  }

  public static Duration min(final Duration x, final Duration y) {
    return Collections.min(List.of(x, y));
  }

  public static Duration max(final Duration x, final Duration y) {
    return Collections.max(List.of(x, y));
  }

  public boolean shorterThan(final Duration other) {
    return this.compareTo(other) < 0;
  }

  public boolean longerThan(final Duration other) {
    return this.compareTo(other) > 0;
  }

  public boolean isNegative() {
    return this.durationInMicroseconds < 0;
  }

  public boolean isPositive() {
    return this.durationInMicroseconds > 0;
  }

  public boolean isZero() {
    return this.durationInMicroseconds == 0;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Duration)) return false;
    final var other = (Duration)o;

    return (this.durationInMicroseconds == other.durationInMicroseconds);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.durationInMicroseconds);
  }

  @Override
  public String toString() {
    return "" + this.durationInMicroseconds + "µs";
  }

  @Override
  public int compareTo(final Duration other) {
    return Long.compareUnsigned(this.durationInMicroseconds, other.durationInMicroseconds);
  }
}
