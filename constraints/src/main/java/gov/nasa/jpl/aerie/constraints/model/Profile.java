package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

public interface Profile<P extends Profile<P>> {
  Windows equalTo(P other, Window bounds);
  Windows notEqualTo(P other, Window bounds);
  Windows changePoints(Window bounds);
}
