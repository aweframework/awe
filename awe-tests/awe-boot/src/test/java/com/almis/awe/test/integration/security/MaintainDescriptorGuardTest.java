package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the maintain descriptors of the framework against the targets that let a client change the data of another
 * user (issue #886):
 * <ul>
 *   <li>the targets that only Java launches ({@code launchPrivateMaintain}) are marked {@code server-only}, so a client
 *   request for them is refused;</li>
 *   <li>a target a client can launch never takes a user from the request: it reads the user from the session, unless it
 *   is one of the audited exceptions below (admin screens, login flows).</li>
 * </ul>
 * A new target that breaks one of the two fails the build until it is marked or audited.
 */
@Tag("integration")
@DisplayName("Maintain descriptor guard tests")
class MaintainDescriptorGuardTest {

  private static final String SERVER_ONLY_ATTRIBUTE = "server-only";
  private static final String VARIABLE_TAG = "variable";
  private static final String MENU_RESTRICTION_ADMIN = "Menu restriction administration: restrictions of the selected user";

  private static final List<String> FRAMEWORK_MAINTAIN_FILES = List.of(
    "application/awe/global/Maintain.xml",
    "application/awe-notifier/global/Maintain.xml",
    "application/awe-scheduler/global/Maintain.xml",
    "application/awe-developer/global/Maintain.xml",
    "application/awe-tools/global/Maintain.xml");

  /**
   * Targets that the framework only launches from Java with launchPrivateMaintain (verified one by one: no screen, action,
   * service or client code names them).
   */
  private static final Set<String> JAVA_ONLY_TARGETS = Set.of(
    // User security and profile: login lockout, 2FA, OAuth provisioning, password rehash and verified password updates
    "store2faSecret", "UpdPwdLck", "UpdCntLog", "ProvisionNewUser", "UpdateOauthUserProfile", "UsrRehashPwd",
    "ChdPwdExpiredUpdate", "UsrUpdPwdUpdate", "ChdPwdPubUpdate",
    // User settings, favourites, avatar and grid layouts: reached from the client through a service target
    "addUserSettings", "updateUserMode", "updateUserDensity", "addToFavourites", "removeFromFavourites",
    "insertUserAvatar", "updateUserAvatar", "insertGridLayout", "updateGridLayout", "deleteGridLayout",
    // Menu restrictions saved by the menu restriction tree service
    "new-restriction", "update-restriction", "delete-restriction",
    // Notifier: notifications, e-mails and the subscriptions toggled by the notifier service
    "new-notification", "notify-email-users", "insert-user-subscription", "update-user-subscription",
    // Scheduler: file, task and execution log bookkeeping
    "insertFile", "updateFile", "deleteFile", "startTask", "endTask", "updateTaskStatus", "updateInterruptedTasks",
    "updateExecutionTime", "purgeExecutionLogs", "insertExecutionLogLines", "updateExecutionLogLines",
    "purgeExecutionLogLines", "purgeExecutionLogOrigin");

  /**
   * Targets a client can launch that take a user (or the id of a user) from the request, on purpose.
   */
  private static final Map<String, String> REQUEST_USER_EXCEPTIONS = Map.ofEntries(
    Map.entry("UsrNew", "User administration screen: creates the user that the administrator types"),
    Map.entry("UsrUpd", "User administration screen: updates the user selected by id"),
    Map.entry("UsrDel", "User administration screen: deletes the user selected by id"),
    Map.entry("UsrUnl", "User administration screen: unlocks the users selected by id"),
    Map.entry("reset2fa", "User administration screen: resets the 2FA of the users selected by id"),
    Map.entry("ModNew", "Module administration screen: assigns the users selected in the module grid"),
    Map.entry("ModUpd", "Module administration screen: assigns the users selected in the module grid"),
    Map.entry("updateScreenRestriction", "Screen access administration: restrictions of the selected user"),
    Map.entry("updateScreenRestrictionNew", "Screen access administration: restrictions of the selected user"),
    Map.entry("updateScreenConfiguration", "Screen configuration administration: configuration of the selected user"),
    Map.entry("StlCnfMul", "Style configuration administration: configuration of the selected user"),
    Map.entry("allowMenuOption", MENU_RESTRICTION_ADMIN),
    Map.entry("restrictMenuOption", MENU_RESTRICTION_ADMIN),
    Map.entry("removeRestriction", MENU_RESTRICTION_ADMIN),
    Map.entry("ChdPwdPub", "Public login flow: the service checks the current password of the user before it changes it"),
    Map.entry("ChkSchSrvCnx", "Scheduler server connection test: the user is the SSH user of the server, not an AWE user"));

  /**
   * Variables that hold a user of the application or the id of a user
   */
  private static final Pattern USER_VARIABLE = Pattern.compile("(?i)^(user|usr|username|l1_nom|ope|operator|ideope|userid|userids|login)$");

  /**
   * Columns that hold a user of the application or the id of a user
   */
  private static final Pattern USER_COLUMN = Pattern.compile("(?i)^(l1_nom|username|ope|ideope)$");

  @Test
  @DisplayName("The targets that only Java launches are server-only")
  void javaOnlyTargets_areServerOnly() throws Exception {
    Map<String, Element> targets = loadTargets();

    List<String> notServerOnly = new ArrayList<>();
    for (String name : new TreeSet<>(JAVA_ONLY_TARGETS)) {
      Element target = targets.get(name);
      assertThat(target).as("Target %s must exist in a framework descriptor", name).isNotNull();
      if (!"true".equals(target.getAttribute(SERVER_ONLY_ATTRIBUTE))) {
        notServerOnly.add(name);
      }
    }

    assertThat(notServerOnly).as("Java-only targets without server-only=\"true\"").isEmpty();
  }

  @Test
  @DisplayName("A server-only target is not public")
  void serverOnlyTargets_areNotPublic() throws Exception {
    List<String> publicServerOnly = new ArrayList<>();
    loadTargets().forEach((name, target) -> {
      if ("true".equals(target.getAttribute(SERVER_ONLY_ATTRIBUTE)) && "true".equals(target.getAttribute("public"))) {
        publicServerOnly.add(name);
      }
    });

    assertThat(publicServerOnly).as("Targets that are public and server-only").isEmpty();
  }

  @Test
  @DisplayName("A client-launchable target never takes the user from the request, except the audited ones")
  void clientTargets_takeTheUserFromTheSession() throws Exception {
    Set<String> requestUserTargets = new TreeSet<>();
    loadTargets().forEach((name, target) -> {
      if (!"true".equals(target.getAttribute(SERVER_ONLY_ATTRIBUTE)) && takesUserFromRequest(target)) {
        requestUserTargets.add(name);
      }
    });

    Set<String> unaudited = new TreeSet<>(requestUserTargets);
    unaudited.removeAll(REQUEST_USER_EXCEPTIONS.keySet());
    assertThat(unaudited)
      .as("Targets that read a user from the request: read it from the session (session=\"user\"), mark them "
        + "server-only if only Java launches them, or add them to REQUEST_USER_EXCEPTIONS with the reason")
      .isEmpty();

    Set<String> stale = new TreeSet<>(REQUEST_USER_EXCEPTIONS.keySet());
    stale.removeAll(requestUserTargets);
    assertThat(stale).as("Audited exceptions that no longer read a user from the request: remove them").isEmpty();
  }

  /**
   * A target takes a user from the request when a variable that holds a user is read from a request parameter (name),
   * or when a user column is bound to such a variable
   */
  private static boolean takesUserFromRequest(Element target) {
    NodeList variables = target.getElementsByTagName(VARIABLE_TAG);
    for (int i = 0; i < variables.getLength(); i++) {
      Element variable = (Element) variables.item(i);
      if (variable.hasAttribute("name")
        && (USER_VARIABLE.matcher(variable.getAttribute("id")).matches() || USER_VARIABLE.matcher(variable.getAttribute("name")).matches())) {
        return true;
      }
    }
    return boundToRequestVariable(target, "field", "id", VARIABLE_TAG)
      || boundToRequestVariable(target, "filter", "left-field", "right-variable");
  }

  private static boolean boundToRequestVariable(Element target, String tag, String columnAttribute, String variableAttribute) {
    NodeList elements = target.getElementsByTagName(tag);
    for (int i = 0; i < elements.getLength(); i++) {
      Element element = (Element) elements.item(i);
      if (USER_COLUMN.matcher(element.getAttribute(columnAttribute)).matches() && isRequestVariable(element, variableAttribute)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isRequestVariable(Element element, String variableAttribute) {
    String variableId = element.getAttribute(variableAttribute);
    if (variableId.isEmpty()) {
      return false;
    }
    // The variables are children of the maintain query that holds the field or the filter
    for (Node parent = element.getParentNode(); parent instanceof Element query; parent = parent.getParentNode()) {
      NodeList variables = query.getElementsByTagName(VARIABLE_TAG);
      for (int i = 0; i < variables.getLength(); i++) {
        Element variable = (Element) variables.item(i);
        if (variableId.equals(variable.getAttribute("id"))) {
          return variable.hasAttribute("name");
        }
      }
    }
    return false;
  }

  private Map<String, Element> loadTargets() throws Exception {
    Map<String, Element> targets = new LinkedHashMap<>();
    for (String file : FRAMEWORK_MAINTAIN_FILES) {
      try (InputStream stream = getClass().getClassLoader().getResourceAsStream(file)) {
        assertThat(stream).as("Descriptor %s must be on the classpath", file).isNotNull();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder().parse(stream);
        NodeList nodes = document.getElementsByTagName("target");
        for (int i = 0; i < nodes.getLength(); i++) {
          Element target = (Element) nodes.item(i);
          targets.put(target.getAttribute("name"), target);
        }
      }
    }
    return targets;
  }
}
