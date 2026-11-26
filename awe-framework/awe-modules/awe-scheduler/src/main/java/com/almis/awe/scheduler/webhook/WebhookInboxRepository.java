package com.almis.awe.scheduler.webhook;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.MaintainConstants.CLAIM_WEBHOOK_MESSAGE;
import static com.almis.awe.scheduler.constant.MaintainConstants.DELETE_WEBHOOK_HOOK;
import static com.almis.awe.scheduler.constant.MaintainConstants.FAIL_WEBHOOK_MESSAGE;
import static com.almis.awe.scheduler.constant.MaintainConstants.FINISH_WEBHOOK_MESSAGE;
import static com.almis.awe.scheduler.constant.MaintainConstants.INSERT_WEBHOOK_HOOK;
import static com.almis.awe.scheduler.constant.MaintainConstants.INSERT_WEBHOOK_MESSAGE;
import static com.almis.awe.scheduler.constant.MaintainConstants.PURGE_WEBHOOK_MESSAGES;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_DUE_WEBHOOK_MESSAGES;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_WEBHOOK_HOOK;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_WEBHOOK_MESSAGE;

/**
 * Persistence of the webhook hooks (table AweSchTskHok) and of the inbox of received webhooks (table AweSchHokMsg)
 */
public class WebhookInboxRepository {

  private static final String HOOK_KEY = "hookKey";
  private static final String MESSAGE_ID = "messageId";

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;

