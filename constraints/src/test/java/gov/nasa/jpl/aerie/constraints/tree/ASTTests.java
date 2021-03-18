package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static gov.nasa.jpl.aerie.constraints.Utilities.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.time.Duration.SECONDS;

public class ASTTests {

  @Test
  public void testNot() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var windows = new Windows();
    windows.add(Window.between(0, Inclusive, 5, Exclusive, SECONDS));
    windows.add(Window.between(10, Exclusive, 15, Exclusive, SECONDS));
    windows.add(Window.at(20, SECONDS));

    final var result = new Not(Supplier.of(windows)).evaluate(simResults, Map.of());

    final var expected = new Windows();
    expected.add(Window.between(5, Inclusive, 10, Inclusive, SECONDS));
    expected.add(Window.between(15, Inclusive, 20, Exclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testAnd() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows();
    left.add(Window.between(0, Inclusive, 5, Exclusive, SECONDS));
    left.add(Window.between(6, Inclusive, 7, Inclusive, SECONDS));
    left.add(Window.between(8, Exclusive, 9, Exclusive, SECONDS));
    left.add(Window.between(10, Exclusive, 15, Exclusive, SECONDS));
    left.add(Window.at(20, SECONDS));

    final var right = new Windows();
    right.add(Window.between(0, Inclusive, 5, Inclusive, SECONDS));
    right.add(Window.between(7, Inclusive, 8, Exclusive, SECONDS));
    right.add(Window.between(10, Inclusive, 12, Inclusive, SECONDS));
    right.add(Window.between(15, Inclusive, 20, Exclusive, SECONDS));

    final var result = new And(Supplier.of(left), Supplier.of(right)).evaluate(simResults, Map.of());

    final var expected = new Windows();
    expected.add(Window.between( 0, Inclusive,  5, Exclusive, SECONDS));
    expected.add(Window.at(7, SECONDS));
    expected.add(Window.between( 10, Exclusive,  12, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testOr() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var left = new Windows();
    left.add(Window.between(0, Inclusive, 5, Exclusive, SECONDS));
    left.add(Window.between(6, Inclusive, 7, Inclusive, SECONDS));
    left.add(Window.between(8, Exclusive, 9, Exclusive, SECONDS));
    left.add(Window.between(10, Exclusive, 15, Exclusive, SECONDS));
    left.add(Window.at(20, SECONDS));

    final var right = new Windows();
    right.add(Window.between(0, Inclusive, 5, Inclusive, SECONDS));
    right.add(Window.between(7, Inclusive, 8, Exclusive, SECONDS));
    right.add(Window.between(10, Inclusive, 12, Inclusive, SECONDS));
    right.add(Window.between(15, Inclusive, 20, Exclusive, SECONDS));

    final var result = new Or(Supplier.of(left), Supplier.of(right)).evaluate(simResults, Map.of());

    final var expected = new Windows();
    expected.add(Window.between(  0, Inclusive,   5, Inclusive, SECONDS));
    expected.add(Window.between(  6, Inclusive,   8, Exclusive, SECONDS));
    expected.add(Window.between(  8, Exclusive,   9, Exclusive, SECONDS));
    expected.add(Window.between( 10, Inclusive,  20, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealValue() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var result = new RealValue(7).evaluate(simResults, Map.of());

    final var expected = new LinearProfile(
        new LinearProfilePiece(simResults.bounds, 7, 0));

    assertEquivalent(expected, result);
  }

  @Test
  public void testDiscreteValue() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var result = new DiscreteValue(SerializedValue.of("IDLE")).evaluate(simResults, Map.of());

    final var expected = new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(simResults.bounds, SerializedValue.of("IDLE"))
        ));

    assertEquivalent(expected, result);
  }

  @Test
  public void testAsReal() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var source = new DiscreteProfile(
        List.of(
            new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(1)),
            new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Inclusive, SECONDS), SerializedValue.of(2)),
            new DiscreteProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), SerializedValue.of(3)),
            new DiscreteProfilePiece(Window.between(15, Inclusive, 15, Inclusive, SECONDS), SerializedValue.of(4)),
            new DiscreteProfilePiece(Window.between(15, Exclusive, 20, Inclusive, SECONDS), SerializedValue.of(5))
        ));

    final var result = new AsReal(Supplier.of(source)).evaluate(simResults, Map.of());

    final var expected = new LinearProfile(
        new LinearProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), 1, 0),
        new LinearProfilePiece(Window.between(5, Inclusive, 10, Inclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Window.between(15, Inclusive, 15, Inclusive, SECONDS), 4, 0),
        new LinearProfilePiece(Window.between(15, Exclusive, 20, Inclusive, SECONDS), 5, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testDiscreteResource() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Window.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Window.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Window.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Window.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Window.at(5, SECONDS), SerializedValue.of("two"))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Window.at(6, SECONDS), SerializedValue.of("three")))
        )
    );

    final var result = new DiscreteResource("discrete2").evaluate(simResults, Map.of());

    final var expected = simResults.discreteProfiles.get("discrete2");

    assertEquivalent(expected, result);
  }

  @Test
  public void testRealResource() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(
            "real1", new LinearProfile(new LinearProfilePiece(Window.at(1, SECONDS), 0, 1)),
            "real2", new LinearProfile(new LinearProfilePiece(Window.at(2, SECONDS), 0, 1)),
            "real3", new LinearProfile(new LinearProfilePiece(Window.at(3, SECONDS), 0, 1))
        ),
        Map.of(
            "discrete1", new DiscreteProfile(new DiscreteProfilePiece(Window.at(4, SECONDS), SerializedValue.of("one"))),
            "discrete2", new DiscreteProfile(new DiscreteProfilePiece(Window.at(5, SECONDS), SerializedValue.of("two"))),
            "discrete3", new DiscreteProfile(new DiscreteProfilePiece(Window.at(6, SECONDS), SerializedValue.of("three")))
            )
    );

    final var result = new RealResource("real2").evaluate(simResults, Map.of());

    final var expected = simResults.realProfiles.get("real2");

    assertEquivalent(expected, result);
  }

  @Test
  public void testForEachActivity() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(
            new ActivityInstance("1", "TypeA", Map.of(), Window.between(4, 6, SECONDS)),
            new ActivityInstance("2", "TypeB", Map.of(), Window.between(5, 7, SECONDS)),
            new  ActivityInstance("3", "TypeA", Map.of(), Window.between(9, 10, SECONDS))
        ),
        Map.of(),
        Map.of()
    );

    final var violation = new Violation(List.of(), new Windows(Window.between(4, 6, SECONDS)));
    final var result = new ForEachActivity(
        "TypeA",
        "act",
        new Supplier<>(List.of(violation))
    ).evaluate(simResults, Map.of());

    final var expected = List.of(
        new Violation(List.of("1"), new Windows(Window.between(4, 6, SECONDS))),
        new Violation(List.of("3"), new Windows(Window.between(4, 6, SECONDS))));

    assertEquivalent(expected, result);
  }

  @Test
  public void testNestedForEachActivity() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(
            new ActivityInstance("1", "TypeA", Map.of(), Window.between(4, 6, SECONDS)),
            new ActivityInstance("2", "TypeB", Map.of(), Window.between(5, 7, SECONDS)),
            new  ActivityInstance("3", "TypeA", Map.of(), Window.between(9, 10, SECONDS))
        ),
        Map.of(),
        Map.of()
    );

    final var violation = new Violation(List.of(), new Windows(Window.between(4, 6, SECONDS)));
    final var result = new ForEachActivity(
        "TypeA",
        "act",
        new ForEachActivity(
            "TypeB",
            "act",
            new Supplier<>(List.of(violation))
        )
    ).evaluate(simResults, Map.of());

    // We expect two violations because there are two activities of TypeA
    // The details of the violation will be the same, since we are using a supplier
    final var expected = List.of(
        new Violation(List.of("1", "2"), new Windows(Window.between(4, 6, SECONDS))),
        new Violation(List.of("3", "2"), new Windows(Window.between(4, 6, SECONDS))));

    assertEquivalent(expected, result);
  }

  @Test
  public void testViolationsOf() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var windows = new Windows();
    windows.add(Window.between(1, 4, SECONDS));
    windows.add(Window.between(7,20, SECONDS));
    final var result = new ViolationsOf(new Supplier<>(windows)).evaluate(simResults, Map.of());

    final var expected = List.of(new Violation(Windows.minus(new Windows(simResults.bounds), windows)));

    assertEquivalent(expected, result);
  }

  @Test
  public void testDuring() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act", new ActivityInstance("1", "TypeA", Map.of(), Window.between(4, 8, SECONDS))
    );

    final var result = new During("act").evaluate(simResults, environment);

    final var expected = new Windows(Window.between(4, 8, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testStartOf() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act", new ActivityInstance("1", "TypeA", Map.of(), Window.between(4, 8, SECONDS))
    );

    final var result = new StartOf("act").evaluate(simResults, environment);

    final var expected = new Windows(Window.at(4, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testEndOf() {
    final var simResults = new SimulationResults(
        Window.between(0, 20, SECONDS),
        List.of(),
        Map.of(),
        Map.of()
    );

    final var environment = Map.of(
        "act", new ActivityInstance("1", "TypeA", Map.of(), Window.between(4, 8, SECONDS))
    );

    final var result = new EndOf("act").evaluate(simResults, environment);

    final var expected = new Windows(Window.at(8, SECONDS));

    assertEquivalent(expected, result);
  }

  private static final class Supplier<T> implements Expression<T> {
    private final T value;

    public Supplier(final T value) {
      this.value = value;
    }


    @Override
    public T evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
      return this.value;
    }

    @Override
    public String prettyPrint(final String prefix) {
      return value.toString();
    }

    public static <V> Supplier<V> of(V value) {
      return new Supplier<>(value);
    }
  }
}
