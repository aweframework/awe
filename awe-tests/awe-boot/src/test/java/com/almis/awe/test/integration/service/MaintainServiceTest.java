package com.almis.awe.test.integration.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.factory.WithMockCustomUser;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.service.MaintainService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("integration")
@DisplayName("Maintain service Tests")
@WithMockCustomUser(username = "test", password = "test")
@Transactional
class MaintainServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private MaintainService maintainService;

  @Mock
  private AweSession aweSession;

  @BeforeEach
  public void setSessionDatabase() {
    given(aweSession.getParameter(String.class, "database")).willReturn("testDatabase");
  }

  /**
   * Test of maintain not defined.
   */
  @Test
  void testMaintainNotDefined() {
    assertThrows(AWException.class, () -> maintainService.launchMaintain("MaintainNotDefined"));
  }

  /**
   * Test of maintain over a null alias (should launch over default database)
   *
   * @throws AWException Test error
   */
  @Test
  void testMaintain() throws Exception {
    MaintainService mock = spy(maintainService);
    mock.launchMaintain("SimpleSingleInsertFromVariableValue");
    verify(mock, times(1)).getDatabaseConnection(any(ObjectNode.class));
  }

  /**
   * Test of maintain over a null alias (should launch over default database)
   *
   * @throws AWException Test error
   */
  @Test
  void testMaintainWithValidAlias() throws Exception {
    MaintainService mock = spy(maintainService);
    doReturn(maintainService.getDatabaseConnection()).when(mock).getDatabaseConnection(anyString());
    mock.launchMaintain("SimpleSingleInsertFromVariableValue", "aweora1");
    verify(mock, times(1)).getDatabaseConnection("aweora1");
  }

  /**
   * Test of maintain over a null alias (should launch over default database)
   *
   * @throws AWException Test error
   */
  @Test
  void testPrivateMaintain() throws Exception {
    MaintainService mock = spy(maintainService);
    mock.launchPrivateMaintain("SimpleSingleInsertFromVariableValue");
    verify(mock, times(1)).getDatabaseConnection(any(ObjectNode.class));
  }

  /**
   * Test of maintain over a null alias (should launch over default database)
   *
   * @throws AWException Test error
   */
  @Test
  void testPrivateMaintainWithValidAlias() throws Exception {
    MaintainService mock = spy(maintainService);
    doReturn(maintainService.getDatabaseConnection()).when(mock).getDatabaseConnection(anyString());
    mock.launchPrivateMaintain("SimpleSingleInsertFromVariableValue", "aweora1");
    verify(mock, times(1)).getDatabaseConnection("aweora1");
  }

}
