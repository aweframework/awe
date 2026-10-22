package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The map screen element is parsed into a GeoMap and published to the client with its attributes (#798)
 */
class GeoMapTest {

  private static final String SCREEN = """
    <screen template="window" label="Maps">
      <tag source="center">
        <map id="fleet" initial-load="query" target-action="TstMapPoints"
             latitude="Lat" longitude="Lon" marker-label="Lug" zoom="4"
             center-latitude="43.86" center-longitude="1.11" style="expand">
          <dependency initial="true" server-action="data" target-action="Other">
            <dependency-element id="fleet" condition="eq" value="1"/>
          </dependency>
        </map>
      </tag>
    </screen>
    """;

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void mapElementIsParsedIntoGeoMap() {
    GeoMap map = parsedMap();

    assertThat(map.getId()).isEqualTo("fleet");
    assertThat(map.getInitialLoad()).isEqualTo("query");
    assertThat(map.getTargetAction()).isEqualTo("TstMapPoints");
    assertThat(map.getStyle()).isEqualTo("expand");
    assertThat(map.getLatitude()).isEqualTo("Lat");
    assertThat(map.getLongitude()).isEqualTo("Lon");
    assertThat(map.getMarkerLabel()).isEqualTo("Lug");
    assertThat(map.getZoom()).isEqualTo(4);
    assertThat(map.getCenterLatitude()).isEqualTo(43.86);
    assertThat(map.getCenterLongitude()).isEqualTo(1.11);
    assertThat(map.getChildrenByType(Dependency.class)).hasSize(1);
  }

  @Test
  void componentTagIsNotTheLegacyMapDirective() {
    assertThat(new GeoMap().getComponentTag()).isEqualTo("geo-map").isNotEqualTo("map");
  }

  @Test
  void copyKeepsAttributesAndDependencies() {
    GeoMap map = parsedMap();
    GeoMap copy = map.copy();

    assertThat(copy).isNotSameAs(map);
    assertThat(copy.getMarkerLabel()).isEqualTo("Lug");
    assertThat(copy.getZoom()).isEqualTo(4);
    assertThat(copy.getChildrenByType(Dependency.class)).hasSize(1);
  }

  @Test
  void attributesArePublishedToTheClient() {
    // Screens are copied before being sent to the client, which also sets the element type
    JsonNode json = mapper.valueToTree(parsedMap().copy());

    assertThat(json.get("elementType").asString()).isEqualTo("GeoMap");
    assertThat(json.get("latitude").asString()).isEqualTo("Lat");
    assertThat(json.get("longitude").asString()).isEqualTo("Lon");
    assertThat(json.get("markerLabel").asString()).isEqualTo("Lug");
    assertThat(json.get("zoom").asInt()).isEqualTo(4);
    assertThat(json.get("centerLatitude").asDouble()).isEqualTo(43.86);
    assertThat(json.get("centerLongitude").asDouble()).isEqualTo(1.11);
    assertThat(json.get("dependencies")).hasSize(1);
  }

  @Test
  void unsetAttributesAreNotPublished() {
    JsonNode json = mapper.valueToTree(new GeoMap());

    assertThat(json.has("latitude")).isFalse();
    assertThat(json.has("zoom")).isFalse();
    assertThat(json.has("centerLatitude")).isFalse();
  }

  private static GeoMap parsedMap() {
    Screen screen = XmlSerializerTestUtil.buildSerializer().getObjectFromTemplate(Screen.class, SCREEN);
    assertThat(screen).as("parsed screen").isNotNull();
    Element tag = screen.<Element>getElementList().get(0);
    return (GeoMap) tag.<Element>getElementList().get(0);
  }
}
