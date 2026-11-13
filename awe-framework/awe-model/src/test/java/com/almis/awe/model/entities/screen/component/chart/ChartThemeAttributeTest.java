package com.almis.awe.model.entities.screen.component.chart;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pins how XStream parses a screen whose chart still has the removed {@code theme} attribute:
 * the attribute is silently ignored, and the chart is the same as without it
 */
class ChartThemeAttributeTest {

  private static final String SCREEN = """
    <screen template="window" label="Charts">
      <tag source="center">
        <chart id="ChrTheme" label="Chart" type="line" stock-chart="true"%s>
          <chart-serie id="serie" x-value="x" y-value="y" label="Serie"/>
        </chart>
      </tag>
    </screen>
    """;

  private static final String WITH_THEME = SCREEN.formatted(" theme=\"blue\"");
  private static final String WITHOUT_THEME = SCREEN.formatted("");

  @Test
  void rawXStreamDoesNotFailOnThemeAttribute() {
    XStreamMarshaller marshaller = XmlSerializerTestUtil.buildMarshaller();

    assertThatCode(() -> marshaller.unmarshal(new StreamSource(new StringReader(WITH_THEME))))
      .doesNotThrowAnyException();
  }

  @Test
  void themeAttributeIsIgnoredWhenParsingTheScreen() {
    Chart withTheme = chartOf(WITH_THEME);
    Chart withoutTheme = chartOf(WITHOUT_THEME);

    assertThat(withTheme.getId()).isEqualTo("ChrTheme");
    assertThat(withTheme.getType()).isEqualTo("line");
    assertThat(withTheme.isStockChart()).isTrue();
    assertThat(withTheme).isEqualTo(withoutTheme);
    assertThat(withTheme.getChartModel()).doesNotContainKey("theme").isEqualTo(withoutTheme.getChartModel());
    assertThat(withTheme.getEchartsModel()).isEqualTo(withoutTheme.getEchartsModel());
    assertThat((Map<String, Object>) withTheme.getEchartsModel().get("awe")).doesNotContainKey("theme");
  }

  private static Chart chartOf(String xml) {
    Screen screen = XmlSerializerTestUtil.buildSerializer().getObjectFromTemplate(Screen.class, xml);
    assertThat(screen).as("parsed screen").isNotNull();
    Element tag = screen.<Element>getElementList().get(0);
    return (Chart) tag.<Element>getElementList().get(0);
  }
}
