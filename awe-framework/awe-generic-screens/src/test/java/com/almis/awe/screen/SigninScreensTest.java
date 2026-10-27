package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

  private List<String> ids(Document document, String tag) {
    return elements(document, tag).stream().map(e -> e.getAttribute("id")).toList();
  }

  private List<String> actions(Element button) {
    return elements(button, "button-action").stream()
      .map(a -> String.join(" ", a.getAttribute("type"), a.getAttribute("server-action"), a.getAttribute("target-action"),
        a.getAttribute("target")).trim().replaceAll(" +", " "))
      .toList();
  }

  private List<Element> elements(Element element, String tag) {
    NodeList nodes = element.getElementsByTagName(tag);
    List<Element> list = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      list.add((Element) nodes.item(i));
    }
    return list;
  }

  /**
   * The change password and two-factor screens take the frame and the fields of the sign-in page (S19): the visible
   * fields are labelled above, large, with a leading icon (lock for the passwords) and without placeholders
   */
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"change-password.xml", "ChdPwdPub.xml", "change-expired-password.xml", "check-2fa.xml"})
  @DisplayName("The password and two-factor screens label their fields above, with a leading icon")
  void passwordAndTwoFactorFieldsAsTheSignin(String file) throws Exception {
    Document screen = load(ANGULAR + file);

    List<Element> visible = elements(screen, "criteria").stream()
      .filter(criterion -> !"hidden".equals(criterion.getAttribute("component"))).toList();
    assertThat(visible).isNotEmpty();
    visible.forEach(criterion -> {
      String id = criterion.getAttribute("id");
      assertThat(criterion.getAttribute("label")).as("label of " + id).isNotBlank();
      assertThat(criterion.hasAttribute("placeholder")).as("placeholder of " + id).isFalse();
      assertThat(criterion.getAttribute("style")).as("style of " + id).doesNotContain("no-label");
      assertThat(criterion.getAttribute("size")).as("size of " + id).isEqualTo("lg");
      assertThat(criterion.getAttribute("icon")).as("icon of " + id).isNotBlank();
      if ("password".equals(criterion.getAttribute("component"))) {
        assertThat(criterion.getAttribute("icon")).as("icon of " + id).startsWith("lock ");
      }
    });
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
    "change-password.xml|page-change-password-bg|awe-change-password change-password-container|Usr:user,OldPas:lock,Pas:lock,RetPas:lock",
    "ChdPwdPub.xml|page-change-password-bg|awe-change-password change-password-container|Usr:user,OldPas:lock,Pas:lock,RetPas:lock",
    "change-expired-password.xml|page-change-password-bg|awe-change-password change-password-container|Pas:lock,RetPas:lock",
    "check-2fa.xml|page-totp-bg|awe-totp totp-container|code:key",
    "activate-2fa.xml|page-totp-bg|awe-totp totp-container|"
  })
  @DisplayName("The password and two-factor screens keep the frame of the login pages and their field icons")
  void passwordAndTwoFactorFrame(String file, String panel, String column, String icons) throws Exception {
    Document screen = load(ANGULAR + file);

    assertThat(screen.getDocumentElement().getAttribute("template")).isEqualTo("full");
    assertThat(ids(screen, "tag")).contains(panel);
    assertThat(elements(screen, "tag").stream().map(e -> e.getAttribute("style"))).contains(column);
    if (icons != null) {
      Arrays.stream(icons.split(",")).map(pair -> pair.split(":")).forEach(pair ->
        assertThat(byId(screen, "criteria", pair[0]).getAttribute("icon").split(" ")[0]).as("icon of " + pair[0])
          .isEqualTo(pair[1]));
    }
  }

  /**
   * The buttons of the password and two-factor screens look as the Login button of the sign-in page (S20): text only,
   * without icon
   */
  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
    ANGULAR + "change-password.xml|ButChdPwd",
    ANGULAR + "ChdPwdPub.xml|ButCnf,ButBck",
    ANGULAR + "change-expired-password.xml|ButChdExpPwd,ButBckExpPwd",
    ANGULAR + "check-2fa.xml|ButChk2fa",
    ANGULAR + "activate-2fa.xml|ButChk2fa",
    "application/awe-react/screen/security/change-expired-password.xml|ButChdExpPwd,ButBckExpPwd"
  })
  @DisplayName("The password and two-factor screens show their buttons without icon, as the sign-in")
  void passwordAndTwoFactorButtonsWithoutIcon(String path, String buttons) throws Exception {
    Document screen = load(path);

    assertThat(ids(screen, "button")).containsExactly(buttons.split(","));
    elements(screen, "button").forEach(button ->
      assertThat(button.hasAttribute("icon")).as("icon of " + button.getAttribute("id")).isFalse());
  }

  @Test
  @DisplayName("The public change password screen keeps its ids, password fields and actions")
  void publicChangePasswordKeepsItsBehaviour() throws Exception {
    Document screen = load(ANGULAR + "ChdPwdPub.xml");

    assertThat(screen.getDocumentElement().getAttribute("label")).isEqualTo("SCREEN_TITLE_CHD_PWD");
    assertThat(ids(screen, "criteria")).containsExactlyInAnyOrder("Usr", "cod_usr", "OldPas", "CrtOldPas", "Pas",
      "pwd_usr", "RetPas", "MinPwd");
    assertThat(byId(screen, "criteria", "Usr").getAttribute("readonly")).isEqualTo("true");
    List.of("OldPas", "Pas", "RetPas").forEach(id ->
      assertThat(byId(screen, "criteria", id).getAttribute("component")).as(id).isEqualTo("password"));
    assertThat(byId(screen, "criteria", "Pas").getAttribute("validation")).isEqualTo("{required: true, "
      + "pattern:{setting:'passwordPattern'}, ne:{criterion:'OldPas', message:'VALIDATOR_MESSAGE_REPEAT_OLD_PASSWORD'}, "
      + "minlength:{setting:'minlengthPassword'}}");
    assertThat(ids(screen, "button")).containsExactly("ButCnf", "ButBck");
    assertThat(byId(screen, "button", "ButCnf").getAttribute("button-type")).isEqualTo("submit");
    assertThat(actions(byId(screen, "button", "ButCnf"))).containsExactly("validate", "confirm UpdMsg",
      "server maintain ChdPwdPub", "server login");
    assertThat(actions(byId(screen, "button", "ButBck"))).containsExactly("screen Lgn");
    assertThat(ids(screen, "message")).containsExactly("UpdMsg");
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
