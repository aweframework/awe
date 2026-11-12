package com.almis.awe.samples.helpdesk.chat;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The public chat creates a ticket for a visitor identified by email: it finds or creates the requester, creates the
 * ticket with the engine targets and returns the code and the access key of the public status check
 */
class ChatTicketServiceTest {

  private static final String PROBLEM = "The guest Wi-Fi asks for a password I do not have";

  private final QueryService queryService = mock(QueryService.class);
  private final MaintainService maintainService = mock(MaintainService.class);
  private final Map<String, Object> sessionStore = new HashMap<>();
  private ChatTicketService service;

  @BeforeEach
  void setUp() throws AWException {
    AweSession session = mock(AweSession.class);
    when(session.getParameter(anyString())).thenAnswer(call -> sessionStore.get(call.<String>getArgument(0)));
    org.mockito.Mockito.doAnswer(call -> sessionStore.put(call.getArgument(0), call.getArgument(1)))
      .when(session).setParameter(anyString(), any());
    service = new ChatTicketService(queryService, maintainService, () -> session);

    when(queryService.launchQuery(eq("HdkCatPub"), any(ObjectNode.class), eq(false)))
      .thenReturn(rows("value", List.of("NETWORK", "SOFTWARE", "OTHER")));
    when(queryService.launchQuery(eq("HdkChatTktKey"), any(ObjectNode.class), eq(false)))
      .thenReturn(rows("Cod", List.of("HD-601"), "AccKey", List.of("K7M2PQ9X")));
    when(maintainService.getNextSequenceValue("TktKey")).thenReturn(601);
    when(maintainService.getNextSequenceValue("ReqKey")).thenReturn(41);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
  }

  private static ServiceData rows(String column, List<String> values) {
    DataList list = new DataList();
    DataListUtil.addColumn(list, column, values);
    list.setRecords(values.size());
    return new ServiceData().setDataList(list);
  }

  private static ServiceData rows(String first, List<String> firstValues, String second, List<String> secondValues) {
    DataList list = new DataList();
    DataListUtil.addColumn(list, first, firstValues);
    DataListUtil.addColumn(list, second, secondValues);
    list.setRecords(firstValues.size());
    return new ServiceData().setDataList(list);
  }

  private void requesterExists() throws AWException {
    when(queryService.launchQuery(eq("HdkChatReqByEml"), any(ObjectNode.class), eq(false)))
      .thenReturn(rows("IdeReq", List.of("7")));
  }

  private void requesterIsNew() throws AWException {
    when(queryService.launchQuery(eq("HdkChatReqByEml"), any(ObjectNode.class), eq(false)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
  }

  @Test
  void createsTheRequesterAndTheTicketAndReturnsTheAccessKey() throws AWException {
    requesterIsNew();

    ServiceData result = service.createTicket("", "Ana.Torres@Mail.Example", "NETWORK", PROBLEM);

    assertThat(result.getType()).isEqualTo(AnswerType.OK);
    assertThat(result.getTitle()).isEqualTo("HD-601");
    assertThat(result.getMessage()).isEqualTo("K7M2PQ9X");
    var requester = forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("HdkChatReqNew"), requester.capture());
    assertThat(requester.getValue().get("IdeReq").asInt()).isEqualTo(41);
    assertThat(requester.getValue().get("Eml").asString()).isEqualTo("ana.torres@mail.example");
    assertThat(requester.getValue().get("Nam").asString()).isEqualTo("ana.torres");
    var ticket = forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("HdkChatTktNew"), ticket.capture());
    assertThat(ticket.getValue().get("NewTkt").asInt()).isEqualTo(601);
    assertThat(ticket.getValue().get("IdeReq").asInt()).isEqualTo(41);
    assertThat(ticket.getValue().get("Cat").asString()).isEqualTo("NETWORK");
    assertThat(ticket.getValue().get("Sub").asString()).isEqualTo(PROBLEM);
    assertThat(sessionStore.get(ChatTicketService.SESSION_COUNTER)).isEqualTo(1);
  }

  @Test
  void reusesTheRequesterOfTheEmail() throws AWException {
    requesterExists();

    ServiceData result = service.createTicket("", "ana@mail.example", "SOFTWARE", PROBLEM);

    assertThat(result.getTitle()).isEqualTo("HD-601");
    verify(maintainService, never()).launchPrivateMaintain(eq("HdkChatReqNew"), any(ObjectNode.class));
    var ticket = forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("HdkChatTktNew"), ticket.capture());
    assertThat(ticket.getValue().get("IdeReq").asInt()).isEqualTo(7);
  }

  @Test
  void aFilledHoneypotAnswersAsReceivedAndCreatesNothing() throws AWException {
    ServiceData result = service.createTicket("http://spam.example", "ana@mail.example", "NETWORK", PROBLEM);

    assertThat(result.getType()).isEqualTo(AnswerType.OK);
    assertThat(result.getTitle()).isEqualTo("RECEIVED");
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
    assertThat(sessionStore).isEmpty();
  }

  @Test
  void stopsAfterThreeTicketsInTheSession() throws AWException {
    requesterExists();
    for (int index = 0; index < 3; index++) {
      assertThat(service.createTicket("", "ana@mail.example", "NETWORK", PROBLEM).getTitle()).isEqualTo("HD-601");
    }

    ServiceData fourth = service.createTicket("", "ana@mail.example", "NETWORK", PROBLEM);

    assertThat(fourth.getType()).isEqualTo(AnswerType.WARNING);
    assertThat(fourth.getTitle()).isEqualTo("LIMIT");
    verify(maintainService, org.mockito.Mockito.times(3)).launchPrivateMaintain(eq("HdkChatTktNew"), any(ObjectNode.class));
  }

  @Test
  void answersTheFirstInvalidFieldAndCreatesNothing() throws AWException {
    assertThat(service.createTicket("", "nonsense", "NETWORK", PROBLEM)).extracting(ServiceData::getType, ServiceData::getTitle)
      .containsExactly(AnswerType.WARNING, "EMAIL");
    assertThat(service.createTicket("", "ana@mail.example", "BILLING", PROBLEM)).extracting(ServiceData::getType, ServiceData::getTitle)
      .containsExactly(AnswerType.WARNING, "CATEGORY");
    assertThat(service.createTicket("", "ana@mail.example", "NETWORK", "hi")).extracting(ServiceData::getType, ServiceData::getTitle)
      .containsExactly(AnswerType.WARNING, "DESCRIPTION");
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
    assertThat(sessionStore).isEmpty();
  }
}
