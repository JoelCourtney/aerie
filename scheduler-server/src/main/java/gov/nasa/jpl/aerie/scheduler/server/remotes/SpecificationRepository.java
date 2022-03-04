package gov.nasa.jpl.aerie.scheduler.server.remotes;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PostgresSpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;

public interface SpecificationRepository {
  // Queries
  Specification getSpecification(SpecificationId specificationId)
  throws NoSuchSpecificationException, PostgresSpecificationRepository.GoalBuildFailureException;
  RevisionData getSpecificationRevisionData(SpecificationId specificationId) throws NoSuchSpecificationException;
}
