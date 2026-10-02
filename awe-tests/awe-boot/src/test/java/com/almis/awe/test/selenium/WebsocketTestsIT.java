package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebsocketTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   */
  @Test
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout();
  }

  /**
   * Websocket message send test
   */
  @Test
  void t001_checkWebsocketMessageSend() {
    // Title
    setTestTitle("Websocket message send test");

    // Do broadcast test
    broadcastMessageToUser("test", "This is a broadcast message test");

    // Invalidate the session of the user from another window
    invalidateSession();

    // Pause 5 seconds
    pause(5000);

    // Accept danger message
    checkAndCloseMessage("warn");

    // Do login
    checkLogin("test", "test", "Manager (test)");

    // Do broadcast test
    broadcastMessageToUser("test", "This is a broadcast message test");

    // Assert there are no info messages
    checkMessageMissing("info");
  }

  /**
   * Send websocket message to all users
   */
  @Test
  void t002_sendWebsocketMessageToAllUsers() {
    // Title
    setTestTitle("Send websocket message to all users");

    // Go to broadcast screen
    gotoScreen("tools", "broadcast-messages");

    // Write on criterion
    writeText("MsgDes", "This is a broadcast message test");

    // Search and wait
    clickButton("ButSnd");

    // Accept message
    checkAndCloseMessage("success");

    // Accept message
    checkAndCloseMessage("info");

    // Check message has been deleted
    checkCriterionContents("MsgDes", "");
  }
}
