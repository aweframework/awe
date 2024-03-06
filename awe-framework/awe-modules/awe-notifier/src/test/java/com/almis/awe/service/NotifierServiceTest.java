package com.almis.awe.service;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.notifier.dto.InterestedUsersDto;
import com.almis.awe.notifier.dto.NotificationDto;
import com.almis.awe.notifier.service.NotifierService;
import com.almis.awe.notifier.type.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifierServiceTest {

  @InjectMocks
  private NotifierService notifierService;

  @Mock
  private QueryService queryService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private BroadcastService broadcastService;

  @Mock
  private ObjectMapper mapper;

  @Test
  void getNotifications() throws Exception {
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(new DataList()));

    // Run
    notifierService.getNotifications();

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString());
  }

  @Test
  void toggleWebSubscriptionInit() throws Exception {
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(new DataList()));

    // Run
    notifierService.toggleWebSubscription("test", 1);

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString());
    verify(maintainService, times(1)).launchMaintain(eq("insert-user-subscription"), any(ObjectNode.class));
  }

  @Test
  void toggleWebSubscriptionUpdate() throws Exception {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("web", new CellData("1"));
    row.put("email", new CellData("1"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));

    // Run
    notifierService.toggleWebSubscription("test", 1);

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString());
    verify(maintainService, times(1)).launchMaintain(eq("update-user-subscription"), any(ObjectNode.class));
  }

  @Test
  void toggleEmailSubscriptionInit() throws Exception {
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(new DataList()));

    // Run
    notifierService.toggleEmailSubscription("test", 1);

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString());
    verify(maintainService, times(1)).launchMaintain(eq("insert-user-subscription"), any(ObjectNode.class));
  }

  @Test
  void toggleEmailSubscriptionUpdate() throws Exception {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("web", new CellData("1"));
    row.put("email", new CellData("1"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));

    // Run
    notifierService.toggleEmailSubscription("test", 1);

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString());
    verify(maintainService, times(1)).launchMaintain(eq("update-user-subscription"), any(ObjectNode.class));
  }

  @Test
  void goToNotificationScreen() throws Exception {
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(
      DataListUtil.fromBeanList(List.of(new NotificationDto().setScreen("screen")))
    ));

    // Run
    ServiceData result = notifierService.goToNotificationScreen(1);

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString(), any(ObjectNode.class));
    assertEquals(1, result.getClientActionList().size());
    assertNotNull(result.getClientActionList().get(0));
  }

  @Test
  void testNotify() throws Exception {
    when(mapper.valueToTree(any())).thenReturn(JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.arrayNode().add("tutut"));
    List<InterestedUsersDto> interestedUsersDtoList = Arrays.asList(
            new InterestedUsersDto().setUser("User1").setByEmail(true).setByWeb(false),
            new InterestedUsersDto().setUser("User2").setByEmail(false).setByWeb(false),
            new InterestedUsersDto().setUser("User3").setByEmail(true).setByWeb(true),
            new InterestedUsersDto().setUser("User4").setByEmail(false).setByWeb(true)
    );
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(
      DataListUtil.fromBeanList(interestedUsersDtoList)
    ));

    // Run
    notifierService.notify(new NotificationDto().setType(NotificationType.NORMAL));

    // Assert
    verify(queryService, times(1)).launchPrivateQuery(anyString(), any(ObjectNode.class));
    verify(broadcastService, times(1)).broadcastMessageToUsers(any(ClientAction.class), anyString(), anyString());
    verify(maintainService, times(1)).launchPrivateMaintain(eq("new-notification"), any(ObjectNode.class));
    verify(maintainService, times(1)).launchPrivateMaintain(eq("notify-email-users"), any(ObjectNode.class));
  }
}
