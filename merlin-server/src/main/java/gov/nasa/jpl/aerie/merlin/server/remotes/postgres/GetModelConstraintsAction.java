package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.remotes.MissionModelRepository;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/*package-local*/ final class GetModelConstraintsAction implements AutoCloseable {
  // We left join through the mission_model table in order to distinguish
  //   a mission model without any constraints from a non-existent mission model.
  // A mission model without constraints will produce a placeholder row with nulls.
  private static final @Language("SQL") String sql = """
    select c.id, c.name, c.summary, c.description, c.definition
    from mission_model AS m
    left join condition AS c
      on m.id = c.model_id
    where m.id = ?
    """;

  private final PreparedStatement statement;

  public GetModelConstraintsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<String, Constraint> get(final long modelId)
  throws SQLException, MissionModelRepository.NoSuchMissionModelException
  {
    this.statement.setLong(1, modelId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new MissionModelRepository.NoSuchMissionModelException();

      final var constraints = new HashMap<String, Constraint>();
      do {
        if (isColumnNull(results, 1)) continue;

        final var constraint = new Constraint(
          results.getString(2),
          results.getString(3),
          results.getString(4),
          results.getString(5));

        constraints.put(constraint.name(), constraint);
      } while (results.next());

      return constraints;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }

  private static boolean isColumnNull(final ResultSet results, final int index) throws SQLException {
    // You're kidding, right? This is how you detect NULL with JDBC?
    results.getObject(index);
    return results.wasNull();
  }
}