  /**
   * Constructor
   *
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param queryUtil       Query utilities
   */
  public WebhookInboxRepository(QueryService queryService, MaintainService maintainService, QueryUtil queryUtil) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
  }

  /**
   * Find a hook by its key
   *
   * @param hookKey Hook key
   * @return Hook, or empty when the key is unknown
   * @throws AWException Error retrieving the hook
   */
  public Optional<WebhookHook> findHook(String hookKey) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(HOOK_KEY, hookKey);
    DataList dataList = queryService.launchPrivateQuery(SCHEDULER_WEBHOOK_HOOK, parameters).getDataList();
    return DataListUtil.asBeanList(dataList, WebhookHook.class).stream().findFirst();
  }

  /**
   * Save the hook of a task: the row of the task is replaced
   *
   * @param hook Hook
   * @throws AWException Error saving the hook
   */
  public void saveHook(WebhookHook hook) throws AWException {
    deleteHook(hook.getTaskId());
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, hook.getTaskId());
    parameters.put(HOOK_KEY, hook.getHookKey());
    parameters.put("active", hook.getActive());
    parameters.put("secretProperty", hook.getSecretProperty());
    parameters.put("typeFilter", hook.getTypeFilter());
    maintainService.launchPrivateMaintain(INSERT_WEBHOOK_HOOK, parameters);
  }

  /**
   * Delete the hook of a task, if it has one
   *
   * @param taskId Task identifier
   * @throws AWException Error deleting the hook
   */
  public void deleteHook(Integer taskId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(TASK_ID, taskId);
    maintainService.launchPrivateMaintain(DELETE_WEBHOOK_HOOK, parameters);
  }

  /**
   * Check whether a message is already in the inbox
   *
   * @param hookKey   Hook key
   * @param messageId Message id (webhook-id header)
   * @return True when the inbox has it
   * @throws AWException Error retrieving the message
   */
  public boolean messageExists(String hookKey, String messageId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(HOOK_KEY, hookKey);
    parameters.put(MESSAGE_ID, messageId);
    return !queryService.launchPrivateQuery(SCHEDULER_WEBHOOK_MESSAGE, parameters).getDataList().getRows().isEmpty();
  }

  /**
   * Store a received message, ready to be processed
   *
   * @param hookKey    Hook key
   * @param messageId  Message id (webhook-id header)
   * @param type       Type of the message (null when it has none)
   * @param payload    Body of the message
   * @param receivedAt Reception date, which is also when it is first due
   * @throws AWException Error storing the message (also when it is already in the inbox)
   */
  public void insertMessage(String hookKey, String messageId, String type, String payload, Instant receivedAt)
    throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(HOOK_KEY, hookKey);
    parameters.put(MESSAGE_ID, messageId);
    parameters.put("type", type);
    parameters.put("payload", payload);
    parameters.put("receivedDate", asTimestamp(receivedAt));
    maintainService.launchPrivateMaintain(INSERT_WEBHOOK_MESSAGE, parameters);
  }

  /**
   * Find the messages that are due: received or failed, with fewer attempts than the limit, whose next attempt has come
   *
   * @param now         Current date
   * @param maxAttempts Attempts a message may have been tried
   * @param limit       Maximum number of messages to return
   * @return Due messages, the oldest first
   * @throws AWException Error retrieving the messages
   */
  public List<WebhookMessage> findDue(Instant now, int maxAttempts, int limit) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", String.valueOf(limit));
    parameters.put("maxAttempts", maxAttempts);
    parameters.put("now", asTimestamp(now));
    DataList dataList = queryService.launchPrivateQuery(SCHEDULER_DUE_WEBHOOK_MESSAGES, parameters).getDataList();
    return DataListUtil.asBeanList(dataList, WebhookMessage.class);
  }

  /**
   * Claim a message for one attempt. The update only succeeds while the message still has the attempts it was read
   * with, so of several nodes only one gets it; the others do not launch it.
   *
   * @param message    Message read from the inbox
   * @param leaseUntil Date until nobody else tries it (the next attempt if this one dies)
   * @return True when this call claimed the message
   * @throws AWException Error claiming the message
   */
  public boolean claim(WebhookMessage message, Instant leaseUntil) throws AWException {
    ObjectNode parameters = keyOf(message);
    parameters.put("attempts", message.getAttempts());
    parameters.put("newAttempts", message.getAttempts() + 1);
    parameters.put("leaseDate", asTimestamp(leaseUntil));
    ServiceData serviceData = maintainService.launchPrivateMaintain(CLAIM_WEBHOOK_MESSAGE, parameters);
    return serviceData.getResultDetails().stream().mapToLong(MaintainResultDetails::getRowsAffected).sum() == 1L;
  }

  /**
   * Mark a message as processed: it launched the task
   *
   * @param message Message
   * @param taskId  Task that was launched
   * @param at      Date of the processing
   * @throws AWException Error updating the message
   */
  public void markProcessed(WebhookMessage message, Integer taskId, Instant at) throws AWException {
    finish(message, "PROCESSED", taskId, null, at);
  }

  /**
   * Mark a message as ignored: it did not launch anything
   *
   * @param message Message
   * @param reason  Why it was ignored
   * @param at      Date of the processing
   * @throws AWException Error updating the message
   */
  public void markIgnored(WebhookMessage message, String reason, Instant at) throws AWException {
    finish(message, "IGNORED", null, reason, at);
  }

  /**
   * Mark a message as failed. It is tried again from the given date while it has attempts left
   *
   * @param message     Message
   * @param error       Error of the attempt
   * @param nextAttempt Date of the next attempt
   * @throws AWException Error updating the message
   */
  public void markFailed(WebhookMessage message, String error, Instant nextAttempt) throws AWException {
    ObjectNode parameters = keyOf(message);
    parameters.put("error", error);
    parameters.put("nextDate", asTimestamp(nextAttempt));
    maintainService.launchPrivateMaintain(FAIL_WEBHOOK_MESSAGE, parameters);
  }

  /**
   * Delete the messages that are over (processed, ignored or failed for good) and were received before a date
   *
   * @param before Limit date
   * @throws AWException Error deleting the messages
   */
  public void purge(Instant before) throws AWException {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put("limitDate", asTimestamp(before));
    maintainService.launchPrivateMaintain(PURGE_WEBHOOK_MESSAGES, parameters);
  }

  private void finish(WebhookMessage message, String status, Integer taskId, String error, Instant at)
    throws AWException {
    ObjectNode parameters = keyOf(message);
    parameters.put("status", status);
    parameters.put("processedDate", asTimestamp(at));
    parameters.put("error", error);
    parameters.put(TASK_ID, taskId);
    maintainService.launchPrivateMaintain(FINISH_WEBHOOK_MESSAGE, parameters);
  }

  private ObjectNode keyOf(WebhookMessage message) {
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(HOOK_KEY, message.getHookKey());
    parameters.put(MESSAGE_ID, message.getMessageId());
    return parameters;
  }

  /**
   * Format an instant as the timestamp text of the query and maintain variables
   *
   * @param instant Instant
   * @return Timestamp text
   */
  private String asTimestamp(Instant instant) {
    return DateUtil.TIMESTAMP_FORMAT_WEB_MS.format(instant.atZone(ZoneId.systemDefault()));
  }
}
