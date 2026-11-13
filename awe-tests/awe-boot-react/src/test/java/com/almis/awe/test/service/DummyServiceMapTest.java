package com.almis.awe.test.service;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data of the map test screen: the points and the route are fixed, so the browser tests always see the same map
 */
// The surefire run of this module (Unit · Java) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class DummyServiceMapTest {

  private final DummyService service = new DummyService(null, null);

  @Test
  void shouldReturnFourFixedPointsWithTheirCoordinatesAndNames() {
    ServiceData data = service.mapPoints();

    assertThat(data.getDataList().getRows()).hasSize(4);
    Map<String, CellData> first = data.getDataList().getRows().get(0);
    assertThat(first.get("id").getStringValue()).isEqualTo("1");
    assertThat(first.get("Lug").getStringValue()).isEqualTo("Madrid");
    assertThat(first.get("Lat").getDoubleValue()).isEqualTo(40.4168);
    assertThat(first.get("Lon").getDoubleValue()).isEqualTo(-3.7038);
  }

  @Test
  void shouldReturnAPolylineActionWithTheRouteRowsForTheMap() {
    ServiceData data = service.mapRoute();

    assertThat(data.getClientActionList()).hasSize(1);
    ClientAction action = data.getClientActionList().get(0);
    assertThat(action.getType()).isEqualTo("polyline");
    assertThat(action.getTarget()).isEqualTo("MapTst");
    assertThat((List<?>) action.getParameters().get("rows")).hasSize(4);
  }
}
