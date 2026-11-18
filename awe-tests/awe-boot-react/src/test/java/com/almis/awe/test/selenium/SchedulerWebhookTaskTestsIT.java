package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;


/**
 * Webhook triggered tasks of the scheduler, one flow on their screens: create a task with the Webhook launch in the wizard
 * (hook key, secret property, the JSON pointer of one parameter), see the URL that the form builds for the key, and delete
 * it. The class creates and deletes its own task and does not need any other record or class. The stored hook, pointer,
 * key rules and signed launch are proven by {@code SchedulerWebhookLaunchTest} and {@code WebhookHookServiceTest}; the flow
 * runs in this engine only.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerWebhookTaskTestsIT extends AbstractSchedulerTests {

  private static final String TASK = "Webhook task";
  private static final String HOOK_KEY = "webhook-flow";

  /**
   * Generate a new webhook task that maps one parameter of its maintain from the data of the webhook
   */
  @Test
  void t001_newWebhookTask() {
    // Title
    setTestTitle("Generate a new webhook triggered task");

    // New task
    addNew(TASK_CHECK_BUTTON, SCHEDULER, TASK_LIST_SCREEN);

    // FILL TASK DATA
    changeTaskData(TASK, "Webhook task description");

    // Suggest on selector
    selectContain("TypExe", "Maintain");

    // Suggest on selector
    suggest("maintain", "waitSomeSeconds", "waitSomeSeconds");

    // Click on next step
    clickButton("FwStep1");

    // Click on next step (parameters): the pointer column only exists for a webhook task, so the launch type comes first
    clickButton("FwStep2");

    // FILL LAUNCH

    // Suggest on selector
    selectContain("TypLch", "Webhook");

    // Insert text
    writeText("HokKey", HOOK_KEY);

    // Insert text
    writeText("HokSec", "awe.test.webhook.secret");

    // The form shows the URL of the key once the server answers the dependency on the key
    checkCriterionContents("HokUrl", "/scheduler/api/v1/webhooks/triggers/" + HOOK_KEY);

    // Click on previous step (parameters)
    clickButton("BkStep2");

    // Map the parameter from the data of the webhook
    editRow(PARAMETERS_GRID, EDITED_PARAMETER);
    writeText(PARAMETERS_GRID, "ParPtr", "/seconds");
    saveRow(PARAMETERS_GRID);

    // Click on next steps
    clickButton("FwStep2");
    clickButton("FwStep3");
    clickButton("FwStep4");

    // Store and confirm
    clickButtonAndConfirm("Finish");

    // Check element
    verifyNew(TASK_CRITERION, TASK);
  }

  /**
   * Delete the webhook task
   */
  @Test
  void t901_deleteWebhookTask() {
    // Title
    setTestTitle("Delete the webhook task");

    delete(TASK_CRITERION, TASK, SCHEDULER, TASK_LIST_SCREEN);
    verifyDeleted(TASK);
  }
}
