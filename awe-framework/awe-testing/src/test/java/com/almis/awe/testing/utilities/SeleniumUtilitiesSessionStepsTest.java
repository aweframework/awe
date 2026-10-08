package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.ReactAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The steps that leave the browser in a known session state ({@code ensureLoggedIn}, {@code ensureLoggedOut} and
 * {@code ensureModule}): they probe the page without waiting and only act when the state is not the one asked for.
 */
class SeleniumUtilitiesSessionStepsTest {

  private static final String USER_NAME = "Manager (test)";

  private BrowserDriver browser;
  private IAweFrontEndInstructions instructions;
  private RecordingSessionUtilities utilities;
  private Locator loggedUser;
  private Locator menuOption;

  @BeforeEach
  void setUp() {
    browser = mock(BrowserDriver.class);
    use(new AngularAweInstructions());
  }

  private void use(IAweFrontEndInstructions frontEndInstructions) {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setTimeout(Duration.ofMillis(300));
    SeleniumModel model = new SeleniumModel().setBrowser(browser).setProperties(properties);
    frontEndInstructions.setSeleniumModel(model);
    instructions = frontEndInstructions;
    utilities = new RecordingSessionUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", frontEndInstructions);
    loggedUser = Locator.from(instructions.getLoggedUser());
    menuOption = Locator.from(instructions.getMenuOptionItem("test"));
  }

  private void showLoggedUser(String text) {
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenReturn(text);
  }

  @Test
  void shouldNotLogInAgainWhenTheUserIsAlreadyLoggedIn() {
    showLoggedUser(USER_NAME);

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).isEmpty();
  }

  @Test
  void shouldLogInWhenNobodyIsLoggedIn() {
    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("login:test");
  }

  @Test
  void shouldLogOutAndLogInAgainWhenAnotherUserIsLoggedIn() {
    showLoggedUser("Other (user)");

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("logout", "login:test");
  }

  @Test
  void shouldAcceptTheConfirmationOfTheLogoutWhenTheApplicationAsksForIt() {
    use(new ReactAweInstructions());
    showLoggedUser("Other (user)");

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("logout-confirm", "login:test");
  }

  @Test
  void shouldCompareTheShownUserExactlyAndNotBySubstring() {
    showLoggedUser(USER_NAME);

    utilities.ensureLoggedIn("test", "test", "test");

    assertThat(utilities.events).containsExactly("logout", "login:test");
  }

  @Test
  void shouldIgnoreTheSpacesAroundTheShownUser() {
    showLoggedUser("  " + USER_NAME + " \n");

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).isEmpty();
  }

  @Test
  void shouldLogInWhenThePageChangesWhileTheUserIsRead() {
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenThrow(new ElementReplacedException(loggedUser, new RuntimeException("replaced")));

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("login:test");
  }

  @Test
  void shouldLogInWhenTheUserIsNotThereWhileItIsRead() {
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenThrow(new ElementNotFoundException(loggedUser, new RuntimeException("missing")));

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("login:test");
  }

  @Test
  void shouldNotLogOutWhenNoUserIsShown() {
    // The login screen, or a page that is not the application: the logged user is not visible
    when(browser.isVisible(loggedUser)).thenReturn(false);

    utilities.ensureLoggedOut();

    assertThat(utilities.events).isEmpty();
    verify(browser, never()).text(loggedUser);
  }

  @Test
  void shouldWaitForTheNameOfTheLoggedUserThatIsNotFilledYet() {
    // The avatar is drawn first and its name arrives later
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenReturn("", "", USER_NAME);

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).isEmpty();
  }

  @Test
  void shouldLogInWithoutLoggingOutWhenTheNameOfTheLoggedUserNeverArrives() {
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenReturn("");

    utilities.ensureLoggedIn("test", "test", USER_NAME);

    assertThat(utilities.events).containsExactly("login:test");
  }

  @Test
  void shouldNotLogOutWhenTheNameOfTheLoggedUserNeverArrives() {
    when(browser.isVisible(loggedUser)).thenReturn(true);
    when(browser.text(loggedUser)).thenReturn("   ");

    utilities.ensureLoggedOut();

    assertThat(utilities.events).isEmpty();
  }

  @Test
  void shouldLogOutWhenAUserIsLoggedIn() {
    showLoggedUser(USER_NAME);

    utilities.ensureLoggedOut();

    assertThat(utilities.events).containsExactly("logout");
  }

  @Test
  void shouldLogOutWithTheConfirmationOfTheApplicationThatAsksForIt() {
    use(new ReactAweInstructions());
    showLoggedUser(USER_NAME);

    utilities.ensureLoggedOut();

    assertThat(utilities.events).containsExactly("logout-confirm");
  }

  @Test
  void shouldNotSelectTheModuleAgainWhenItsMenuIsAlreadyThere() {
    when(browser.isVisible(menuOption)).thenReturn(true);

    utilities.ensureModule("Test", "test");

    assertThat(utilities.events).isEmpty();
  }

  @Test
  void shouldSelectTheModuleWhenItsMenuIsNotThere() {
    when(browser.isVisible(menuOption)).thenReturn(false, true);

    utilities.ensureModule("Test", "test");

    assertThat(utilities.events).containsExactly("module:Test");
  }

  @Test
  void shouldSelectTheModuleWhenThePageChangesWhileItsMenuIsLookedFor() {
    when(browser.isVisible(any(Locator.class))).thenThrow(new ElementReplacedException(menuOption, new RuntimeException("replaced")))
      .thenReturn(true);

    utilities.ensureModule("Test", "test");

    assertThat(utilities.events).containsExactly("module:Test");
  }

  @Test
  void shouldSelectTheModuleWhenItsMenuIsNotThereWhileItIsLookedFor() {
    when(browser.isVisible(any(Locator.class))).thenThrow(new ElementNotFoundException(menuOption, new RuntimeException("missing")))
      .thenReturn(true);

    utilities.ensureModule("Test", "test");

    assertThat(utilities.events).containsExactly("module:Test");
  }

  private static class RecordingSessionUtilities extends SeleniumUtilities {
    private final List<String> events = new ArrayList<>();

    @Override
    protected void checkLogin(String username, String password, String userName) {
      events.add("login:" + username);
    }

    @Override
    protected void checkLogout() {
      events.add("logout");
    }

    @Override
    protected void checkLogoutWithConfirmation() {
      events.add("logout-confirm");
    }

    @Override
    protected void selectModule(String moduleName) {
      events.add("module:" + moduleName);
    }

    @Override
    protected void checkVisible(Locator selector) {
      // The menu of the module is shown once it is selected
    }
  }
}
