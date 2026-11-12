package com.almis.awe.samples.helpdesk.chat;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.samples.helpdesk.chat.ChatTicketRules.Outcome;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.function.Supplier;

/**
 * Opens a ticket for a visitor of the public support chat, with no sign-in. The visitor is a requester identified by
 * email (the existing one or a new one) and follows the ticket with the public status check: access key and email.
 * <p>
 * The answer is a {@link ServiceData} the maintain target sends back: the title says what happened ("HD-601" with the
 * access key as message, "RECEIVED" for a bot, or the code of the refusal: LIMIT, EMAIL, CATEGORY, DESCRIPTION)
 */
@Slf4j
@Service
public class ChatTicketService extends ServiceConfig {

  /** Session parameter with the tickets the session opened through the chat */
  static final String SESSION_COUNTER = "HdkChatTickets";
  static final String RECEIVED = "RECEIVED";
  private static final String REQUESTER = "IdeReq";

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final Supplier<AweSession> sessions;

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   */
  @Autowired
  public ChatTicketService(QueryService queryService, MaintainService maintainService) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.sessions = this::getSession;
  }

  /**
   * Constructor with the session of the visitor given (tests)
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param sessions        Session of the visitor
   */
  ChatTicketService(QueryService queryService, MaintainService maintainService, Supplier<AweSession> sessions) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.sessions = sessions;
  }

  /**
   * Create the ticket of a visitor
   *
   * @param honeypot    Hidden field of the chat: only a bot fills it
   * @param email       Email of the visitor
   * @param category    Category of the ticket
   * @param description Description of the problem
   * @return Answer of the maintain
   * @throws AWException Error creating the ticket
   */
  public ServiceData createTicket(String honeypot, String email, String category, String description) throws AWException {
    AweSession session = sessions.get();
    int created = created(session);
    Outcome outcome = ChatTicketRules.evaluate(honeypot, email, category, description, categories(), created,
      ChatTicketRules.SESSION_LIMIT);
    switch (outcome) {
      case HONEYPOT:
        log.info("[chat] Request dropped: the hidden field was filled");
        return new ServiceData().setTitle(RECEIVED);
      case CREATE:
        ServiceData answer = create(ChatTicketRules.normalizeEmail(email), category, description.trim());
        if (session != null) {
          session.setParameter(SESSION_COUNTER, created + 1);
        }
        return answer;
      default:
        return new ServiceData().setType(AnswerType.WARNING).setTitle(outcome.name());
    }
  }

  private ServiceData create(String email, String category, String description) throws AWException {
    int requester = requester(email);
    int ticket = maintainService.getNextSequenceValue("TktKey");
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("NewTkt", ticket);
    parameters.put(REQUESTER, requester);
    parameters.put("Cat", category);
    parameters.put("Sub", ChatTicketRules.subject(description));
    parameters.put("Dsc", description);
    maintainService.launchPrivateMaintain("HdkChatTktNew", parameters);

    ObjectNode key = JsonNodeFactory.instance.objectNode();
    key.put("IdeTkt", ticket);
    DataList row = queryService.launchQuery("HdkChatTktKey", key, false).getDataList();
    log.info("[chat] Ticket {} created for requester {}", ticket, requester);
    return new ServiceData().setTitle(DataListUtil.getData(row, 0, "Cod")).setMessage(DataListUtil.getData(row, 0, "AccKey"));
  }

  private int requester(String email) throws AWException {
    ObjectNode search = JsonNodeFactory.instance.objectNode();
    search.put("Eml", email);
    DataList found = queryService.launchQuery("HdkChatReqByEml", search, false).getDataList();
    if (found != null && !found.getRows().isEmpty()) {
      return Integer.parseInt(DataListUtil.getData(found, 0, REQUESTER));
    }
    int requester = maintainService.getNextSequenceValue("ReqKey");
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put(REQUESTER, requester);
    parameters.put("Eml", email);
    parameters.put("Nam", ChatTicketRules.requesterName(email));
    maintainService.launchPrivateMaintain("HdkChatReqNew", parameters);
    return requester;
  }

  private List<String> categories() throws AWException {
    DataList list = queryService.launchQuery("HdkCatPub", JsonNodeFactory.instance.objectNode(), false).getDataList();
    return DataListUtil.getColumn(list, "value").stream().map(CellData::getStringValue).toList();
  }

  private static int created(AweSession session) {
    return session != null && session.getParameter(SESSION_COUNTER) instanceof Integer count ? count : 0;
  }
}
