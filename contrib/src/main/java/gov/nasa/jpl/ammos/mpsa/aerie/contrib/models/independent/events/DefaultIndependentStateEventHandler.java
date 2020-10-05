package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

public interface DefaultIndependentStateEventHandler<Result> extends IndependentStateEventHandler<Result> {
    Result unhandled();

    @Override
    default Result add(final String binName, final double amount) {
        return this.unhandled();
    }

    @Override
    default Result set(final String binName, final SerializedValue value) {
        return this.unhandled();
    }
}
