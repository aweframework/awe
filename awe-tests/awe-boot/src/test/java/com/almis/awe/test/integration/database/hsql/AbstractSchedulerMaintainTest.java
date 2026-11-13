package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers of the scheduler maintain tests: launch the maintains and the queries of the scheduler screens through the
 * action controller, as the screens do, and read their results.
 */
@WithMockUser(username = "test", password = "test")
abstract class AbstractSchedulerMaintainTest extends AbstractSpringAppIntegrationTest {

  /**
   * Initial date of the scheduled tasks: far enough in the future that no trigger fires while the tests run
   */
  protected static final String FUTURE_DATE = "01/01/2099";
  protected static final String SCHEDULER_RUNNING = "Started and running";

  /**
   * Launch a maintain and check that it ends with a success message
   *
   * @param target     Maintain target
   * @param parameters Parameters that the screen sends
   */
  protected void maintain(String target, ObjectNode parameters) throws Exception {
    JsonNode actions = perform("maintain", target, parameters);
    List<String> messageTypes = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("message".equals(action.get("type").asString())) {
        messageTypes.add(action.get("parameters").get("type").asString());
      }
    }
    assertThat(messageTypes).as("Messages of maintain %s: %s", target, actions).containsExactly("ok");
  }

  /**
   * Launch the query of a screen and return its rows
   *
   * @param query      Query
   * @param parameters Parameters that the screen sends
   * @return Rows
   */
  protected List<JsonNode> rows(String query, ObjectNode parameters) throws Exception {
    JsonNode actions = perform("data", query, parameters);
    for (JsonNode action : actions) {
      if ("fill".equals(action.get("type").asString())) {
        List<JsonNode> rows = new ArrayList<>();
        action.get("parameters").get("datalist").get("rows").forEach(rows::add);
        return rows;
      }
    }
    throw new AssertionError("Query " + query + " returned no data: " + actions);
  }

  /**
   * Find the row of a query with a value in a column
   *
   * @param rows   Rows
   * @param column Column
   * @param value  Value
   * @return Row
   */
  protected Optional<JsonNode> find(List<JsonNode> rows, String column, String value) {
    return rows.stream().filter(row -> row.has(column) && value.equals(row.get(column).asString())).findFirst();
  }

  /**
   * Values of a column of the rows
   */
  protected List<String> column(List<JsonNode> rows, String column) {
    return rows.stream().map(row -> row.get(column).asString()).toList();
  }

  /**
   * Status of the scheduler that the management screen shows
   */
  protected String schedulerStatus() throws Exception {
    return rows("getSchedulerMetadata", params()).get(0).get("schedulerStatus").asString();
  }

  /**
   * Keys of the jobs that the management screen shows as configured in the scheduler
   */
  protected List<String> configuredJobs() throws Exception {
    return column(rows("getConfiguredJobs", params()), "configuredJobTaskKey");
  }

  /**
   * Row of a task in the task list of the tasks screen
   *
   * @param taskId Task identifier
   * @return Task row
   */
  protected Optional<JsonNode> taskListRow(int taskId) throws Exception {
    List<JsonNode> rows = rows("getSchedulerTaskList", params().put("CrtTsk", taskId));
    assertThat(rows).hasSizeLessThanOrEqualTo(1);
    return rows.stream().findFirst();
  }

  /**
   * Data that the update task screen loads for a task
   *
   * @param taskId Task identifier
   * @return Task data
   */
  protected JsonNode taskData(int taskId) throws Exception {
    List<JsonNode> rows = rows("taskData", params().put("IdeTsk.selected", taskId));
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  /**
   * Create a task with the new task screen maintain and return its identifier
   *
   * @param parameters Task parameters
   * @return Task identifier
   */
  protected int createTask(ObjectNode parameters) throws Exception {
    maintain("NewSchedulerTask", parameters);
    String name = parameters.get("Nam").asString();
    return find(rows("getSchedulerTaskList", params()), "Nam", name)
      .orElseThrow(() -> new AssertionError("Task " + name + " not in the task list"))
      .get("IdeTsk").asInt();
  }

  /**
   * Delete tasks with the maintain of the tasks screen
   *
   * @param taskIds Task identifiers
   */
  protected void deleteTasks(Integer... taskIds) throws Exception {
    maintain("DeleteSchedulerTask", params().set("IdeTsk", ints(taskIds)));
  }

  /**
   * Parameters of a manual task as the new task screen sends them: a maintain task with one parameter and a broadcast
   * report
   *
   * @param name Task name
   * @return Parameters
   */
  protected ObjectNode manualTask(String name) {
    ObjectNode parameters = params()
      .put("Nam", name)
      .put("Des", name + " description")
      .put("Act", 1)
      .put("NumStoExe", 45)
      .put("TimOutExe", 59)
      .put("TypExe", 1)
      .put("CmdExe", "waitSomeSeconds")
      .put("LchDepWrn", 1)
      .put("LchDepErr", 1)
      .put("LchSetWrn", 0)
      .put("TypLch", 0)
      .put("RepTyp", 2)
      .put("RepMsg", "Broadcast message");
    parameters.set("RepSndSta", strings("1", "2"));
    parameters.set("RepUsrDst", strings("test"));
    parameters.set("RepEmaDst", strings());
    parameters.set("ParNam", strings("secondsToWait"));
    parameters.set("ParSrc", ints(0));
    parameters.set("ParTyp", strings("INTEGER"));
    parameters.set("ParVal", strings("3"));
    parameters.set("DepTskIde", ints());
    return launchLists(parameters);
  }

  /**
   * Parameters of a scheduled task that launches every some hours from {@link #FUTURE_DATE}
   *
   * @param name  Task name
   * @param hours Hours between launches
   * @return Parameters
   */
  protected ObjectNode scheduledTask(String name, int hours) {
    return manualTask(name)
      .put("TypLch", 1)
      .put("RptTyp", 2)
      .put("RptNum", hours)
      .put("IniDat", FUTURE_DATE)
      .put("IniTim", "00:00:00");
  }

  /**
   * Empty lists of the custom launch, which the screen always sends
   */
  protected ObjectNode launchLists(ObjectNode parameters) {
    for (String list : List.of("years", "months", "weeks", "weekDays", "days", "hours", "minutes", "seconds")) {
      parameters.set(list, strings());
    }
    return parameters;
  }

  /**
   * Name that no other record has, short enough for the name columns of the scheduler tables
   */
  protected String unique(String prefix) {
    return prefix + " " + System.nanoTime() % 1_000_000;
  }

  protected ObjectNode params() {
    return objectMapper.createObjectNode();
  }

  protected ArrayNode strings(String... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (String value : values) {
      list.add(value);
    }
    return list;
  }

  protected ArrayNode ints(Integer... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (Integer value : values) {
      if (value == null) {
        list.addNull();
      } else {
        list.add(value);
      }
    }
    return list;
  }

  private JsonNode perform(String type, String name, ObjectNode parameters) throws Exception {
    String response = mockMvc.perform(post("/action/" + type + "/" + name)
        .with(csrf())
        .content(objectMapper.writeValueAsString(parameters))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response);
  }
}
