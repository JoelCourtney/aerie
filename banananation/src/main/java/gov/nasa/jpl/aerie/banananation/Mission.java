package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.generated.Model;
import gov.nasa.jpl.aerie.contrib.models.Register;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;

public class Mission extends Model {
  public final AdditiveRegister fruit;
  public final AdditiveRegister peel;
  public final Register<Flag> flag;

  public Mission(final Registrar registrar) {
    super(registrar);

    this.flag = Register.create(registrar.descend("flag"), Flag.A);
    this.peel = AdditiveRegister.create(registrar.descend("peel"), 4.0);
    this.fruit = AdditiveRegister.create(registrar.descend("fruit"), 4.0);

    registrar.constraint("FlagIsA", this.flag.is(Flag.A));
    registrar.constraint("haha", this.fruit.value.isOneOf(2.0, 2.5, 3.0, 3.5));
  }
}
