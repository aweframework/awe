package com.almis.awe.samples.fleetlogistics.map;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The map services draw the routes with the rows of AWE queries. They never read the points themselves, so the guard
 * of the query (code AND postal code of the shipment, permission of the staff) is the only way to get a route
 */
class FleetMapServiceTest {

  private final QueryService queryService = mock(QueryService.class);
  private final FleetMapService service = new FleetMapService(queryService);

  @Test
  void showsTheNumberedPointsOfAShipmentOnTheMapOfTheTrackingScreenWithoutDrawingALine() throws AWException {
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(stops(
      List.of("1", "2", "3"), List.of("Hub Bilbao", "Central hub, Madrid", "Delivery, Vigo"),
      List.of("43.263000", "40.416800", "42.2406"), List.of("-2.935000", "-3.703800", "-8.7207")));

    ServiceData data = service.trackRoute("MF-T9JSDP6N", "36919");

    assertThat(data.getClientActionList()).hasSize(1);
    ClientAction action = data.getClientActionList().get(0);
    assertThat(action.getType()).isEqualTo("fill");
    assertThat(action.getTarget()).isEqualTo("MapFltTrack");
    DataList markers = markers(action);
    assertThat(markers.getRows()).hasSize(3);
    assertThat(DataListUtil.getData(markers, 0, "Seq")).isEqualTo("1");
    assertThat(DataListUtil.getData(markers, 1, "Loc")).isEqualTo("Central hub, Madrid");
    assertThat(DataListUtil.getData(markers, 2, "Lat")).isEqualTo("42.2406");
    assertThat(DataListUtil.hasColumn(markers, "id")).isTrue();
  }

  @Test
  void sendsTheCodeAndThePostalCodeToTheGuardedQuery() throws AWException {
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(points(List.of(), List.of()));

    service.trackRoute("MF-T9JSDP6N", "36919");

    org.mockito.ArgumentCaptor<ObjectNode> parameters = org.mockito.ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(org.mockito.ArgumentMatchers.eq("FltTrackPts"), parameters.capture());
    assertThat(parameters.getValue().get("TrkCod").asText()).isEqualTo("MF-T9JSDP6N");
    assertThat(parameters.getValue().get("TrkPst").asText()).isEqualTo("36919");
  }

  @Test
  void showsNoPointsWhenTheCodeAndThePostalCodeDoNotMatchAnyShipment() throws AWException {
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(points(List.of(), List.of()));

    ServiceData data = service.trackRoute("MF-T9JSDP6N", "00000");

    assertThat(markers(data.getClientActionList().get(0)).getRows()).isEmpty();
  }

  @Test
  void doesNotLaunchTheQueryWithoutTheCodeOrWithoutThePostalCode() throws AWException {
    assertThat(markers(service.trackRoute(null, "36919").getClientActionList().get(0)).getRows()).isEmpty();
    assertThat(markers(service.trackRoute("MF-T9JSDP6N", " ").getClientActionList().get(0)).getRows()).isEmpty();

    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void drawsTheRoadOfTheSelectedRouteAndItsNumberedStopsOnTheMapOfTheRoutesScreen() throws AWException {
    when(queryService.launchQuery(eq("FltRteMapPts"), any(ObjectNode.class))).thenReturn(points(
      List.of("43.1", "43.2", "43.4"), List.of("-2.9", "-2.8", "-2.7")));
    when(queryService.launchQuery(eq("FltRteMapStops"), any(ObjectNode.class))).thenReturn(stops(
      List.of("1", "2"), List.of("Getxo", "Barakaldo"), List.of("43.356", "43.296"), List.of("-3.011", "-2.989")));

    ServiceData data = service.routeStops(12);

    assertThat(data.getClientActionList()).hasSize(2);
    ClientAction fill = data.getClientActionList().get(0);
    assertThat(fill.getType()).isEqualTo("fill");
    assertThat(fill.getTarget()).isEqualTo("MapFltRoute");
    assertThat(DataListUtil.getData(markers(fill), 1, "Loc")).isEqualTo("Barakaldo");
    ClientAction line = data.getClientActionList().get(1);
    assertThat(line.getType()).isEqualTo("polyline");
    assertThat(line.getTarget()).isEqualTo("MapFltRoute");
    assertThat(rows(line)).hasSize(3);
    org.mockito.ArgumentCaptor<ObjectNode> parameters = org.mockito.ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, org.mockito.Mockito.times(2)).launchQuery(anyString(), parameters.capture());
    assertThat(parameters.getAllValues()).allSatisfy(value -> assertThat(value.get("Ide").asInt()).isEqualTo(12));
    // The road has more points than a page of rows: every row is read, not the first page only
    assertThat(parameters.getAllValues()).allSatisfy(value -> assertThat(value.get("max").asInt()).isZero());
  }

  @Test
  void removesTheRouteAndTheStopsWhenNoRouteIsSelected() throws AWException {
    ServiceData data = service.routeStops(null);

    assertThat(markers(data.getClientActionList().get(0)).getRows()).isEmpty();
    assertThat(rows(data.getClientActionList().get(1))).isEmpty();
    verify(queryService, never()).launchQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void skipsThePointsOfTheRoadWithoutCoordinates() throws AWException {
    when(queryService.launchQuery(eq("FltRteMapPts"), any(ObjectNode.class))).thenReturn(points(
      java.util.Arrays.asList("43.1", null, "43.4"), java.util.Arrays.asList("-2.9", "-2.8", "")));
    when(queryService.launchQuery(eq("FltRteMapStops"), any(ObjectNode.class))).thenReturn(stops(
      List.of(), List.of(), List.of(), List.of()));

    ServiceData data = service.routeStops(3);

    assertThat(rows(data.getClientActionList().get(1))).hasSize(1);
  }

  private static ServiceData stops(List<String> sequences, List<String> places, List<String> latitudes,
                                   List<String> longitudes) {
    DataList list = new DataList();
    DataListUtil.addColumn(list, "Seq", sequences);
    DataListUtil.addColumn(list, "Loc", places);
    DataListUtil.addColumn(list, "Lat", latitudes);
    DataListUtil.addColumn(list, "Lon", longitudes);
    list.setRecords(sequences.size());
    return new ServiceData().setDataList(list);
  }

  private static DataList markers(ClientAction action) {
    return (DataList) action.getParameters().get("datalist");
  }

  private static ServiceData points(List<String> latitudes, List<String> longitudes) {
    DataList list = new DataList();
    DataListUtil.addColumn(list, "Lat", latitudes);
    DataListUtil.addColumn(list, "Lon", longitudes);
    list.setRecords(latitudes.size());
    return new ServiceData().setDataList(list);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> rows(ClientAction action) {
    return (List<Map<String, Object>>) action.getParameters().get("rows");
  }
}
