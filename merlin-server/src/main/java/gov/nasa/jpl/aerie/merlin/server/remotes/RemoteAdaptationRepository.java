package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationJar;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.utilities.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public final class RemoteAdaptationRepository implements AdaptationRepository {
    private final Path ADAPTATION_FILE_PATH = Path.of("adaptation_files").toAbsolutePath();
    private final MongoCollection<Document> adaptationCollection;

    public RemoteAdaptationRepository(
            final MongoDatabase database,
            final String adaptationCollectionName
    ) {
        this.adaptationCollection = database.getCollection(adaptationCollectionName);
    }

    public void clear() {
        this.adaptationCollection.drop();
    }

    @Override
    public Stream<Pair<String, AdaptationJar>> getAllAdaptations() {
        final var query = this.adaptationCollection.find();

        return documentStream(query)
                .map(adaptationDocument -> {
                    final String adaptationId = adaptationDocument.getObjectId("_id").toString();
                    final AdaptationJar adaptationJar = adaptationFromDocuments(adaptationDocument);

                    return Pair.of(adaptationId, adaptationJar);
                });
    }

    @Override
    public AdaptationJar getAdaptation(final String id) throws NoSuchAdaptationException {
        final Document adaptationDocument;
        try {
            adaptationDocument = this.adaptationCollection.find(adaptationById(id)).first();
        } catch (IllegalArgumentException e) {
            throw new NoSuchAdaptationException();
        }

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException();
        }

        return adaptationFromDocuments(adaptationDocument);
    }

    @Override
    public Map<String, Constraint> getConstraints(final String id) throws NoSuchAdaptationException {
        final Document adaptationDocument;
        try {
            adaptationDocument = this.adaptationCollection.find(adaptationById(id)).first();
        } catch (IllegalArgumentException e) {
            throw new NoSuchAdaptationException();
        }

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException();
        }

        final var constraints = new HashMap<String, Constraint>();

        final var constraintsDocument = adaptationDocument.get("constraints", Document.class);
        for (final var key : constraintsDocument.keySet()) {
          final var constraintId = key;
          final var constraintDocument = constraintsDocument.get(constraintId, Document.class);
          constraints.put(constraintId, constraintFromDocument(constraintDocument));
        }

        return constraints;
    }

    @Override
    public String createAdaptation(final AdaptationJar adaptationJar) {
        // Store Adaptation JAR
        final Path location = FileUtils.getUniqueFilePath(adaptationJar, ADAPTATION_FILE_PATH);
        try {
            Files.createDirectories(location.getParent());
            Files.copy(adaptationJar.path, location);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }

        final AdaptationJar newJar = new AdaptationJar(adaptationJar);
        newJar.path = location;

        final Document adaptationDocument = this.toDocument(newJar);

        this.adaptationCollection.insertOne(adaptationDocument);

        return adaptationDocument.getObjectId("_id").toString();
    }

    @Override
    public void deleteAdaptation(final String adaptationId) throws NoSuchAdaptationException {
        final AdaptationJar adaptationJar = this.getAdaptation(adaptationId);

        // Delete adaptation JAR
        try {
            Files.deleteIfExists(adaptationJar.path);
        } catch (final IOException e) {
            throw new AdaptationAccessException(adaptationJar.path, e);
        }

        this.adaptationCollection.deleteOne(adaptationById(adaptationId));
    }

    @Override
    public void replaceConstraints(final String id, final Map<String, Constraint> newConstraints)
    throws NoSuchAdaptationException
    {
        final Document adaptationDocument;
        try {
            adaptationDocument = this.adaptationCollection.find(adaptationById(id)).first();
        } catch (IllegalArgumentException e) {
            throw new NoSuchAdaptationException();
        }

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException();
        }

        final var constraints = adaptationDocument.get("constraints", Document.class);
        for (final var entry : newConstraints.entrySet()) {
          constraints.put(entry.getKey(), this.toDocument(entry.getValue()));
        }

        adaptationDocument.put("constraints", constraints);

        this.adaptationCollection.replaceOne(adaptationById(id), adaptationDocument);
    }

    @Override
    public void deleteConstraint(final String adaptationId, final String constraintName)
    throws NoSuchAdaptationException
    {
        final Document adaptationDocument;
        try {
            adaptationDocument = this.adaptationCollection.find(adaptationById(adaptationId)).first();
        } catch (IllegalArgumentException e) {
            throw new NoSuchAdaptationException();
        }

        if (adaptationDocument == null) {
            throw new NoSuchAdaptationException();
        }

        final var constraints = adaptationDocument.get("constraints", Document.class);
        constraints.remove(constraintName);
        adaptationDocument.put("constraints", constraints);

        this.adaptationCollection.replaceOne(adaptationById(adaptationId), adaptationDocument);
    }

    private AdaptationJar adaptationFromDocuments(final Document adaptationDocument) {
        final AdaptationJar adaptationJar = new AdaptationJar();
        adaptationJar.name = adaptationDocument.getString("name");
        adaptationJar.version = adaptationDocument.getString("version");
        adaptationJar.mission = adaptationDocument.getString("mission");
        adaptationJar.owner = adaptationDocument.getString("owner");
        adaptationJar.path = Path.of(adaptationDocument.getString("path"));

        return adaptationJar;
    }

    // @TODO should this be factored out with the duplicate code in RemotePlanRespository?
    private Constraint constraintFromDocument(final Document document) {
      final Constraint constraint = new Constraint(
          document.getString("name"),
          document.getString("summary"),
          document.getString("description"),
          document.getString("definition"));

      return constraint;
    }

    private Document toDocument(final Constraint constraint) {
      final Document constraintDocument = new Document();
      constraintDocument.put("name", constraint.name());
      constraintDocument.put("summary", constraint.summary());
      constraintDocument.put("description", constraint.description());
      constraintDocument.put("definition", constraint.definition());

      return constraintDocument;
    }

    private Document toDocument(final AdaptationJar adaptationJar) {
        final Document adaptationDocument = new Document();
        adaptationDocument.put("name", adaptationJar.name);
        adaptationDocument.put("version", adaptationJar.version);
        adaptationDocument.put("mission", adaptationJar.mission);
        adaptationDocument.put("owner", adaptationJar.owner);
        adaptationDocument.put("path", adaptationJar.path.toString());
        adaptationDocument.put("constraints", new Document());

        return adaptationDocument;
    }

    private static <T> Stream<T> documentStream(final MongoIterable<T> documents) {
        // Eagerly construct a new iterator so we can close it after the stream is done.
        final MongoCursor<T> cursor = documents.iterator();
        // Wrap the fresh cursor in an Iterable so we can convert it to a Stream.
        final Iterable<T> iterable = () -> cursor;
        // Create a sequential stream that propagates closure to the cursor.
        return StreamSupport
                .stream(iterable.spliterator(), false)
                .onClose(cursor::close);
    }

    private Bson adaptationById(final String adaptationId) throws IllegalArgumentException {
        return eq("_id", new ObjectId(adaptationId));
    }

    public static class AdaptationAccessException extends RuntimeException {
        private final Path path;

        public AdaptationAccessException(final Path path, final Throwable cause) {
            super(cause);
            this.path = path;
        }

        public Path getPath() {
            return this.path;
        }
    }
}
