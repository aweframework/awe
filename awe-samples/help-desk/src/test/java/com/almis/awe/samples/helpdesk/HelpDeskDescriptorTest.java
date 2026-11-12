package com.almis.awe.samples.helpdesk;

import com.almis.awe.testing.descriptor.AbstractApplicationDescriptorTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Descriptors of the help desk: they parse, every locale key exists in English, Spanish and French, every target of
 * a screen exists, only the intended targets are public, and the targets that only a supervisor may launch require a
 * permission that only the supervisor profile grants
 */
class HelpDeskDescriptorTest extends AbstractApplicationDescriptorTest {

  /**
   * Targets of the application that are reserved to the supervisors, with the permission each one must require
   */
  private static final Map<String, String> SUPERVISOR_TARGETS = Map.of(
    "HdkTktAssign", "tickets.reassign",
    "HdkTktPrioAny", "tickets.critical",
    "HdkKbNew", "kb.create",
    "HdkKbUpd", "kb.update",
    "HdkKbDel", "kb.delete",
    "HdkDashTickets", "reports.read",
    "HdkDashBacklog", "reports.read",
    "HdkDashTeams", "reports.read",
    "HdkDashSla", "reports.read",
    "HdkTeamTree", "teams.read");

  @Override
  protected Path moduleDirectory() {
    return Path.of("src/main/resources/application/helpdesk");
  }

  /**
   * Public targets, all of them reads: the status of the services, the published articles and their categories, and
   * the status and the public replies of one ticket (they need the access key of the ticket and the email of the
   * requester, both matched on the server; the sequential code HD-n is only for the agents). The only public maintain is the
   * support chat (HdkChatTicket): it checks a honeypot, the limit of tickets of the session and every field on the server
   */
  @Override
  protected Set<String> publicTargets() {
    return Set.of("HdkSvcPub", "HdkArtPub", "HdkCatPub", "HdkStatusPub", "HdkRepliesPub", "HdkChatTicket");
  }

  /**
   * Every supervisor-only target declares its supervisor permission, and the agent profile grants none of them (no
   * wildcard either) while the supervisor profile does
   */
  @Test
  void supervisorOnlyTargetsRequireSupervisorPermissions() throws Exception {
    Map<String, String> declared = new HashMap<>();
    declared.putAll(permissionsOf("global/Queries.xml", "query", "id"));
    declared.putAll(permissionsOf("global/Maintain.xml", "target", "name"));
    assertThat(declared).containsAllEntriesOf(SUPERVISOR_TARGETS);

    Set<String> agent = granted("profile/agent.xml");
    Set<String> supervisor = granted("profile/supervisor.xml");
    assertThat(agent).noneMatch(permission -> permission.contains("*"));
    assertThat(SUPERVISOR_TARGETS.values()).allSatisfy(permission -> {
      assertThat(agent).as("agent must not grant " + permission).doesNotContain(permission);
      assertThat(supervisor).as("supervisor must grant " + permission).contains(permission);
    });
  }

  /**
   * The agents' priority target takes the new value from a server-only query (the lookup of the priorities an agent may
   * set), not from a comparison of two bound parameters, which fails on every database
   */
  @Test
  void agentPriorityIsResolvedByAServerQuery() throws Exception {
    Element target = elementWith("global/Maintain.xml", "target", "name", "HdkTktPrio");
    NodeList fields = target.getElementsByTagName("field");
    assertThat(fields.getLength()).isEqualTo(1);
    assertThat(((Element) fields.item(0)).getAttribute("query")).isEqualTo("HdkPriAgent");
    NodeList filters = target.getElementsByTagName("filter");
    for (int index = 0; index < filters.getLength(); index++) {
      assertThat(((Element) filters.item(index)).hasAttribute("left-variable")).isFalse();
    }
    assertThat(elementWith("global/Queries.xml", "query", "id", "HdkPriAgent").getAttribute("server-only")).isEqualTo("true");
  }

  /**
   * A maintain target that updates many rows ("multiple") cannot use a list variable in an "in" filter: the list is
   * indexed per row and fails from the second state. The workflow targets list their source states as constant "eq"
   * filters inside an "or"
   */
  @Test
  void multipleUpdatesDoNotUseListVariables() throws Exception {
    NodeList targets = parseXml("global/Maintain.xml").getElementsByTagName("target");
    for (int index = 0; index < targets.getLength(); index++) {
      Element target = (Element) targets.item(index);
      NodeList updates = target.getElementsByTagName("update");
      for (int update = 0; update < updates.getLength(); update++) {
        if ("true".equals(((Element) updates.item(update)).getAttribute("multiple"))) {
          NodeList variables = target.getElementsByTagName("variable");
          for (int variable = 0; variable < variables.getLength(); variable++) {
            assertThat(((Element) variables.item(variable)).getAttribute("type"))
              .as("variable of " + target.getAttribute("name")).isNotEqualTo("STRING_TO_LIST");
          }
        }
      }
    }
  }

  private Element elementWith(String file, String tag, String attribute, String value) throws Exception {
    NodeList nodes = parseXml(file).getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      Element element = (Element) nodes.item(index);
      if (value.equals(element.getAttribute(attribute))) {
        return element;
      }
    }
    throw new AssertionError(tag + " " + value + " not found in " + file);
  }

  private Map<String, String> permissionsOf(String file, String tag, String attribute) throws Exception {
    Map<String, String> permissions = new HashMap<>();
    NodeList nodes = parseXml(file).getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      Element element = (Element) nodes.item(index);
      permissions.put(element.getAttribute(attribute), element.getAttribute("permission"));
    }
    return permissions;
  }

  private Set<String> granted(String file) throws Exception {
    NodeList nodes = parseXml(file).getElementsByTagName("permission");
    List<String> ids = new java.util.ArrayList<>();
    for (int index = 0; index < nodes.getLength(); index++) {
      ids.add(((Element) nodes.item(index)).getAttribute("id"));
    }
    return Set.copyOf(ids);
  }

  private org.w3c.dom.Document parseXml(String file) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(moduleDirectory().resolve(file).toFile());
  }
}
