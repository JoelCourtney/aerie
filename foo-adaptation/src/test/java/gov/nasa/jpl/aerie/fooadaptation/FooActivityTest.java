package gov.nasa.jpl.aerie.fooadaptation;

import static gov.nasa.jpl.aerie.merlin.framework.ModelActions.delay;
import static org.junit.Assert.assertEquals;
import static gov.nasa.jpl.aerie.fooadaptation.generated.ActivityActions.spawn;

import gov.nasa.jpl.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.aerie.fooadaptation.generated.GeneratedAdaptationFactory;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.framework.ModelTestFramework;
import gov.nasa.jpl.aerie.time.Duration;
import org.junit.Test;

public class FooActivityTest {
  @Test
  public void testActivity() {
    ModelTestFramework.test(
      new GeneratedAdaptationFactory(),
      Mission::new,
      (model) ->
      {
        spawn(new FooActivity());
        delay(Duration.SECOND);
        assertEquals(15.0, model.simpleData.totalVolume.get(), 1e-9);
      });
  }
}
