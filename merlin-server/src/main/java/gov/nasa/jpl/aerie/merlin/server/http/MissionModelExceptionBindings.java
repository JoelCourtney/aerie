package gov.nasa.jpl.aerie.merlin.server.http;

import gov.nasa.jpl.aerie.merlin.server.models.MissionModelFacade;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;

public final class MissionModelExceptionBindings implements Plugin {
    @Override
    public void apply(final Javalin javalin) {
        javalin.exception(MissionModelFacade.MissionModelContractException.class, (ex, ctx) -> ctx
            .status(500)
            .result(ResponseSerializers.serializeMissionModelContractException(ex).toString())
            .contentType("application/json"));
    }
}
