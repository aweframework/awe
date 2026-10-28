package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.settings.WebChartOptions;
import com.almis.awe.model.settings.WebNumberOptions;
import com.almis.awe.model.settings.WebPivotOptions;
import com.almis.awe.model.settings.WebSettings;
import com.almis.awe.model.settings.WebTooltip;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application default of how the grids show their Columns reaches the clients in the settings (#843)
 */
class WebSettingsGridColumnsPanelTest {

  private static WebSettings webSettings(BaseConfigProperties properties) {
    return new PropertyConfig().webSettings(properties, new SecurityConfigProperties(), new WebNumberOptions(),
      new WebChartOptions(), new WebTooltip(), new WebPivotOptions());
  }

  @Test
  void gridsShowTheirColumnsInAPopoverByDefault() {
    assertThat(new BaseConfigProperties().getComponent().getGridColumnsPanel()).isEqualTo("popover");
    assertThat(webSettings(new BaseConfigProperties()).getGridColumnsPanel()).isEqualTo("popover");
  }

  @Test
  void theApplicationCanStillShowTheColumnsOfItsGridsInADialog() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getComponent().setGridColumnsPanel("dialog");

    assertThat(webSettings(properties).getGridColumnsPanel()).isEqualTo("dialog");
  }

  @Test
  void theApplicationCanShowTheColumnsOfItsGridsInASidePanel() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getComponent().setGridColumnsPanel("side-panel");

    assertThat(webSettings(properties).getGridColumnsPanel()).isEqualTo("side-panel");
  }
}
