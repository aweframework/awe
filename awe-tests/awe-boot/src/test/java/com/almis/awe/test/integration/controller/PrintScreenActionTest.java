package com.almis.awe.test.integration.controller;

import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Print of a real screen through the action of the print dialog ({@code PrnOpt}): its accept button launches the
 * {@code ExpFil} maintain with the print options and the print data of every component of the screen, and the client
 * downloads each generated file through {@code /file/download}. The report generator is the real one (Jasper).
 * <p>
 * It replaces the browser flow {@code PrintTestsIT.verifyPrintScreen} of both engines (menu, print button, print dialog,
 * Generate, success message) for {@code users}, {@code profiles}, {@code matrix-test} (selected tab and all tabs),
 * {@code grid-and-chart} and {@code layout2-test}: the browser only sees the success message, which the action returns
 * even when a file fails. The chart of {@code grid-and-chart} is printed from an SVG like the one the clients draw; the
 * drawing of that SVG is client code, out of this test.
 */
@Tag("integration")
@DisplayName("Print screen action Tests")
@WithMockUser
class PrintScreenActionTest extends AbstractSpringAppIntegrationTest {

  private static final String DATA_SUFFIX = ".data";
  // Formats of the print dialog (a multiple selector: the clients send the selected values as a list)
  private static final List<String> FORMATS = List.of("PDF", "XLSX", "DOCX", "CSV", "TEXT");
  private static final String PRINT_ALL_TABS = "0";
  private static final String PRINT_SELECTED_TAB = "1";
  // The image the clients draw from the chart (SVG) and send with the print data
  private static final String CHART_IMAGE = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"200\">"
    + "<polyline points=\"0,150 100,80 200,120 300,40 400,90\" fill=\"none\" stroke=\"#0080FF\"/></svg>";

  /**
   * Each format the print dialog offers gives one file to download, not empty and with the signature of its format;
   * the tab option of the dialog decides which tabs are printed.
   *
   * @param option     Menu option of the screen
   * @param screen     Screen of the option
   * @param printTabs  Value of the tab option of the dialog (0 all tabs, 1 selected tab)
   * @param printed    Text of the printed data that the CSV must contain
   * @param notPrinted Text of a tab not printed that the CSV must not contain (empty when every tab is printed)
   */
  @ParameterizedTest(name = "{0} (tab option {2})")
  @CsvSource({
    "matrix-test,    MatTst,    " + PRINT_SELECTED_TAB + ", aweora1,    adminflare",
    "matrix-test,    MatTst,    " + PRINT_ALL_TABS + ",     adminflare,  ''",
    "grid-and-chart, GrdChrPrn, " + PRINT_SELECTED_TAB + ", AppParKey,  ''",
    "users,          user-admin,    " + PRINT_SELECTED_TAB + ", Pato Donald, ''",
    "profiles,       profile-admin, " + PRINT_SELECTED_TAB + ", operator,    ''",
    "layout2-test,   layout2,       " + PRINT_SELECTED_TAB + ", amazonia,    ''"})
  @DisplayName("Printing a screen generates a file of each format of the print dialog")
  void printScreenGeneratesEveryFormat(String option, String screen, String printTabs, String printed, String notPrinted)
    throws Exception {
    MockHttpSession session = userSession();
    ObjectNode parameters = printParameters(session, option, screen, printTabs);

    JsonNode actions = objectMapper.readTree(mockMvc.perform(post("/action/maintain/ExpFil").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(parameters.toString()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());

    // The success message the dialog shows, and one file to download per format
    List<String> messages = new ArrayList<>();
    List<FileData> files = new ArrayList<>();
    for (JsonNode action : actions) {
      switch (action.get("type").asString()) {
        case "message" -> messages.add(action.get("parameters").get("type").asString());
        case "get-file" -> files.add(FileUtil.stringToFileData(action.get("parameters").get("filename").asString()));
        default -> {
          // Other client actions (end of load) do not change the print
        }
      }
    }
    assertThat(messages).containsExactly("ok");
    assertThat(files).extracting(file -> file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1))
      .containsExactly("pdf", "xlsx", "docx", "csv", "txt");

    for (FileData file : files) {
      byte[] content = download(session, file);
      assertThat(content).as(file.getFileName()).isNotEmpty().hasSize(file.getFileSize().intValue());
      switch (file.getFileName().substring(file.getFileName().lastIndexOf('.') + 1)) {
        case "pdf" -> assertThat(new String(content, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        case "xlsx", "docx" -> assertThat(new String(content, 0, 2, StandardCharsets.ISO_8859_1)).as(file.getFileName())
          .isEqualTo("PK");
        case "csv" -> assertPrintedData(new String(content, StandardCharsets.UTF_8), printed, notPrinted);
        default -> assertThat(new String(content, StandardCharsets.UTF_8)).isNotBlank();
      }
    }
  }

  private static void assertPrintedData(String csv, String printed, String notPrinted) {
    assertThat(csv).contains(printed);
    if (!notPrinted.isEmpty()) {
      assertThat(csv).doesNotContain(notPrinted);
    }
  }

  /**
   * Session of a user with the menu of an administrator
   */
  private MockHttpSession userSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", "test").session(session)).andExpect(status().isOk());
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session))
      .andExpect(status().isOk());
    return session;
  }

