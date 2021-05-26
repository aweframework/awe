package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Query service tests
 */
@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

  @InjectMocks
  private QueryService queryService;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private QueryLauncher queryLauncher;

  @Mock
  private QueryUtil queryUtil;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    queryService.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    doReturn(aweRequest).when(context).getBean(AweRequest.class);
  }

  @Test
  void launchQueryAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.launchQueryAction().getType());
  }

  @Test
  void launchQueryActionQueryNotFound() {
    when(aweRequest.getTargetAction()).thenReturn("target");
    assertThrows(AWException.class, () -> queryService.launchQueryAction());
  }

  @Test
  void launchQueryActionQueryExceptionWithMessage() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenThrow(new BadCredentialsException("Error"));
    assertThrows(BadCredentialsException.class, () -> queryService.launchQueryAction());
  }

  @Test
  void launchQueryActionNullTarget() {
    when(aweRequest.getTargetAction()).thenReturn(null);
    assertThrows(AWException.class, () -> queryService.launchQueryAction());
  }

  @Test
  void updateModelAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.updateModelAction().getType());
  }

  @Test
  void updateModelActionNullTarget() {
    when(aweRequest.getTargetAction()).thenReturn(null);
    assertThrows(AWException.class, () -> queryService.updateModelAction());
  }

  @Test
  void checkUniqueAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.checkUniqueAction().getType());
  }

  @Test
  void checkUniqueActionNotEmpty() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(1L)));
    assertThrows(AWException.class, () -> queryService.checkUniqueAction());
  }

  @Test
  void checkUniqueActionNullTarget() {
    when(aweRequest.getTargetAction()).thenReturn(null);
    assertThrows(AWException.class, () -> queryService.checkUniqueAction());
  }

  @Test
  void checkEmptyAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(1L)));
    assertEquals(AnswerType.OK, queryService.checkEmptyAction().getType());
  }

  @Test
  void checkEmptyActionNoLines() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(0L)));
    assertThrows(AWException.class, () -> queryService.checkEmptyAction());
  }

  @Test
  void checkEmptyActionNullTarget() {
    when(aweRequest.getTargetAction()).thenReturn(null);
    assertThrows(AWException.class, () -> queryService.checkEmptyAction());
  }

  @Test
  void subscribeAction() throws Exception {
    ObjectNode address = JsonNodeFactory.instance.objectNode();
    address.put("view", "base");
    address.put("component", "component");
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweRequest.getParameter("address")).thenReturn(address);
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.subscribe(any(Query.class), any(ComponentAddress.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.subscribeAction().getType());
  }

  @Test
  void subscribeActionNullTarget() {
    when(aweRequest.getTargetAction()).thenReturn(null);
    assertThrows(AWException.class, () -> queryService.subscribeAction());
  }
}