package com.almis.awe.model.entities.screen.component.chart;

import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.util.XmlSerializerTestUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test helper that parses the chart test screens with the same XStream setup used at runtime
 */
public final class ChartFixtures {

  private static final XStreamSerializer SERIALIZER = XmlSerializerTestUtil.buildSerializer();

  private ChartFixtures() {
  }

  /**
   * Parse a screen stored in the test resources and return its charts by identifier
   *
   * @param resource Classpath resource of the screen XML
   * @return Charts of the screen, in declaration order
   */
  public static synchronized Map<String, Chart> loadCharts(String resource) {
    try (InputStream stream = ChartFixtures.class.getResourceAsStream(resource)) {
      Screen screen = SERIALIZER.getObjectFromXml(Screen.class, stream);
      Map<String, Chart> charts = new LinkedHashMap<>();
      collect(screen, charts);
      return charts;
    } catch (IOException exc) {
      throw new IllegalStateException("Cannot read " + resource, exc);
    }
  }

  private static void collect(Element element, Map<String, Chart> charts) {
    if (element instanceof Chart chart) {
      charts.put(chart.getId(), chart);
    }
    for (Element child : element.<Element>getElementList()) {
      collect(child, charts);
    }
  }
}
