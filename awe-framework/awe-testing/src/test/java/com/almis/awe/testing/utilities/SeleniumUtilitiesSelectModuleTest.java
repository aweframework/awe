package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code selectModule} closes the settings panel with its toggle button only while the panel is still open: the AngularJS
 * panel may already be closed by the click on the module option (Firefox), and a second toggle would open it again.
 */
class SeleniumUtilitiesSelectModuleTest {

  private BrowserDriver browser;
  private RecordingUtilities utilities;
  private Locator module;

  @BeforeEach
  void setUp() {
    browser = mock(BrowserDriver.class);
    IAweFrontEndInstructions instructions = new AngularAweInstructions();
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setTimeout(Duration.ofMillis(300));
    SeleniumModel model = new SeleniumModel().setBrowser(browser).setProperties(properties);
    instructions.setSeleniumModel(model);
    utilities = new RecordingUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", instructions);
    module = Locator.from(instructions.getCriterion("module"));
  }

  @Test
  void shouldNotToggleThePanelAgainWhenChoosingTheModuleClosedIt() {
    when(browser.isRendered(module)).thenReturn(false);

    utilities.selectModule("Test");

    assertThat(utilities.steps).containsExactly("toggle ButSetTog", "select module Test", "loading bar");
  }

  @Test
  void shouldCloseThePanelWithItsButtonWhenItIsStillOpen() {
    when(browser.isRendered(module)).thenAnswer(invocation -> utilities.steps.size() < 4);
    when(browser.isVisible(module)).thenAnswer(invocation -> utilities.steps.size() < 4);

    utilities.selectModule("Test");

    assertThat(utilities.steps).containsExactly("toggle ButSetTog", "select module Test", "loading bar", "toggle ButSetTog");
  }

  private static class RecordingUtilities extends SeleniumUtilities {
    private final List<String> steps = new ArrayList<>();

    @Override
    protected void clickInfoButton(String infoButtonName) {
      steps.add("toggle " + infoButtonName);
    }

    @Override
    protected void selectContain(String criterionName, String label) {
      steps.add("select " + criterionName + " " + label);
    }

    @Override
    protected void waitForLoadingBar() {
      steps.add("loading bar");
    }
  }
}
