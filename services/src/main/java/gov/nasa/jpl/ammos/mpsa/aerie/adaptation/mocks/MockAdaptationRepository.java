package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationAccessException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationJar;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.NewAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.remotes.AdaptationRepository;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities.FileUtils.getUniqueFilePath;

public final class MockAdaptationRepository implements AdaptationRepository {
    private final Path ADAPTATION_FILE_PATH;
    private final Map<String, AdaptationJar> adaptations = new HashMap<>();
    private int nextAdaptationId;

    public MockAdaptationRepository() {
        try {
            ADAPTATION_FILE_PATH = Files.createTempDirectory("mock_adaptation_files").toAbsolutePath();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String createAdaptation(final NewAdaptation newAdaptation) {
        final String adaptationId = Objects.toString(this.nextAdaptationId++);

        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = newAdaptation.name;
        adaptationJar.version = newAdaptation.version;
        adaptationJar.mission = newAdaptation.mission;
        adaptationJar.owner = newAdaptation.owner;

        // Store Adaptation JAR
        final Path location = getUniqueFilePath(adaptationJar, ADAPTATION_FILE_PATH);
        try {
            Files.copy(newAdaptation.path, location);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }
        adaptationJar.path = location;

        this.adaptations.put(adaptationId, adaptationJar);
        return adaptationId;
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptationJar = getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptationJar.path);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }

        this.adaptations.remove(adaptationId);
    }

    @Override
    public AdaptationJar getAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptation = Optional
                .ofNullable(this.adaptations.get(adaptationId))
                .orElseThrow(() -> new NoSuchAdaptationException(adaptationId));

        return new AdaptationJar(adaptation);
    }

    @Override
    public Stream<Pair<String, AdaptationJar>> getAllAdaptations() {
        return this.adaptations
                .entrySet()
                .stream()
                .map(entry -> Pair.of(entry.getKey(), new AdaptationJar(entry.getValue())));
    }
}
