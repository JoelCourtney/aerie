package gov.nasa.jpl.aerie.merlin.server.remotes;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Disabled
@Tag("integration")
public final class MongoPlanRepositoryTest extends PlanRepositoryContractTest {
  private static final URI MONGO_URI = URI.create("mongodb://localhost:27017");
  private static final String MONGO_DATABASE = "plan-service";
  private static final String MONGO_PLAN_COLLECTION = "plans";
  private static final String MONGO_ACTIVITY_COLLECTION = "activities";

  private static final MongoDatabase mongoDatabase =
      MongoClients.create(MONGO_URI.toString()).getDatabase(MONGO_DATABASE);
  private static final MongoPlanRepository remoteRepository =
      new MongoPlanRepository(mongoDatabase, MONGO_PLAN_COLLECTION, MONGO_ACTIVITY_COLLECTION);

  @Override
  protected void resetRepository() {
    MongoPlanRepositoryTest.remoteRepository.clear();
    this.planRepository = MongoPlanRepositoryTest.remoteRepository;
  }

  @Test
  public void invalidKeyShouldThrow() {
    // GIVEN
    final String invalidObjectId = "abc";
    assertThat(ObjectId.isValid(invalidObjectId)).isFalse();

    // WHEN
    final Throwable thrown = catchThrowable(() -> this.planRepository.getPlan(invalidObjectId));

    // THEN
    assertThat(thrown).isInstanceOf(NoSuchPlanException.class);
    assertThat(((NoSuchPlanException)thrown).getInvalidPlanId()).isEqualTo(invalidObjectId);
  }
}
