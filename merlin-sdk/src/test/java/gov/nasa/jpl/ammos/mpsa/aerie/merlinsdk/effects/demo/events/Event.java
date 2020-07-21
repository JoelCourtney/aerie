package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;

import java.util.Objects;
import java.util.Optional;

// This can be mechanically derived from `EventHandler`.
public abstract class Event {
  private Event() {}

  public abstract <Result> Result visit(EventHandler<Result> visitor);

  public static Event addDataRate(final String binName, final double amount) {
    Objects.requireNonNull(binName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.addDataRate(binName, amount);
      }
    };
  }

  public static Event clearDataRate(final String binName) {
    Objects.requireNonNull(binName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.clearDataRate(binName);
      }
    };
  }

  public static Event log(final String message) {
    Objects.requireNonNull(message);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.log(message);
      }
    };
  }

  public static Event activity(final ActivityEvent event) {
    Objects.requireNonNull(event);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.activity(event);
      }
    };
  }

  public Optional<ActivityEvent> asActivity() {
    return this.visit(new DefaultEventHandler<>() {
      @Override
      public Optional<ActivityEvent> activity(final ActivityEvent event) {
        return Optional.of(event);
      }

      @Override
      public Optional<ActivityEvent> unhandled() {
        return Optional.empty();
      }
    });
  }

  @Override
  public final String toString() {
    return this.visit(new EventHandler<>() {
      @Override
      public String addDataRate(final String binName, final double amount) {
        return String.format("addDataRate(\"%s\", %s)",
            binName.replace("\\", "\\\\").replace("\"", "\\\""),
            amount);
      }

      @Override
      public String clearDataRate(final String binName) {
        return String.format("clearDataRate(\"%s\")",
            binName.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String log(final String message) {
        return String.format("log(\"%s\")",
            message.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String activity(final ActivityEvent event) {
        return String.format("activity.%s", event);
      }
    });
  }
}
