package gov.nasa.jpl.aerie.banananation.activities;

import gov.nasa.jpl.aerie.banananation.Mission;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.EffectModel;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Parameter;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType.Validation;

/**
 * Pick a banana from the plant.
 */
@ActivityType("PickBanana")
public final class PickBananaActivity {
  @Parameter
  public int quantity = 10;

  @Validation("quantity must be positive")
  public boolean validateQuantity() {
    return this.quantity > 0;
  }

  @EffectModel
  public void run(final Mission mission) {
    mission.plant.add(-quantity);
  }
}