  private byte[] download(MockHttpSession session, FileData file) throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode()
      .put("filename", FileUtil.fileDataToString(file))
      .put("d", 0);
    return mockMvc.perform(post("/file/download").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(parameters.toString()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsByteArray();
  }

  /**
   * Parameters the print dialog sends: the print options and the print data of the components of the screen, as the
   * clients build them from the screen they show (the screen data of the option)
   */
  private ObjectNode printParameters(MockHttpSession session, String option, String screen, String printTabs) throws Exception {
    JsonNode screenData = objectMapper.readTree(mockMvc.perform(post("/screen-data/" + option).session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"base\",\"option\":\"" + option + "\"}"))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    for (JsonNode component : screenData.get("components")) {
      addPrintData(parameters, component);
    }

    // Options of the print dialog: generate the files of every format, with the tab option given
    parameters.put("PrnScr", screen)
      .put("ActPrn", "CREATE")
      .put("TypPrn", printTabs)
      .put("DblFmtPrn", "0")
      .putNull("UsrPrn")
      .putNull("reportOrientation");
    FORMATS.forEach(parameters.putArray("FmtPrn")::add);
    return parameters;
  }

  private void addPrintData(ObjectNode parameters, JsonNode component) {
    String id = component.get("id").asString();
    JsonNode controller = component.get("controller");
    JsonNode model = component.get("model");
    switch (controller.get("elementType").asString()) {
      case "Grid" -> addGridPrintData(parameters, id, controller, model);
      case "Chart" -> parameters.set(id, JsonNodeFactory.instance.objectNode().put("image", CHART_IMAGE));
      case "Tab", "Criteria" -> addCriterionPrintData(parameters, id, controller, model);
      default -> {
        // Other components send no print data
      }
    }
  }

  /**
   * Criteria and tabs: the selected value, and its text and values when printable. A tab with no selection shows its
   * first tab.
   */
  private void addCriterionPrintData(ObjectNode parameters, String id, JsonNode controller, JsonNode model) {
    JsonNode values = model.get("values");
    JsonNode selected = model.get("selected");
    String value = selected.isEmpty() ? null : selected.get(0).asString();
    if (value == null && "Tab".equals(controller.get("elementType").asString()) && !values.isEmpty()) {
      value = values.get(0).path("value").asString("");
    }
    parameters.put(id, value);
    if (controller.path("printable").asBoolean(true)) {
      String text = value;
      for (JsonNode option : values) {
        if (option.path("value").asString("").equals(value)) {
          text = option.path("label").asString("");
        }
      }
      parameters.set(id + DATA_SUFFIX, JsonNodeFactory.instance.objectNode().put("text", text).set("all", values));
    }
  }

  /**
   * Grids: the visible columns with the pagination and the footer, and the values and cells of each column
   */
  private void addGridPrintData(ObjectNode parameters, String id, JsonNode controller, JsonNode model) {
    ArrayNode visibleColumns = JsonNodeFactory.instance.arrayNode();
    ObjectNode footer = JsonNodeFactory.instance.objectNode();
    for (JsonNode column : controller.get("columnModel")) {
      String name = column.get("name").asString();
      if (column.path("sendable").asBoolean(false)) {
        addColumnPrintData(parameters, name, model.get("values"));
        footer.set(name, JsonNodeFactory.instance.objectNode().putNull("value").put("label", ""));
      }
      if (column.path("printable").asBoolean(false) && !column.path("hidden").asBoolean(false)
        && !"button".equals(column.path("component").asString(""))) {
        visibleColumns.add(JsonNodeFactory.instance.objectNode()
          .put("name", name)
          .put("label", column.path("label").asString(""))
          .put("component", column.path("component").asString(null))
          .put("charlength", column.path("charlength").asInt(10))
          .put("align", column.path("align").asString("left")));
      }
    }
    ObjectNode gridData = JsonNodeFactory.instance.objectNode()
      .put("max", controller.path("max").asInt(30))
      .put("page", 1);
    gridData.set("visibleColumns", visibleColumns);
    gridData.set("footer", footer);
    parameters.set(id + DATA_SUFFIX, gridData);
  }

  private static void addColumnPrintData(ObjectNode parameters, String name, JsonNode rows) {
    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    ArrayNode cells = JsonNodeFactory.instance.arrayNode();
    for (JsonNode row : rows) {
      JsonNode cell = row.path(name);
      JsonNode value = cell.isObject() ? cell.path("value") : cell;
      value = value.isMissingNode() || value.isNull() ? JsonNodeFactory.instance.stringNode("") : value;
      String label = cell.isObject() && cell.hasNonNull("label") ? cell.get("label").asString() : value.asString("");
      values.add(value);
      cells.add(JsonNodeFactory.instance.objectNode().put("label", label).set("value", value));
    }
    parameters.set(name, values);
    parameters.set(name + DATA_SUFFIX, cells);
  }
}
