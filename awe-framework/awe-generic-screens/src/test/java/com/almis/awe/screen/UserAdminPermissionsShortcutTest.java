package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static com.almis.awe.screen.AccessAdminDescriptor.AWE;
import static com.almis.awe.screen.AccessAdminDescriptor.SCREEN;
import static com.almis.awe.screen.AccessAdminDescriptor.actions;
import static com.almis.awe.screen.AccessAdminDescriptor.attributes;
import static com.almis.awe.screen.AccessAdminDescriptor.byId;
import static com.almis.awe.screen.AccessAdminDescriptor.elements;
import static com.almis.awe.screen.AccessAdminDescriptor.load;
import static com.almis.awe.screen.AccessAdminDescriptor.showWhen;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * View permissions of the users screen (user-admin, design/user-permissions/04): the row menu and the user panel open
 * the access window on its Permissions view with the user, through the variables of access-admin. Both declare the
 * permissions of that view, so a user without them does not get them.
 */
@DisplayName("Users screen: View permissions shortcut")
class UserAdminPermissionsShortcutTest {

  private static final String USERS = "screen/users/user-admin.xml";
  private static final String GRID = "GrdUsrAdm";
  private static final String PERMISSION = "users.read permissions.read";

  @Test
  @DisplayName("The access window reads its view and its user from the variables sent by another screen")
  void accessWindowReadsItsViewAndUser() throws Exception {
    Document access = load(AWE + SCREEN);

    assertThat(byId(access, "access-view").getAttribute("variable")).isEqualTo("access-view");
    assertThat(byId(access, "IdeOpe").getAttribute("variable")).isEqualTo("access-user");
  }

  @Test
  @DisplayName("The row menu opens Access control › Permissions with the selected user, second after Edit")
  void rowMenu() throws Exception {
    Document users = load(AWE + USERS);
    Element shortcut = byId(users, "CtxButPer");

    assertThat(shortcut.getAttribute("permission")).isEqualTo(PERMISSION);
    assertThat(elements(byId(users, GRID), "context-button")).extracting(button -> button.getAttribute("id"))
      .startsWith("CtxButUpd", "CtxButPer");
    assertThat(actions(shortcut)).containsExactly("check-one-selected:" + GRID, "value:access-view",
      "screen:screen-access");
    assertThat(elements(shortcut, "button-action").get(1).getAttribute("value")).isEqualTo("permissions");
    // The user sent is the one of the selected row
    Element user = byId(users, "access-user");
    assertThat(user.getAttribute("component")).isEqualTo("hidden");
    assertThat(elements(user, "dependency-element")).extracting(element -> element.getAttribute("column"))
      .contains("IdeOpe");
    assertThat(byId(users, "access-view").getAttribute("value")).isEmpty();
  }

  @Test
  @DisplayName("The user panel shows View permissions and its help when editing, disabled while the main profile is changed")
  void userPanel() throws Exception {
    Document users = load(AWE + USERS);
    Element shortcut = byId(users, "ButPnlPer");

    assertThat(shortcut.getAttribute("permission")).isEqualTo(PERMISSION);
    assertThat(actions(shortcut)).containsExactly("value:access-view", "screen:screen-access");
    assertThat(showWhen(shortcut, "show", "PnlMod")).isEqualTo("eq edit");
    assertThat(elements(shortcut, "dependency")).filteredOn(dependency -> "disable".equals(dependency.getAttribute("target-type")))
      .singleElement()
      .satisfies(dependency -> assertThat(elements(dependency, "dependency-element"))
        .extracting(element -> List.of(element.getAttribute("id"), element.getAttribute("condition"),
          element.getAttribute("id2"), element.getAttribute("column2"), element.getAttribute("attribute2")))
        .contains(List.of("Pro", "ne", GRID, "Pro", "selectedRowValue")));
    // A static help under it, shown with it: the changes of the panel are not in Permissions until they are saved
    Element help = byId(users, "UsrPerHlp");
    assertThat(help.getAttribute("label")).isEqualTo("SCREEN_TEXT_USER_VIEW_PERMISSIONS_HELP");
    assertThat(help.getParentNode()).isSameAs(shortcut.getParentNode());
    assertThat(elements(shortcut, "dependency-action")).extracting(action -> action.getAttribute("target") + " "
      + action.getAttribute("type")).containsExactlyInAnyOrder("#UsrPerHlp remove-class", "#UsrPerHlp add-class");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("The texts of the shortcut are translated")
  void textsAreTranslated(String locale) throws Exception {
    assertThat(attributes(elements(load(AWE + "locale/" + locale).getDocumentElement(), "locale"), "name"))
      .contains("BUTTON_VIEW_PERMISSIONS", "HELP_VIEW_PERMISSIONS_BUTTON", "SCREEN_TEXT_USER_VIEW_PERMISSIONS_HELP");
  }
}
