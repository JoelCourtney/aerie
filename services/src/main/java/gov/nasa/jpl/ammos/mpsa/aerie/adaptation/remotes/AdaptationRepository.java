package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.*;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Stream;

public interface AdaptationRepository {
    // Queries
    Stream<Pair<String, AdaptationJar>> getAllAdaptations();
    AdaptationJar getAdaptation(String id) throws NoSuchAdaptationException;

    // Mutations
    String createAdaptation(NewAdaptation adaptation);
    void deleteAdaptation(String adaptationId) throws NoSuchAdaptationException;
}
