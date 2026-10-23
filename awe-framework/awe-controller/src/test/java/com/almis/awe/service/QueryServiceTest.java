package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
  void launchQueryActionWithoutDataListAddsNullDataVariables() throws Exception {
    when(aweRequest.getTargetAction()).thenReturn("target");
    when(aweElements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    when(queryUtil.getParameters(null, null, null)).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData out = queryService.launchQueryAction();

    assertThat(out.getVariableMap().get(AweConstants.ACTION_DATA)).isEqualTo(new CellData());
    assertThat(out.getVariableMap().get(AweConstants.ACTION_ROWS)).isEqualTo(new CellData());
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

  /**
   * A grid inside a row detail is created once per open row with its own identifier (GrdOrders-7). Its data requests are
   * launched by their target action, never look the component up in the screen, and send the parent row values as
   * <parent grid>.row.<column>, which the query variables read by that name
   */
  @Test
  void launchQueryActionOfARowDetailInstanceReadsTheParentRow() throws Exception {
    QueryUtil realQueryUtil = new QueryUtil(new BaseConfigProperties(), new DatabaseConfigProperties(), new JsonMapper(), new PrototypeRequestBeanHolder());
    Query query = new Query().setIsPublic(true);
    query.setVariableDefinitionList(List.of(Variable.builder().id("productId").type("INTEGER").name("GrdProducts.row.id").build(),
      Variable.builder().id("productName").type("STRING").name("GrdProducts.row.name").build()));
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("GrdProducts.row.id", 7).put("GrdProducts.row.name", "Pen")
      .put(AweConstants.COMPONENT_MAX, 5);
    parameters.set("GrdProducts.id", JsonNodeFactory.instance.arrayNode().add(1).add(7));
    parameters.set(AweConstants.PARAMETER_ADDRESS, JsonNodeFactory.instance.objectNode().put("view", "base").put("component", "GrdOrders-7"));
    when(aweRequest.getTargetAction()).thenReturn("QryOrders");
    when(aweElements.getQuery("QryOrders")).thenReturn(query);
    when(queryUtil.getParameters(null, null, null)).thenReturn(parameters);
    Map<String, Map<String, QueryParameter>> resolved = new HashMap<>();
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class))).thenAnswer(invocation -> {
      resolved.put("variables", realQueryUtil.getVariableMap((Query) invocation.getArgument(0), (ObjectNode) invocation.getArgument(1)));
      return new ServiceData().setDataList(new DataList());
    });

    assertEquals(AnswerType.OK, queryService.launchQueryAction().getType());

    verify(aweElements, never()).getScreen(anyString());
    Map<String, QueryParameter> variables = resolved.get("variables");
    assertEquals(7, variables.get("productId").getValue().asInt());
    assertThat(variables.get("productId").isList()).isFalse();
    assertEquals("Pen", variables.get("productName").getValue().asString());
  }
}
