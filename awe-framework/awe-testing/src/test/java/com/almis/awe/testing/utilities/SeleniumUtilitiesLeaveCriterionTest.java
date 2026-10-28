package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Leaving a criterion after typing in it: the click that takes the focus away lands on the criterion itself, not on
 * whatever the page shows above it (a criterion without label row has the step bar of a wizard right above its input)
 */
class SeleniumUtilitiesLeaveCriterionTest {

  private static final Locator CRITERION = Locator.css("[criterion-id='epa']");

  @TempDir
  Path tempDir;

  private BrowserDriver browser;
  private SeleniumUtilities steps;

  @BeforeEach
  void setUp() {
    browser = mock(BrowserDriver.class);
    when(browser.exists(any())).thenReturn(true);
    when(browser.isVisible(any())).thenReturn(true);
    when(browser.isRendered(any())).thenReturn(true);
    when(browser.isEnabled(any())).thenReturn(true);
    when(browser.count(any())).thenReturn(1);
    when(browser.executeScriptOn(eq(CRITERION), anyString())).thenReturn(200.0);

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setStartUrl("http://localhost:8080/");
    properties.setScreenshotPath(tempDir.toString());
    properties.setTimeout(Duration.ofMillis(150));
    SeleniumModel model = new SeleniumModel().setBrowser(browser).setCurrentOption("unit-test").setProperties(properties);

    AngularAweInstructions instructions = new AngularAweInstructions();
    instructions.setSeleniumModel(model);
    steps = new SeleniumUtilities();
    ReflectionTestUtils.setField(steps, "properties", properties);
    ReflectionTestUtils.setField(steps, "seleniumModel", model);
    ReflectionTestUtils.setField(steps, "frontEndInstructions", instructions);
  }

  @Test
  void shouldClickTheEdgeOfTheCriterionToLeaveItAfterTypingInIt() {
    steps.writeText("epa", "aaa");

    // From the center of a 200 px wide criterion to 2 px from its left edge, inside the gutter of its column
    InOrder order = inOrder(browser);
    order.verify(browser).type(any(), eq("aaa"));
    order.verify(browser).hover(CRITERION);
    order.verify(browser).moveMouseBy(-98, 0);
    order.verify(browser).clickAtPointer();
  }

  @Test
  void shouldNotClickWhatThePageShowsAboveTheCriterion() {
    steps.writeText("epa", "aaa");

    verify(browser, never()).moveMouseBy(anyInt(), eq(-30));
  }
}
