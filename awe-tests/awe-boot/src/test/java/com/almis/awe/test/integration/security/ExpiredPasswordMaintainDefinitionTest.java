package com.almis.awe.test.integration.security;

import com.almis.awe.service.user.PasswordChangeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The user of the expired password update is read from a server-side session parameter. The name is written in the
 * maintain definition and in the service: a rename on one side only must fail the build.
 */
@Tag("integration")
@DisplayName("Expired password maintain definition tests")
class ExpiredPasswordMaintainDefinitionTest {

  @Test
  @DisplayName("The user of the update is read from the session parameter the service sets, never from the request")
  void updateUser_isReadFromTheSessionParameterOfTheService() throws Exception {
    Element target = findTarget(PasswordChangeService.UPDATE_MAINTAIN);
    NodeList variables = target.getElementsByTagName("variable");
    Element userVariable = null;
    for (int i = 0; i < variables.getLength(); i++) {
      Element variable = (Element) variables.item(i);
      if ("l1_nom".equals(variable.getAttribute("id"))) {
        userVariable = variable;
      }
    }

    assertNotNull(userVariable);
    assertEquals(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER, userVariable.getAttribute("session"));
    assertFalse(userVariable.hasAttribute("name"));
    assertFalse(userVariable.hasAttribute("value"));
  }

  @Test
  @DisplayName("The update is not public")
  void update_isNotPublic() throws Exception {
    assertFalse(findTarget(PasswordChangeService.UPDATE_MAINTAIN).hasAttribute("public"));
  }

  private Element findTarget(String name) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("application/awe/global/Maintain.xml")) {
      assertNotNull(stream);
      NodeList targets = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream).getElementsByTagName("target");
      for (int i = 0; i < targets.getLength(); i++) {
        Element target = (Element) targets.item(i);
        if (name.equals(target.getAttribute("name"))) {
          return target;
        }
      }
    }
    throw new AssertionError("Target not found: " + name);
  }
}
