package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Websocket messages: one flow that sends a message to the test user, invalidates the session, logs in again and sends a
 * message to every user. Every test starts logged in (session setup of {@link AbstractSessionTests}); the login and logout
 * flows are proven by IntegrationTestsIT.
 */
@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebsocketTestsIT extends AbstractSessionTests {

  WebsocketTestsIT() {
    super(Session.LOGGED_IN);
  }

  /**
   * Websocket messages: a message to a user is received, an invalidated session warns the user, a message to the user is
   * received again after logging in (and leaves no info message behind), and a message to all users is sent and clears
   * its text
   */
  @Test
  void t001_sendWebsocketMessages() {
    // Title
    setTestTitle("Websocket messages: send to a user, invalidate the session, log in again and send to all users");

    // Do broadcast test
    broadcastMessageToUser("test", "This is a broadcast message test");

    // Invalidate the session of the user from another window
    invalidateSession();

    // Accept the warning message: it is shown only for a few seconds, so wait for it instead of pausing
    checkAndCloseMessage("warn");

    // Do login
    checkLogin("test", "test", "Manager (test)");

    // Do broadcast test
    broadcastMessageToUser("test", "This is a broadcast message test");

    // Assert there are no info messages
    checkMessageMissing("info");

    // Send a message to all users from the broadcast screen
    gotoScreen("tools", "broadcast-messages");

    // Write on criterion
    writeText("MsgDes", "This is a broadcast message test");

    // Send the message
    clickButton("ButSnd");

    // Accept message
    checkAndCloseMessage("success");

    // Accept message
    checkAndCloseMessage("info");

    // Check message has been deleted
    checkCriterionContents("MsgDes", "");
  }
}
