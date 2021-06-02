package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Violation {
  public final List<String> activityInstanceIds;
  public final List<String> resourceNames;
  public final Windows violationWindows;

  public Violation(final List<String> activityInstanceIds, final List<String> resourceNames, final Windows violationWindows) {
    this.activityInstanceIds = new ArrayList<>(activityInstanceIds);
    this.resourceNames = new ArrayList<>(resourceNames);
    this.violationWindows = violationWindows;
  }

  public Violation(final Windows violationWindows) {
    this(new ArrayList<>(), new ArrayList<>(), violationWindows);
  }

  public Violation clone() {
    return new Violation(List.copyOf(this.activityInstanceIds), List.copyOf(this.resourceNames), new Windows(violationWindows));
  }

  public void addActivityId(final String activityId) {
    this.activityInstanceIds.add(0, activityId);
  }

  public String prettyPrint(final String prefix) {
    StringBuilder s = new StringBuilder(prefix).append("{\n");
    s.append(prefix).append("  Activity IDs: [");
    final var iter = this.activityInstanceIds.iterator();
    while (iter.hasNext()) {
      s.append(" ").append(iter.next());
      if (iter.hasNext()) s.append(",");
    }
    s.append(prefix).append("  Resource Names: [");
    final var resources = this.resourceNames.iterator();
    while (resources.hasNext()) {
      s.append(" ").append(resources.next());
      if (resources.hasNext()) s.append(",");
    }

    s.append(" ],\n").append(prefix).append("  Windows: [").append(violationWindows).append("]\n");
    s.append("}");
    return s.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Violation)) return false;
    final var other = (Violation)obj;
    return Objects.equals(this.activityInstanceIds, other.activityInstanceIds) &&
           Objects.equals(this.resourceNames, other.resourceNames) &&
           Objects.equals(this.violationWindows, other.violationWindows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.activityInstanceIds, this.resourceNames, this.violationWindows);
  }
}
