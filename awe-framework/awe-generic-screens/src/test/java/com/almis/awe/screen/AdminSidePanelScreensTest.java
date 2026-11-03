package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The buttons that open the side panel of the databases, sites and modules screens as a new record start it from scratch:
 * active, and without the site, module, profile or user rows of the record edited before (those rows live in a grid of the
 * panel, which keeps them until it is reset). The broker user and password of the queues panel apply only to a remote
 * connection.
 */
@DisplayName("Admin side panel screens")
class AdminSidePanelScreensTest {

  private static final String AWE = "application/awe/";
  private static final String DATABASES = "screen/databases/database-admin.xml";
  private static final String SITES = "screen/sites/site-admin.xml";
  private static final String MODULES = "screen/modules/module-admin.xml";
  private static final String QUEUES = "screen/queues/queue-admin.xml";

  private Document load(String path) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(AWE + path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  /**
   * Actions of a button or a context button, in order, as type:target[=value]
   */
  private List<String> actions(Document screen, String tag, String id) {
    NodeList buttons = screen.getElementsByTagName(tag);
    for (int i = 0; i < buttons.getLength(); i++) {
      Element button = (Element) buttons.item(i);
      if (id.equals(button.getAttribute("id"))) {
        List<String> actions = new ArrayList<>();
        NodeList children = button.getElementsByTagName("button-action");
        for (int j = 0; j < children.getLength(); j++) {
          Element action = (Element) children.item(j);
          String value = action.getAttribute("value");
          actions.add(action.getAttribute("type") + ":" + action.getAttribute("target") + (value.isEmpty() ? "" : "=" + value));
        }
        return actions;
      }
    }
    throw new AssertionError("No " + tag + " " + id);
  }

  /**
   * Dependencies of a criterion on another criterion, as target-type[/source-type]:element condition value
   */
  private List<String> dependenciesOn(Document screen, String criterion, String element) {
    NodeList criteria = screen.getElementsByTagName("criteria");
    for (int i = 0; i < criteria.getLength(); i++) {
      Element current = (Element) criteria.item(i);
      if (criterion.equals(current.getAttribute("id"))) {
        List<String> dependencies = new ArrayList<>();
        NodeList children = current.getElementsByTagName("dependency");
        for (int j = 0; j < children.getLength(); j++) {
          Element dependency = (Element) children.item(j);
          String source = dependency.getAttribute("source-type");
          NodeList elements = dependency.getElementsByTagName("dependency-element");
          for (int k = 0; k < elements.getLength(); k++) {
            Element dependencyElement = (Element) elements.item(k);
            if (element.equals(dependencyElement.getAttribute("id"))) {
              dependencies.add(dependency.getAttribute("target-type") + (source.isEmpty() ? "" : "/" + source) + ":" + element
                + " " + dependencyElement.getAttribute("condition") + " " + dependencyElement.getAttribute("value"));
            }
          }
        }
        return dependencies;
      }
    }
    throw new AssertionError("No criteria " + criterion);
  }

  @Test
  @DisplayName("A new connection starts active and without the sites of the connection edited before")
  void newConnectionStartsActiveWithoutSites() throws Exception {
    assertThat(actions(load(DATABASES), "button", "ButNew")).containsSubsequence(
      "unselect-all-rows:GrdDbsAdm", "value:PnlMod=new", "value:Act=1", "reset:SitModDbsLst",
      "copy-rows:SitModDbsViw=SitModDbsLst", "dialog:PnlDbs");
  }

  @Test
  @DisplayName("A new site starts active and without the module rows of the site edited before")
  void newSiteStartsActiveWithoutModules() throws Exception {
    assertThat(actions(load(SITES), "button", "ButNew")).containsSubsequence(
      "unselect-all-rows:GrdSitAdm", "value:PnlMod=new", "value:Act=1", "reset:SitModDbsLst", "dialog:PnlSit");
  }

  @Test
  @DisplayName("Duplicating a site opens the panel as a new site with its values and without its module rows")
  void duplicatedSiteStartsAsANewSiteWithoutModules() throws Exception {
    List<String> duplicate = actions(load(SITES), "context-button", "CtxButDup");

    assertThat(duplicate).containsSubsequence("check-one-selected:GrdSitAdm", "value:PnlMod=new", "reset:SitModDbsLst",
      "dialog:PnlSit");
    // The values of the selected site stay in the panel
    assertThat(duplicate).doesNotContain("unselect-all-rows:GrdSitAdm", "reset:Nam");
  }

  @Test
  @DisplayName("A new module starts active, on General and without the rows of the module edited before")
  void newModuleStartsActiveWithoutRows() throws Exception {
    assertThat(actions(load(MODULES), "button", "ButNew")).containsSubsequence(
      "unselect-all-rows:GrdModAdm", "value:PnlMod=new", "reset:IdeMod", "value:Act=1", "reset:MdlPrfLst",
      "reset:MdlUsrLst", "reset:MdlSitDbsLst", "value:ModTab=general", "dialog:PnlModAdm");
  }

  @Test
  @DisplayName("Duplicating a module opens the panel as a new module with its values and without its rows")
  void duplicatedModuleStartsAsANewModuleWithoutRows() throws Exception {
    List<String> duplicate = actions(load(MODULES), "context-button", "CtxButDup");

    assertThat(duplicate).containsSubsequence("check-one-selected:GrdModAdm", "value:PnlMod=new", "reset:IdeMod",
      "reset:MdlPrfLst", "reset:MdlUsrLst", "reset:MdlSitDbsLst", "value:ModTab=general", "dialog:PnlModAdm");
    // The values of the selected module stay in the panel
    assertThat(duplicate).doesNotContain("unselect-all-rows:GrdModAdm", "reset:Nam", "reset:Ord");
  }

  @Test
  @DisplayName("The broker user and password of a queue can be edited only for a remote connection and are cleared otherwise")
  void brokerCredentialsApplyOnlyToARemoteQueue() throws Exception {
    Document queues = load(QUEUES);

    for (String criterion : List.of("JmsUsr", "JmsPwd")) {
      assertThat(dependenciesOn(queues, criterion, "ConTyp")).as(criterion)
        .containsExactly("set-editable:ConTyp eq R", "input/reset:ConTyp ne R");
    }
  }
}
