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
 * Settings published to the client carry the tile url of the maps (#798)
 */
class PropertyConfigWebSettingsTest {

  private static WebSettings settings(BaseConfigProperties properties) {
    return new PropertyConfig().webSettings(properties, new SecurityConfigProperties(), new WebNumberOptions(),
      new WebChartOptions(), new WebTooltip(), new WebPivotOptions());
  }

  @Test
  void mapTileUrlDefaultsToOpenStreetMap() {
    assertThat(settings(new BaseConfigProperties()).getMapTileUrl())
      .isEqualTo("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
  }

  @Test
  void mapTileUrlComesFromTheProperty() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getMap().setTileUrl("https://tiles.example.com/{z}/{x}/{y}.png");

    assertThat(settings(properties).getMapTileUrl()).isEqualTo("https://tiles.example.com/{z}/{x}/{y}.png");
  }
}
