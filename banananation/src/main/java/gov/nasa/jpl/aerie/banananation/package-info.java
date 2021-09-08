@Adaptation(model = Mission.class)

@WithMappers(BasicValueMappers.class)
@WithMappers(BananaValueMappers.class)

@WithConfiguration(Configuration.class)

@WithActivityType(BiteBananaActivity.class)
@WithActivityType(PeelBananaActivity.class)
@WithActivityType(ParameterTestActivity.class)
@WithActivityType(PickBananaActivity.class)
@WithActivityType(ChangeProducerActivity.class)
@WithActivityType(ThrowBananaActivity.class)
@WithActivityType(GrowBananaActivity.class)
@WithActivityType(LineCountBananaActivity.class)
@WithActivityType(DecomposingActivity.ParentActivity.class)
@WithActivityType(DecomposingActivity.ChildActivity.class)

package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.GrowBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.DecomposingActivity;
import gov.nasa.jpl.aerie.banananation.activities.LineCountBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.PickBananaActivity;
import gov.nasa.jpl.aerie.banananation.activities.ChangeProducerActivity;
import gov.nasa.jpl.aerie.banananation.activities.ThrowBananaActivity;
import gov.nasa.jpl.aerie.contrib.serialization.rulesets.BasicValueMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithMappers;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation.WithConfiguration;
