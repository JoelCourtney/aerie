package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * Abstract class defining methods that must be implemented by global constraints such as mutex or cardinality
 * Also provides a directory for creating these constraints
 */
public abstract class GlobalConstraint {

  //todo: probably needs a domain

  //is the constraint enforced on its domain
  public abstract ConstraintState isEnforced(Plan plan, Windows windows);


}
