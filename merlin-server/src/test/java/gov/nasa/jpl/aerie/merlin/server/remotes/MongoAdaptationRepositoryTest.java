package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import java.net.URI;
import java.nio.file.Path;

@Disabled
@Tag("integration")
public final class MongoAdaptationRepositoryTest extends AdaptationRepositoryContractTest {
    private static final Path ADAPTATION_FILES_PATH = Path.of("/dev/null");
    private static final URI MONGO_URI = URI.create("mongodb://localhost:27019");
    private static final String MONGO_DATABASE = "adaptation-service";
    private static final String MONGO_ADAPTATION_COLLECTION = "adaptations";

    private static final MongoAdaptationRepository remoteRepository = new MongoAdaptationRepository(
        ADAPTATION_FILES_PATH,
        MongoClients
            .create(MONGO_URI.toString())
            .getDatabase(MONGO_DATABASE),
        MONGO_ADAPTATION_COLLECTION);

    @Override
    protected void resetRepository() {
        MongoAdaptationRepositoryTest.remoteRepository.clear();
        this.adaptationRepository = MongoAdaptationRepositoryTest.remoteRepository;
    }
}
