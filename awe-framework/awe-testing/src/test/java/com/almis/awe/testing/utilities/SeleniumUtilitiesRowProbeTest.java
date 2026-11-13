package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementRef;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.selenium.AngularAweInstructions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The probe of a grid ({@code hasRowContents}): it tells whether a text is in the grid once the grid has loaded, without
 * failing when it is not, so a test step can find out whether the record that it creates or deletes is already there (the
 * state that a failed attempt left when the step is run again).
 */
class SeleniumUtilitiesRowProbeTest {

  private BrowserDriver browser;
  private AngularAweInstructions instructions;
  private SeleniumUtilities utilities;

  @BeforeEach
  void setUp() {
    browser = mock(BrowserDriver.class);
    instructions = new AngularAweInstructions();
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setFrontend(FrontendType.ANGULAR);
    properties.setTimeout(Duration.ofMillis(300));
    SeleniumModel model = new SeleniumModel().setBrowser(browser).setProperties(properties);
    instructions.setSeleniumModel(model);
    utilities = new SeleniumUtilities();
    ReflectionTestUtils.setField(utilities, "properties", properties);
    ReflectionTestUtils.setField(utilities, "seleniumModel", model);
    ReflectionTestUtils.setField(utilities, "frontEndInstructions", instructions);
  }

  private void showCell(String gridId, String text, boolean visible) {
    ElementRef cell = mock(ElementRef.class);
    when(cell.isVisible()).thenReturn(visible);
    when(browser.elements(Locator.from(instructions.findGridCell(gridId, text)))).thenReturn(List.of(cell));
  }

  @Test
  void shouldFindARowThatTheGridShows() {
    showCell(null, "Site alpha", true);

    assertThat(utilities.hasRowContents("Site alpha")).isTrue();
  }

  @Test
  void shouldFindARowOfAGridById() {
    showCell("MdlPrfLst", "TST", true);

    assertThat(utilities.hasRowContentsGrid("MdlPrfLst", "TST")).isTrue();
    assertThat(utilities.hasRowContentsGrid("MdlPrfLst", "ADM")).isFalse();
  }

  @Test
  void shouldNotFailWhenTheGridDoesNotShowTheRow() {
    when(browser.elements(Locator.from(instructions.findGridCell(null, "Site alpha")))).thenReturn(List.of());

    assertThat(utilities.hasRowContents("Site alpha")).isFalse();
  }

  @Test
  void shouldNotCountARowThatIsNotVisible() {
    showCell(null, "Site alpha", false);

    assertThat(utilities.hasRowContents("Site alpha")).isFalse();
  }
}
