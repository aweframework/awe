package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditChannel;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseSecurityAuditSinkTest {

  private JDBCDataSource dataSource;
  private DatabaseSecurityAuditSink sink;

  @BeforeEach
  void setUp() throws SQLException {
    dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:audit-" + UUID.randomUUID());
    dataSource.setUser("sa");
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("""
        CREATE TABLE AweSecAudEvt (
          IdeSecAud varchar(36) NOT NULL PRIMARY KEY, EvtDat timestamp NOT NULL, EvtDay date NOT NULL, EvtTyp varchar(40) NOT NULL,
          EvtRes varchar(10) NOT NULL, EvtCau varchar(40), UsrNam varchar(255), Ten varchar(100), Chn varchar(10),
          IpAdr varchar(45), UsrAgt varchar(512), SesHsh varchar(64), CorIde varchar(64), Det varchar(1000))""");
    }
    sink = new DatabaseSecurityAuditSink(dataSource, ZoneId.of("Europe/Madrid"));
  }

  @Test
  void storesEveryField() throws SQLException {
    sink.record(event("e-1", Instant.parse("2026-10-16T10:15:30Z")));

    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
         ResultSet row = statement.executeQuery("SELECT * FROM AweSecAudEvt")) {
      assertThat(row.next()).isTrue();
      assertThat(row.getString("IdeSecAud")).isEqualTo("e-1");
      assertThat(row.getTimestamp("EvtDat")).isEqualTo(Timestamp.from(Instant.parse("2026-10-16T10:15:30Z")));
      assertThat(row.getDate("EvtDay")).as("day in the sink time zone").isEqualTo(java.sql.Date.valueOf("2026-10-16"));
      assertThat(row.getString("EvtTyp")).isEqualTo("LOGIN");
      assertThat(row.getString("EvtRes")).isEqualTo("FAILURE");
      assertThat(row.getString("EvtCau")).isEqualTo("BAD_CREDENTIALS");
      assertThat(row.getString("UsrNam")).isEqualTo("jdoe");
      assertThat(row.getString("Ten")).isEqualTo("tenant-a");
      assertThat(row.getString("Chn")).isEqualTo("WEB");
      assertThat(row.getString("IpAdr")).isEqualTo("10.0.0.1");
      assertThat(row.getString("UsrAgt")).isEqualTo("agent");
      assertThat(row.getString("SesHsh")).isEqualTo("hash");
      assertThat(row.getString("CorIde")).isEqualTo("corr");
      assertThat(row.getString("Det")).isEqualTo("detail");
      assertThat(row.next()).isFalse();
    }
  }

  @Test
  void storesNullOptionalFields() throws SQLException {
    sink.record(SecurityAuditEvent.builder().id("e-2").timestamp(Instant.now())
      .type(SecurityAuditEventType.LOGOUT).outcome(SecurityAuditOutcome.SUCCESS).build());

    assertThat(count()).isEqualTo(1);
  }

  @Test
  void purgeDeletesOnlyEventsOlderThanTheLimit() throws SQLException {
    sink.record(event("old", Instant.parse("2025-01-01T00:00:00Z")));
    sink.record(event("new", Instant.parse("2026-10-16T00:00:00Z")));

    int deleted = sink.purge(Instant.parse("2026-01-01T00:00:00Z"));

    assertThat(deleted).isEqualTo(1);
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void databaseErrorsAreReported() throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE AweSecAudEvt");
    }

    assertThatThrownBy(() -> sink.record(event("e-3", Instant.now()))).isInstanceOf(RuntimeException.class);
  }

  private SecurityAuditEvent event(String id, Instant timestamp) {
    return SecurityAuditEvent.builder().id(id).timestamp(timestamp)
      .type(SecurityAuditEventType.LOGIN).outcome(SecurityAuditOutcome.FAILURE).cause(SecurityAuditCause.BAD_CREDENTIALS)
      .username("jdoe").tenant("tenant-a").channel(SecurityAuditChannel.WEB).ipAddress("10.0.0.1")
      .userAgent("agent").sessionHash("hash").correlationId("corr").detail("detail").build();
  }

  private int count() throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
         ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM AweSecAudEvt")) {
      row.next();
      return row.getInt(1);
    }
  }
}
