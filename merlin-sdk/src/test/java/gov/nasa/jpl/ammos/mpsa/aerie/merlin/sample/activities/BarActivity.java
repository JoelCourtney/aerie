package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public final class BarActivity<$Timeline> extends Activity<$Timeline> {
  @Override
  public void modelEffects(final Context<$Timeline> ctx, final FooResources<? super $Timeline> resources) {
    resources.rate.add(ctx, 1.0);
    ctx.delay(1, Duration.SECOND);
    resources.rate.add(ctx, 2.0);
    resources.rate.add(ctx, resources.rate.get(ctx));
  }
}
