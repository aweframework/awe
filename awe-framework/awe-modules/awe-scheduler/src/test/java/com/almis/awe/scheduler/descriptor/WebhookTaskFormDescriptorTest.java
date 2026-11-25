package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the Webhook launch type of the task editor: its fields, the pointer column, the hook URL, the
 * maintains that save the hook, the query that loads it and the locales (#979).
 */
class WebhookTaskFormDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String EDITOR = "screen/scheduler-task-editor.xml";

  @Test
  void theWebhookViewShowsItsContainerAndTheOtherViewsHideIt() throws IOException {
    String xml = read(EDITOR);

    String view = "\n        <dependency-element id=\"SchVw\" condition=\"in\" value=\"WHK\" />";
    String container = "\" async=\"true\" target-action=\"hidden\" target=\"#TskEdtWhk\" />";
    String show = element(xml, "<dependency initial=\"true\">" + view, "</dependency>");
    assertTrue(show.contains("<dependency-action type=\"remove-class" + container));
    String hide = element(xml, "<dependency initial=\"true\" invert=\"true\">" + view, "</dependency>");
    assertTrue(hide.contains("<dependency-action type=\"add-class" + container));
  }

  @Test
  void theHookKeyAndTheSecretPropertyAreRequiredOnlyForAWebhookTask() throws IOException {
    String xml = read(EDITOR);

    for (String id : new String[]{"HokKey", "HokSec"}) {
      String criteria = element(xml, "id=\"" + id + "\"", "</criteria>");
      assertTrue(criteria.contains("target-type=\"set-required\""), id);
      assertTrue(criteria.contains("id=\"TypLch\" condition=\"eq\" value=\"4\""), id);
    }
    assertTrue(xml.contains("id=\"HokAct\""));
    assertTrue(xml.contains("id=\"HokTyp\""));
  }

  @Test
  void theParametersGridHasAPointerColumnShownOnlyForAWebhookTask() throws IOException {
    String column = element(read(EDITOR), "name=\"ParPtr\"", "</column>");

    assertTrue(column.contains("target-type=\"show-column\""));
    assertTrue(column.contains("<dependency-element id=\"TypLch\" condition=\"eq\" value=\"4\" />"));
    assertTrue(column.contains("help=\"SCHEDULER_WEBHOOK_POINTER_HELP\""));
  }

  @Test
  void theFormShowsTheFullUrlOfTheKeyInAReadOnlyField() throws IOException {
    String url = element(read(EDITOR), "id=\"HokUrl\"", "</criteria>");

    assertTrue(url.contains("readonly=\"true\""));
    assertTrue(url.contains("target-action=\"webhookUrl\""));
    // A text input takes the value of the query; the data action only fills lists, and React leaves a text empty with it
    assertTrue(url.contains("server-action=\"value\""));
    assertTrue(url.contains("<dependency-element id=\"HokKey\" />"));
    String query = query("webhookUrl");
    assertTrue(query.contains("service=\"getWebhookUrl\""));
    assertTrue(query.contains("name=\"HokKey\""));
    assertTrue(read("global/Services.xml").contains("method=\"getWebhookUrl\""));
  }

  @Test
  void bothTaskMaintainsValidateTheHookBeforeAndSaveItAfterTheCommit() throws IOException {
    String xml = read("global/Maintain.xml");

    for (String target : new String[]{"NewSchedulerTaskEditor", "UpdateSchedulerTaskEditor"}) {
      String body = element(xml, "<target name=\"" + target + "\"", "</target>");
      int validate = body.indexOf("<serve service=\"validateWebhookHook\">");
      int firstWrite = body.indexOf(body.contains("<insert audit=\"HISAweSchTsk\">") ? "<insert audit=\"HISAweSchTsk\">"
        : "<update audit=\"HISAweSchTsk\">");
      assertTrue(validate >= 0 && validate < firstWrite, target + " validates before writing");
      assertTrue(body.indexOf("<serve service=\"saveWebhookHook\">") > body.indexOf("<commit/>"), target + " saves after the commit");
    }
  }

  /**
   * A remote scheduler reads the task (and its hook) with its own connection: the hook is committed before the
   * scheduler is told, else that read waits for the transaction of the maintain, which waits for the scheduler
   */
  @Test
  void bothTaskMaintainsCommitTheHookBeforeTellingTheScheduler() throws IOException {
    String xml = read("global/Maintain.xml");
    String[][] targets = {{"NewSchedulerTaskEditor", "insertSchedulerTask"}, {"UpdateSchedulerTaskEditor", "refreshSchedulerTask"}};

    for (String[] target : targets) {
      String body = element(xml, "<target name=\"" + target[0] + "\"", "</target>");
      int save = body.indexOf("<serve service=\"saveWebhookHook\">");
      int scheduler = body.indexOf("<serve service=\"" + target[1] + "\">");
      int commit = body.indexOf("<commit/>", save);
      assertTrue(save >= 0 && commit > save && commit < scheduler, target[0] + " commits the hook before " + target[1]);
    }
  }

  @Test
  void theHookRowIsInsertedAndDeletedByServerOnlyTargets() throws IOException {
    String xml = read("global/Maintain.xml");

    assertTrue(element(xml, "<target name=\"insertWebhookHook\"", "</target>").contains("server-only=\"true\""));
    assertTrue(element(xml, "<target name=\"deleteWebhookHook\"", "</target>").contains("server-only=\"true\""));
  }

  @Test
  void theTaskQueryLoadsTheHookFieldsOfTheForm() throws IOException {
    String query = query("taskData");

    for (String alias : new String[]{"HokKey", "HokAct", "HokSec", "HokTyp"}) {
      assertTrue(query.contains("alias=\"" + alias + "\" table=\"Hok\"") || query.contains("alias=\"" + alias + "\""), alias);
    }
    assertTrue(query.contains("<table id=\"AweSchTskHok\" alias=\"Hok\"/>"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"en-GB", "es-ES", "fr-FR"})
  void everyLocaleCarriesTheWebhookFormKeys(String locale) throws IOException {
    String xml = read("locale/Locale-" + locale + ".xml");

    for (String key : new String[]{"KEY", "KEY_HELP", "ACTIVE", "SECRET", "SECRET_HELP", "TYPE", "TYPE_HELP", "URL",
      "URL_HELP", "POINTER", "POINTER_HELP", "KEY_INVALID_TITLE", "KEY_INVALID_MESSAGE", "SECRET_REQUIRED_MESSAGE",
      "KEY_DUPLICATED_MESSAGE"}) {
      assertTrue(xml.contains("<locale name=\"SCHEDULER_WEBHOOK_" + key + "\""), locale + " is missing " + key);
    }
  }

  private String element(String xml, String from, String to) {
    int start = xml.indexOf(from);
    assertTrue(start >= 0, from + " must exist");
    int end = xml.indexOf(to, start + from.length());
    assertTrue(end >= 0, to + " must follow " + from);
    return xml.substring(start, end);
  }

  private String query(String id) throws IOException {
    return element(read("global/Queries.xml"), "<query id=\"" + id + "\"", "</query>");
  }

  private String read(String path) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BASE + path)) {
      assertNotNull(stream, BASE + path + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
