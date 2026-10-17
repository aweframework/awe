package com.almis.awe.security.audit;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountAuditServiceTest {

  private final RecordingSecurityAudit audit = new RecordingSecurityAudit();
  private final QueryService queryService = mock(QueryService.class);
  private final LoginAttemptService attempts = mock(LoginAttemptService.class);
  private final UserAccountAuditService service = new UserAccountAuditService(audit.service(), queryService, attempts);

  @Test
  void unlockingAUserIsAuditedAndClearsItsFailures() throws AWException {
    stubUsers(user("jdoe  ", "1"));

    service.auditUserUpdate(7, 0, 0);

    assertThat(audit.events()).extracting("type", "cause", "username")
      .containsExactly(tuple(SecurityAuditEventType.ACCOUNT_UNLOCKED, SecurityAuditCause.ADMINISTRATOR, "jdoe"));
    verify(attempts).resetFailures("jdoe");
  }

  @Test
  void lockingAUserIsAudited() throws AWException {
    stubUsers(user("jdoe", "0"));

    service.auditUserUpdate(7, 1, 0);

    assertThat(audit.events()).extracting("type", "cause")
      .containsExactly(tuple(SecurityAuditEventType.ACCOUNT_LOCKED, SecurityAuditCause.ADMINISTRATOR));
    verify(attempts, never()).resetFailures(any());
  }

  @Test
  void aPasswordResetIsAuditedAndUnlocks() throws AWException {
    stubUsers(user("jdoe", "1"));

    service.auditUserUpdate(7, 1, 1);

    assertThat(audit.events()).extracting("type").containsExactly(SecurityAuditEventType.ACCOUNT_UNLOCKED, SecurityAuditEventType.PASSWORD_RESET);
  }

  @Test
  void anUpdateThatDoesNotTouchTheLockIsNotAudited() throws AWException {
    stubUsers(user("jdoe", "0"));

    service.auditUserUpdate(7, 0, 0);

    assertThat(audit.events()).isEmpty();
  }

  @Test
  void passwordChangesAreCheckedAgainstTheStoredPassword() throws AWException {
    when(queryService.launchPrivateQuery(eq(UserAccountAuditService.PASSWORD_CHECK_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(dataList(user("jdoe", "0"))))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    service.auditPasswordChange("jdoe", "new");
    service.auditPasswordChange("jdoe", "new");

    assertThat(audit.events()).extracting("type", "outcome", "cause").containsExactly(
      tuple(SecurityAuditEventType.PASSWORD_CHANGE, SecurityAuditOutcome.SUCCESS, null),
      tuple(SecurityAuditEventType.PASSWORD_CHANGE, SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS));
  }

  @Test
  void secondFactorResetsAreAuditedPerUser() throws AWException {
    stubUsers(user("ann", "0"), user("bob", "0"));

    service.auditTotpReset(List.of(1, 2));

    assertThat(audit.events()).extracting("type", "username")
      .containsExactly(tuple(SecurityAuditEventType.TOTP_RESET, "ann"), tuple(SecurityAuditEventType.TOTP_RESET, "bob"));
  }

  private void stubUsers(Map<String, CellData>... rows) throws AWException {
    when(queryService.launchPrivateQuery(eq(UserAccountAuditService.USER_STATUS_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(dataList(rows)));
  }

  @SafeVarargs
  private static DataList dataList(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(row);
    }
    return dataList;
  }

  private static Map<String, CellData> user(String name, String locked) {
    Map<String, CellData> row = new HashMap<>();
    row.put("user", new CellData(name));
    row.put("locked", new CellData(locked));
    return row;
  }
}
