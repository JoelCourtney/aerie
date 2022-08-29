package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class LinearProfile implements Profile<LinearProfile> {
  // IMPORTANT: Profile pieces must be non-overlapping, and increasing (based on interval field)
  public final List<LinearProfilePiece> profilePieces;

  public LinearProfile(final List<LinearProfilePiece> profilePieces) {
    this.profilePieces = Objects.requireNonNull(profilePieces);
  }

  public LinearProfile(final LinearProfilePiece... profilePieces) {
    this(List.of(profilePieces));
  }

  @Override
  public Windows equalTo(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::equalTo);
  }

  @Override
  public Windows notEqualTo(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::notEqualTo);
  }

  public Windows lessThan(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::lessThan);
  }

  public Windows lessThanOrEqualTo(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::lessThanOrEqualTo);
  }

  public Windows greaterThan(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::greaterThan);
  }

  public Windows greaterThanOrEqualTo(final LinearProfile other, final Interval bounds) {
    return this.getWindowsSatisfying(other, bounds, LinearProfilePiece::greaterThanOrEqualTo);
  }

  public LinearProfile plus(final LinearProfile other) {
    final var profilePieces = processIntersections(
        this,
        other,
        (p, o) -> {
          final var intersection = Interval.intersect(p.interval, o.interval);
          return new LinearProfilePiece(
              intersection,
              p.valueAt(intersection.start) + o.valueAt(intersection.start),
              p.rate + o.rate
          );
        },
        Interval.FOREVER
    );

    return new LinearProfile(profilePieces);
  }

  public LinearProfile times(final double multiplier) {
    return transformProfile(
        p -> new LinearProfilePiece(
            p.interval,
            p.initialValue * multiplier,
            p.rate * multiplier
        )
    );
  }

  public LinearProfile rate() {
    return transformProfile(
        p -> new LinearProfilePiece(
            p.interval,
            p.rate,
            0.0
        )
    );
  }

  private static boolean profileOutsideBounds(final LinearProfilePiece piece, final Interval bounds){
    return piece.interval.isStrictlyBefore(bounds) || piece.interval.isStrictlyAfter(bounds);
  }

  private Windows getWindowsSatisfying(final LinearProfile other, final Interval bounds, final BiFunction<LinearProfilePiece, LinearProfilePiece, List<Pair<Interval, Boolean>>> condition) {
    final var windows = new Windows();
    for (final var intervals : processIntersections(this, other, condition, bounds)) {
      for (final var interval: intervals) {
        windows.set(interval.getKey(), interval.getValue());
      }
    }
    windows.bound(bounds);
    return windows;
  }

    // TODO: Gaps in profiles will cause an error
    //       We may want to deal with gaps someday
    @Override
    public Windows changePoints(final Interval bounds) {
      final var changePoints = new Windows();
      if (this.profilePieces.size() == 0) return changePoints;

      final var iter = this.profilePieces.iterator();
      var prev = iter.next();
      if(!profileOutsideBounds(prev, bounds)) {
        changePoints.setAll(prev.changePoints());
      }
      while (iter.hasNext()) {
        final var curr = iter.next();
        if(!profileOutsideBounds(curr, bounds)) {
          changePoints.setAll(curr.changePoints());
        }

        if (Interval.meets(prev.interval, curr.interval)) {
          if (prev.finalValue() != curr.initialValue && !profileOutsideBounds(prev, bounds)) changePoints.setTrue(Interval.at(prev.interval.end));
        } else {
          throw new Error("Unexpected gap in profile pieces not allowed");
        }

        prev = curr;
      }

      changePoints.bound(bounds);
      return changePoints;
    }

  /**
   * Process each pair of intersecting profile pieces to build a list of results
   *
   * ASSUMPTION: Both sets of profile pieces are ordered in increasing order
   * @param right LinearProfile to intersect with
   * @param processor BiFunction taking two profile pieces and a desired result based on their intersection
   * @return Set of all windows within bounds for which condition is satisfied between this and another profile
   */
  private static <T> List<T> processIntersections(final LinearProfile left, final LinearProfile right, final BiFunction<LinearProfilePiece, LinearProfilePiece, T> processor, Interval bounds) {
    if (left.profilePieces.isEmpty() || right.profilePieces.isEmpty()) return new ArrayList<>();

    // Setup to step through profiles simultaneously,
    // finding all windows satisfying condition within the supplied bounds
    final var processedIntersections = new ArrayList<T>();
    final var rightIter = right.profilePieces.iterator();

    // Loop through the left pieces, stepping up the right pieces
    // appropriately each time to get intersections
    if (!rightIter.hasNext()) return processedIntersections;
    var rightPiece = rightIter.next();
    for (final var leftPiece : left.profilePieces) {
      //if left piece ends before bounds start, skip left piece
      if (profileOutsideBounds(leftPiece, bounds)) continue;
      // If left piece ends before right piece, skip left piece
      if (Interval.compareEndToStart(leftPiece.interval, rightPiece.interval) < 0) continue;

      // Step through right pieces ending before left piece starts
      // If no right pieces ending after left piece starts exist, end the loop
      while (Interval.compareEndToStart(rightPiece.interval, leftPiece.interval) < 0) {
        if (rightIter.hasNext()) {
          rightPiece = rightIter.next();
        } else {
          break;
        }
      }

      // Process all intersections with right pieces that start before the left piece ends
      // If we run out of right pieces, end the loop
      while (Interval.compareStartToEnd(rightPiece.interval, leftPiece.interval) <= 0) {
        processedIntersections.add(processor.apply(leftPiece, rightPiece));

        // Only step passed right piece if it doesn't exceed the left piece
        if (Interval.compareEndToEnd(rightPiece.interval, leftPiece.interval) <= 0) {
          if (rightIter.hasNext()) {
            rightPiece = rightIter.next();
            continue;
          }
        }
        break;
      }
    }

    return processedIntersections;
  }

  /**
   * Apply a transformation to each piece of this profile
   * @param transformation - function to transform a single piece of this profile into the desired form
   * @return a new LinearProfile representing the transformation of this profile
   */
  private LinearProfile transformProfile(final Function<LinearProfilePiece, LinearProfilePiece> transformation) {
    final var profilePieces = new ArrayList<LinearProfilePiece>(this.profilePieces.size());

    for (final var piece : this.profilePieces) {
      profilePieces.add(transformation.apply(piece));
    }

    return new LinearProfile(profilePieces);
  }

  public String toString() {
    return this.profilePieces.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof LinearProfile)) return false;
    final var other = (LinearProfile)obj;

    return Objects.equals(this.profilePieces, other.profilePieces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profilePieces);
  }
}
