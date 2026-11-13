package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base of the browser test classes that need the test user: before every test it leaves the browser logged in as the test
 * user (and in the Test module, when the class asks for it), and it does nothing when the browser already is. A test does
 * not depend on the login or the module selection of another test or class, so a failing test does not leave the next ones
 * without a session. The tests of a create, update and delete sequence on the same record of a class are still a chain
 * ordered by their names.
 *
 * <p>The class chooses the state that its tests start from in the constructor. A test that checks the login or the module
 * selection itself starts from an earlier state, which it declares with {@link StartsFrom}.</p>
 */
abstract class AbstractSessionTests extends SeleniumUtilities {

  private static final String USER = "test";
  private static final String PASSWORD = "test";
  private static final String USER_NAME = "Manager (test)";
  private static final String TEST_MODULE = "Test";
  private static final String TEST_MODULE_MENU = "test";

  /**
   * What the browser shows before a test
   */
  enum Session {
    /**
     * The login screen: nobody is logged in, whatever the browser showed before (the setup logs out when a user is shown)
     */
    BLANK,
    /**
     * The test user is logged in
     */
    LOGGED_IN,
    /**
     * The test user is logged in and the Test module is selected
     */
    TEST_MODULE
  }

  /**
   * State that a test starts from, when it is not the one of its class
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface StartsFrom {
    Session value();
  }

  private final Session classSession;

  /**
   * @param classSession State that the tests of the class start from
   */
  protected AbstractSessionTests(Session classSession) {
    this.classSession = classSession;
  }

  @BeforeEach
  void prepareSession(TestInfo testInfo) {
    Session session = testInfo.getTestMethod()
      .map(method -> method.getAnnotation(StartsFrom.class))
      .map(StartsFrom::value)
      .orElse(classSession);

    if (session == Session.BLANK) {
      ensureLoggedOut();
      return;
    }
    ensureLoggedIn(USER, PASSWORD, USER_NAME);
    if (session == Session.TEST_MODULE) {
      ensureModule(TEST_MODULE, TEST_MODULE_MENU);
    }
  }
}
