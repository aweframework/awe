package com.almis.awe.test.integration.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Structural assertion (Slice 6, task 37) that {@code settings.xml}'s {@code profile-panel}
 * contains the avatar uploader criterion and its confirm button, wired to the
 * {@code saveUserAvatar} maintain target. This is a pure XML-structure check (no Spring context
 * needed): the {@code profile-panel} tag is shared by both the base and notifier
 * {@code user-settings.xml} windows via {@code <include target-screen="settings" .../>}, so
 * asserting the shared source screen is sufficient to cover both windows.
 */
@Tag("integration")
@DisplayName("Settings screen avatar uploader structure tests")
class SettingsScreenAvatarUploaderTest {

  private static final String SETTINGS_SCREEN_RESOURCE = "application/awe/screen/users/settings.xml";

  private Document loadSettingsScreen() throws Exception {
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SETTINGS_SCREEN_RESOURCE)) {
      assertNotNull(inputStream, "settings.xml must be present on the classpath (awe-generic-screens)");
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(inputStream);
    }
  }

  private Element findProfilePanel(Document document) {
    NodeList tags = document.getElementsByTagName("tag");
    for (int i = 0; i < tags.getLength(); i++) {
      Element tag = (Element) tags.item(i);
      if ("profile-panel".equals(tag.getAttribute("source"))) {
        return tag;
      }
    }
    fail("profile-panel tag not found in settings.xml");
    return null;
  }

  @Test
  void profilePanelContainsAvatarUploaderCriterion() throws Exception {
    Document document = loadSettingsScreen();
    Element profilePanel = findProfilePanel(document);

    NodeList criteria = profilePanel.getElementsByTagName("criteria");
    boolean found = false;
    for (int i = 0; i < criteria.getLength(); i++) {
      Element criterion = (Element) criteria.item(i);
      if ("CrtAvatar".equals(criterion.getAttribute("id"))) {
        assertEquals("uploader", criterion.getAttribute("component"));
        assertEquals("avatar", criterion.getAttribute("destination"));
        found = true;
      }
    }
    assertTrue(found, "profile-panel must contain a criteria with id=CrtAvatar, component=uploader, destination=avatar");
  }

  @Test
  void profilePanelContainsConfirmButtonWiredToSaveUserAvatarMaintain() throws Exception {
    Document document = loadSettingsScreen();
    Element profilePanel = findProfilePanel(document);

    NodeList buttons = profilePanel.getElementsByTagName("button");
    boolean found = false;
    for (int i = 0; i < buttons.getLength(); i++) {
      Element button = (Element) buttons.item(i);
      if ("updateAvatar".equals(button.getAttribute("id"))) {
        NodeList buttonActions = button.getElementsByTagName("button-action");
        boolean wiredToSaveUserAvatar = false;
        for (int j = 0; j < buttonActions.getLength(); j++) {
          Element action = (Element) buttonActions.item(j);
          // Accept maintain or maintain-silent (the button uses maintain-silent to suppress the success popup)
          if (action.getAttribute("server-action").startsWith("maintain") && "saveUserAvatar".equals(action.getAttribute("target-action"))) {
            wiredToSaveUserAvatar = true;
          }
        }
        assertTrue(wiredToSaveUserAvatar, "updateAvatar button must have a maintain(-silent) server-action wired to target-action=saveUserAvatar");
        found = true;
      }
    }
    assertTrue(found, "profile-panel must contain a confirm button with id=updateAvatar");
  }
}
