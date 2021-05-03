package com.almis.awe.test.unit.services;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryServiceTest {

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
  @Before
  public void initBeans() throws Exception {
    queryService.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    doReturn(aweRequest).when(context).getBean(AweRequest.class);
  }

  @Test
  public void launchQueryAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.launchQueryAction().getType());
  }

  @Test(expected = AWException.class)
  public void launchQueryActionQueryNotFound() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    queryService.launchQueryAction();
  }

  @Test(expected = BadCredentialsException.class)
  public void launchQueryActionQueryExceptionWithMessage() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenThrow(new BadCredentialsException("Error"));
    queryService.launchQueryAction();
  }

  @Test(expected = AWException.class)
  public void launchQueryActionNullTarget() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn(null);
    queryService.launchQueryAction();
  }

  @Test
  public void updateModelAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.updateModelAction().getType());
  }

  @Test(expected = AWException.class)
  public void updateModelActionNullTarget() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn(null);
    queryService.updateModelAction();
  }

  @Test
  public void checkUniqueAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    assertEquals(AnswerType.OK, queryService.checkUniqueAction().getType());
  }

  @Test(expected = AWException.class)
  public void checkUniqueActionNotEmpty() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(1L)));
    queryService.checkUniqueAction();
  }

  @Test(expected = AWException.class)
  public void checkUniqueActionNullTarget() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn(null);
    queryService.checkUniqueAction();
  }

  @Test
  public void checkEmptyAction() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(1L)));
    assertEquals(AnswerType.OK, queryService.checkEmptyAction().getType());
  }

  @Test(expected = AWException.class)
  public void checkEmptyActionNoLines() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList().setRecords(0L)));
    queryService.checkEmptyAction();
  }

  @Test(expected = AWException.class)
  public void checkEmptyActionNullTarget() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn(null);
    queryService.checkEmptyAction();
  }

  @Test
  public void subscribeAction() throws Exception {
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

  @Test(expected = AWException.class)
  public void subscribeActionNullTarget() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn(null);
    queryService.subscribeAction();
  }
}