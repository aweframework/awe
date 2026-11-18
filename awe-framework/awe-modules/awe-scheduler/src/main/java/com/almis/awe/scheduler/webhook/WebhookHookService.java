package com.almis.awe.scheduler.webhook;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Services of the hook that the task form defines for a WEBHOOK task: its validation, the hook row that follows the
 * launch type and the full URL of the endpoint
 */
public class WebhookHookService extends ServiceConfig {

  private static final Pattern HOOK_KEY = Pattern.compile("[a-z0-9-]{1,64}");
  private static final String TRIGGER_PATH = "/scheduler/api/v1/webhooks/triggers/";
  private static final String KEY_TITLE = "SCHEDULER_WEBHOOK_KEY_INVALID_TITLE";
  private static final String KEY_MESSAGE = "SCHEDULER_WEBHOOK_KEY_INVALID_MESSAGE";
  private static final String SECRET_MESSAGE = "SCHEDULER_WEBHOOK_SECRET_REQUIRED_MESSAGE";
  private static final String DUPLICATE_MESSAGE = "SCHEDULER_WEBHOOK_KEY_DUPLICATED_MESSAGE";

  private final WebhookInboxRepository repository;

  /**
   * Constructor
   *
   * @param repository Webhook hooks and inbox
   */
  public WebhookHookService(WebhookInboxRepository repository) {
    this.repository = repository;
  }

  /**
   * Reject the hook of a WEBHOOK task that cannot be saved: a key outside the allowed alphabet, no secret property
   * or a key that another task already uses. Tasks of any other launch type are not checked.
   *
   * @param taskId         Task identifier (null while the task is new)
   * @param launchType     Launch type of the task
   * @param hookKey        Hook key
   * @param secretProperty Name of the property that holds the secret
   * @throws AWException The hook is not valid
   */
  public void validateWebhookHook(Integer taskId, Integer launchType, String hookKey, String secretProperty)
    throws AWException {
    if (!isWebhook(launchType)) {
      return;
    }
    if (hookKey == null || !HOOK_KEY.matcher(hookKey).matches()) {
      throw new AWException(getLocale(KEY_TITLE), getLocale(KEY_MESSAGE));
    }
    if (secretProperty == null || secretProperty.isBlank()) {
      throw new AWException(getLocale(KEY_TITLE), getLocale(SECRET_MESSAGE));
    }
    if (repository.findHook(hookKey).filter(hook -> !hook.getTaskId().equals(taskId)).isPresent()) {
      throw new AWException(getLocale(KEY_TITLE), getLocale(DUPLICATE_MESSAGE, hookKey));
    }
  }

  /**
   * Save the hook of a task: a WEBHOOK task has it, any other task has none
   *
   * @param taskId         Task identifier
   * @param launchType     Launch type of the task
   * @param hookKey        Hook key
   * @param active         Active flag of the hook
   * @param secretProperty Name of the property that holds the secret
   * @param typeFilter     Type of the messages the hook accepts (null for any)
   * @throws AWException Error saving the hook
   */
  public void saveWebhookHook(Integer taskId, Integer launchType, String hookKey, Integer active,
                              String secretProperty, String typeFilter) throws AWException {
    if (isWebhook(launchType)) {
      repository.saveHook(new WebhookHook().setTaskId(taskId).setHookKey(hookKey).setActive(active)
        .setSecretProperty(secretProperty).setTypeFilter(typeFilter));
    } else {
      repository.deleteHook(taskId);
    }
  }

  /**
   * Full URL of the endpoint of a hook, built on the address the request came to
   *
   * @param hookKey Hook key
   * @return Data list with the URL in the column value (empty without a key)
   */
  public ServiceData getWebhookUrl(String hookKey) {
    String url = hookKey == null || hookKey.isBlank() ? "" : ServletUriComponentsBuilder.fromCurrentContextPath()
      .path(TRIGGER_PATH + hookKey.trim()).build().toUriString();
    Map<String, CellData> row = new HashMap<>();
    row.put("value", new CellData(url));
    DataList dataList = new DataList();
    dataList.getRows().add(row);
    dataList.setRecords(1L);
    return new ServiceData().setDataList(dataList);
  }

  private boolean isWebhook(Integer launchType) {
    return TaskLaunchType.WEBHOOK.getValue().equals(launchType);
  }
}
