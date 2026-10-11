package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Both clients (AngularJS and React) render the sign-in screens from these XML definitions, so the hide logic of the
 * local login form is checked here, for each of them.
 */
@DisplayName("Sign-in screens")
class SigninScreensTest {

  private static final String ANGULAR = "application/awe/screen/security/";
  private static final String REACT = "application/awe-react/screen/";

  private Document load(String path) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  private List<Element> elements(Document document, String tag) {
    NodeList nodes = document.getElementsByTagName(tag);
    List<Element> list = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      list.add((Element) nodes.item(i));
    }
    return list;
  }

  private Element byId(Document document, String tag, String id) {
    return elements(document, tag).stream().filter(e -> id.equals(e.getAttribute("id"))).findFirst()
      .orElseThrow(() -> new AssertionError(tag + " " + id + " not found"));
  }

  private void assertHidesWhenLocalLoginIsOffUnderSso(Element element) {
    List<Element> dependencies = new ArrayList<>();
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element child && "dependency".equals(child.getTagName())) {
        dependencies.add(child);
      }
    }
    assertThat(dependencies).as("dependencies of " + element.getAttribute("id")).hasSize(1);
    Element dependency = dependencies.get(0);
    assertThat(dependency.getAttribute("target-type")).isEqualTo("hide");
    assertThat(dependency.getAttribute("type")).isEqualTo("and");
    List<String> conditions = new ArrayList<>();
    NodeList inner = dependency.getElementsByTagName("dependency-element");
    for (int i = 0; i < inner.getLength(); i++) {
      Element condition = (Element) inner.item(i);
      conditions.add(condition.getAttribute("id") + " " + condition.getAttribute("condition") + " " + condition.getAttribute("value"));
    }
    assertThat(conditions).containsExactly("local-login eq false", "sso-master-enabled eq true");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {ANGULAR + "signin.xml", REACT + "signin.xml"})
  @DisplayName("The main sign-in screen hides the local form only when SSO is on and local login is off")
  void mainScreenHidesTheLocalForm(String path) throws Exception {
    Document screen = load(path);

    assertHidesWhenLocalLoginIsOffUnderSso(byId(screen, "criteria", "cod_usr"));
    assertHidesWhenLocalLoginIsOffUnderSso(byId(screen, "criteria", "pwd_usr"));
    assertHidesWhenLocalLoginIsOffUnderSso(byId(screen, "button", "ButLogIn"));
    assertThat(byId(screen, "criteria", "local-login").getAttribute("property")).isEqualTo("awe.security.sso.local-login");
    assertThat(byId(screen, "criteria", "sso-master-enabled").getAttribute("property")).isEqualTo("awe.security.sso.enabled");
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {ANGULAR + "signin-local.xml", REACT + "signin-local.xml"})
  @DisplayName("The local sign-in screen always shows the local form")
  void localScreenAlwaysShowsTheLocalForm(String path) throws Exception {
    Document screen = load(path);

    assertThat(byId(screen, "criteria", "cod_usr")).isNotNull();
    assertThat(byId(screen, "criteria", "pwd_usr")).isNotNull();
    assertThat(byId(screen, "button", "ButLogIn")).isNotNull();
    assertThat(elements(screen, "dependency")).isEmpty();
  }
}
