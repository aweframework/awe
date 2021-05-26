package com.almis.awe.test.selenium;

import com.almis.awe.testing.utilities.SeleniumUtilities;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.JavascriptExecutor;

@Tag("RegressionWebsocketPrintIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class WebsocketTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   */
  @Test
  void t000_loginTest() {
    checkLogin("test", "test", "#ButUsrAct span.info-text", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout(".slogan", "Almis Web Engine");
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

    String a = "var winNew = window.open('" + getBaseUrl() + "session/invalidate','_blank', 'width=1, height=1');setTimeout(function(){ winNew.close();}, 1000);";
    ((JavascriptExecutor) getDriver()).executeScript(a);

    // Pause 5 seconds
    pause(5000);

    // Go to broadcast screen
    gotoScreen("tools", "sites");

    // Accept danger message
    checkAndCloseMessage("danger");

    // Do login
    checkLogin("test", "test", "#ButUsrAct span.info-text", "Manager (test)");

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
