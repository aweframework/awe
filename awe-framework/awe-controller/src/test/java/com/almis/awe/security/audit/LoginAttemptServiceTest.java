package com.almis.awe.security.audit;

import com.almis.awe.config.LockoutConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

  private final LockoutConfigProperties properties = new LockoutConfigProperties();
  private final QueryService queryService = mock(QueryService.class);
  private final MaintainService maintainService = mock(MaintainService.class);
  private final LoginAttemptService service = new LoginAttemptService(properties, queryService, maintainService);

  @BeforeEach
  void setUp() {
    properties.setMaxAttempts(3);
  }

  @Test
  void switchedOffByDefault() throws AWException {
    LoginAttemptService defaults = new LoginAttemptService(new LockoutConfigProperties(), queryService, maintainService);

    assertThat(defaults.isEnabled()).isFalse();
    assertThat(defaults.registerFailure("jdoe")).isFalse();
    defaults.registerSuccess("jdoe");
    verifyNoInteractions(queryService, maintainService);
  }

  @Test
  void aFailureBelowTheLimitIsCounted() throws AWException {
    stubAttempts(1);

    assertThat(service.registerFailure("jdoe")).isFalse();

    ObjectNode parameters = maintainParameters(LoginAttemptService.UPDATE_ATTEMPTS_MAINTAIN);
    assertThat(parameters.get("Usr").asString()).isEqualTo("jdoe");
    assertThat(parameters.get(LoginAttemptService.UPDATE_ATTEMPTS_MAINTAIN).asInt()).isEqualTo(2);
    verify(maintainService, never()).launchPrivateMaintain(eq(LoginAttemptService.UPDATE_LOCK_MAINTAIN), any(ObjectNode.class));
  }

  @Test
  void theFailureReachingTheLimitLocksTheAccount() throws AWException {
    stubAttempts(2);

    assertThat(service.registerFailure("jdoe")).isTrue();

    ObjectNode lock = maintainParameters(LoginAttemptService.UPDATE_LOCK_MAINTAIN);
    assertThat(lock.get("Usr").asString()).isEqualTo("jdoe");
    assertThat(lock.get("PwdLck").asInt()).isEqualTo(1);
  }

  @Test
  void unknownUsersAreNotCounted() throws AWException {
    when(queryService.launchPrivateQuery(eq(LoginAttemptService.FAILED_ATTEMPTS_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    assertThat(service.registerFailure("ghost")).isFalse();
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void aSuccessResetsOnlyPendingFailures() throws AWException {
    stubAttempts(0);
    service.registerSuccess("jdoe");
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));

    stubAttempts(2);
    service.registerSuccess("jdoe");
    ObjectNode parameters = maintainParameters(LoginAttemptService.UPDATE_ATTEMPTS_MAINTAIN);
    assertThat(parameters.get(LoginAttemptService.UPDATE_ATTEMPTS_MAINTAIN).asInt()).isZero();
  }

  private void stubAttempts(int attempts) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    row.put("value", new CellData(attempts));
    DataList dataList = new DataList();
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(eq(LoginAttemptService.FAILED_ATTEMPTS_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(dataList));
  }

  private ObjectNode maintainParameters(String maintain) throws AWException {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(1)).launchPrivateMaintain(eq(maintain), captor.capture());
    return captor.getValue();
  }
}
