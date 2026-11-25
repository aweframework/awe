package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every scheduler grid with a row menu has a last narrow column with a «⋯» button that opens that same menu for its
 * row (#993, T33): an icon-only ghost button named «More actions» whose only action is show-context-menu, which selects
 * the row of the button and opens the menu of the grid under it (same options and the same rules as the right click).
 */
class SchedulerRowMenuButtonDescriptorTest {

  private static final String BASE = "application/awe-scheduler/screen/";

  @ParameterizedTest
  @CsvSource({
    "scheduler-task-catalog.xml, GrdTskLst",
    "scheduler-activity.xml, GrdExeAct",
    "scheduler-activity.xml, GrdExeRun",
    "scheduler-task.xml, GrdTskExe"
  })
  void theLastColumnOfAGridWithARowMenuIsTheButtonThatOpensIt(String screen, String gridId) throws Exception {
    Element grid = grid(parse(screen), gridId);
    assertFalse(children(grid, "context-button").isEmpty(), gridId + " has a row menu");

    List<Element> visible = children(grid, "column").stream()
      .filter(column -> !"true".equals(column.getAttribute("hidden")))
      .toList();
    Element more = visible.get(visible.size() - 1);
    assertEquals("MorBtn", more.getAttribute("name"));
    assertEquals("button", more.getAttribute("component"));
    assertEquals("lucide:ellipsis", more.getAttribute("icon"));
    assertEquals("BUTTON_MORE_ACTIONS", more.getAttribute("title"));
    assertEquals("btn-ghost no-label", more.getAttribute("style"));
    assertEquals("", more.getAttribute("label"));
    assertEquals("4", more.getAttribute("charlength"));
    assertEquals("false", more.getAttribute("sortable"));
    assertEquals("false", more.getAttribute("sendable"));
    List<Element> actions = children(more, "button-action");
    assertEquals(1, actions.size());
    assertEquals("show-context-menu", actions.get(0).getAttribute("type"));
    assertEquals("", actions.get(0).getAttribute("target"));
  }

  private static Element grid(Document document, String id) {
    NodeList grids = document.getElementsByTagName("grid");
    for (int index = 0; index < grids.getLength(); index++) {
      Element grid = (Element) grids.item(index);
      if (id.equals(grid.getAttribute("id"))) {
        return grid;
      }
    }
    throw new AssertionError("grid " + id + " must exist");
  }

  private static List<Element> children(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && tag.equals(element.getTagName())) {
        list.add(element);
      }
    }
    return list;
  }

  private static Document parse(String path) throws Exception {
    try (InputStream stream = SchedulerRowMenuButtonDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
