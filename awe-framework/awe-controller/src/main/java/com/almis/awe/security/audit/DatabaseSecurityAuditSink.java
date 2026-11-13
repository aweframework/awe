package com.almis.awe.security.audit;

import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.security.audit.SecurityAuditEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Stores security audit events in the append-only {@value #TABLE} table, created by the AWE_V1.3.0 migration.
 * <p>
 * Each event is written on its own connection, outside any transaction of the audited operation, and committed at
 * once. The only deletion is the purge of the events older than the retention.
 */
public class DatabaseSecurityAuditSink implements SecurityAuditSink {

  /** Events table */
  public static final String TABLE = "AweSecAudEvt";

  private static final String INSERT = "INSERT INTO " + TABLE
    + " (IdeSecAud, EvtDat, EvtDay, EvtTyp, EvtRes, EvtCau, UsrNam, Ten, Chn, IpAdr, UsrAgt, SesHsh, CorIde, Det)"
    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private static final String PURGE = "DELETE FROM " + TABLE + " WHERE EvtDat < ?";

  private final DataSource dataSource;
  private final ZoneId zone;

  /**
   * Constructor (event days in the server time zone)
   *
   * @param dataSource Data source holding the events table
   */
  public DatabaseSecurityAuditSink(DataSource dataSource) {
    this(dataSource, ZoneId.systemDefault());
  }

  /**
   * Constructor
   *
   * @param dataSource Data source holding the events table
   * @param zone       Time zone of the event days
   */
  public DatabaseSecurityAuditSink(DataSource dataSource, ZoneId zone) {
    this.dataSource = dataSource;
    this.zone = zone;
  }

  @Override
  public void record(SecurityAuditEvent event) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(INSERT)) {
      statement.setString(1, event.getId());
      statement.setTimestamp(2, Timestamp.from(event.getTimestamp()));
      // Day of the event in the server time zone: day filters and daily charts compare plain dates
      statement.setDate(3, java.sql.Date.valueOf(LocalDate.ofInstant(event.getTimestamp(), zone)));
      statement.setString(4, name(event.getType()));
      statement.setString(5, name(event.getOutcome()));
      statement.setString(6, name(event.getCause()));
      statement.setString(7, event.getUsername());
      statement.setString(8, event.getTenant());
      statement.setString(9, name(event.getChannel()));
      statement.setString(10, event.getIpAddress());
      statement.setString(11, event.getUserAgent());
      statement.setString(12, event.getSessionHash());
      statement.setString(13, event.getCorrelationId());
      statement.setString(14, event.getDetail());
      statement.executeUpdate();
      commit(connection);
    } catch (SQLException exc) {
      throw new AWERuntimeException("Security audit event could not be stored in " + TABLE, exc);
    }
  }

  /**
   * Delete the events older than a moment
   *
   * @param before Events strictly older are deleted
   * @return Deleted events
   */
  public int purge(Instant before) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(PURGE)) {
      statement.setTimestamp(1, Timestamp.from(before));
      int deleted = statement.executeUpdate();
      commit(connection);
      return deleted;
    } catch (SQLException exc) {
      throw new AWERuntimeException("Expired security audit events could not be purged from " + TABLE, exc);
    }
  }

  private static void commit(Connection connection) throws SQLException {
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
