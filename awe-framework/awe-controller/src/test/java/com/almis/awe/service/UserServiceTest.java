package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private QueryService queryService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @BeforeEach
  void setUp() {
    userService.setApplicationContext(applicationContext);
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
  }

  @Test
  void changeUserMode() throws Exception {
    // Mock
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // Do
    ServiceData serviceData = userService.changeUserMode("user", "mode");

    // Verify
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(maintainService, times(1)).launchPrivateMaintain(eq("addUserSettings"), any(ObjectNode.class));
  }

  @Test
  void updateUserMode() throws Exception {
    // Mock
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "test", "test");
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // Do
    ServiceData serviceData = userService.changeUserMode("user", "mode");

    // Verify
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(maintainService, times(1)).launchPrivateMaintain(eq("updateUserMode"), any(ObjectNode.class));
  }

  @Test
  void changeUserDensityAddsTheUserSettings() throws Exception {
    // Mock
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // Do
    ServiceData serviceData = userService.changeUserDensity("user", "Compact");

    // Verify
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(queryService).launchPrivateQuery(eq("getUserDensity"), any(ObjectNode.class));
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(1)).launchPrivateMaintain(eq("addUserSettings"), parameters.capture());
    assertThat(parameters.getValue().get("user").asText()).isEqualTo("user");
    assertThat(parameters.getValue().get("density").asText()).isEqualTo("compact");
    verify(aweSession).setParameter("density", "compact");
  }

  @Test
  void changeUserDensityUpdatesTheUserSettings() throws Exception {
    // Mock
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "value", "compact");
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // Do
    ServiceData serviceData = userService.changeUserDensity("user", "comfortable");

    // Verify
    assertEquals(AnswerType.OK, serviceData.getType());
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(1)).launchPrivateMaintain(eq("updateUserDensity"), parameters.capture());
    assertThat(parameters.getValue().get("density").asText()).isEqualTo("comfortable");
    verify(aweSession).setParameter("density", "comfortable");
  }

  @Test
  void changeUserDensityRejectsAnInvalidDensity() {
    assertThatThrownBy(() -> userService.changeUserDensity("user", "cozy")).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> userService.changeUserDensity("user", null)).isInstanceOf(AWException.class);

    // Nothing is read or written and the session keeps its density
    verifyNoInteractions(queryService, maintainService, aweSession);
  }
}
