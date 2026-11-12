package com.almis.awe.samples.fleetlogistics.map;

import com.almis.awe.builder.client.ClientActionBuilder;
import com.almis.awe.builder.client.FillActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows the stops and the roads of the fleet screens: the numbered markers of a map are filled (<code>fill</code> client
 * action) and the road of a route is drawn with the <code>polyline</code> client action. The points are never read
 * here: they come from AWE queries, so the rules of the query decide who sees what:
 * <ul>
 *   <li><code>FltTrackPts</code> (public tracking) only returns the points of a shipment when the tracking code AND the
 *   destination postal code match, exactly like the timeline query <code>FltTrackPub</code>.</li>
 *   <li><code>FltRteMapPts</code> (road) and <code>FltRteMapStops</code> (stops) of the route planner require the
 *   <code>routes.read</code> permission.</li>
 * </ul>
 * The roads are precomputed in the seed (no routing service is called at runtime).
 */
@Slf4j
@Service
public class FleetMapService extends ServiceConfig {

  static final String TRACK_MAP = "MapFltTrack";
  static final String ROUTE_MAP = "MapFltRoute";
  private static final String LATITUDE = "Lat";
  private static final String LONGITUDE = "Lon";
  /** Max rows of a query that means every row */
  private static final int ALL_ROWS = 0;

  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public FleetMapService(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Points of a shipment in order (origin hub, central hub, destination), as the numbered markers of the tracking map.
   * Without the right code and postal code there are no points (the map removes any marker it was showing), so the
   * lookup says nothing about which of both values failed
   *
   * @param code   Tracking code
   * @param postal Destination postal code
   * @return Service data with the fill action of the tracking map
   * @throws AWException Query failed
   */
  public ServiceData trackRoute(String code, String postal) throws AWException {
    if (isBlank(code) || isBlank(postal)) {
      return markers(TRACK_MAP, new DataList());
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("TrkCod", code.trim());
    parameters.put("TrkPst", postal.trim());
    parameters.put(AweConstants.COMPONENT_MAX, ALL_ROWS);
    // The query is private (not launchable from the client) and carries the guard: both variables are mandatory
    return markers(TRACK_MAP, queryService.launchPrivateQuery("FltTrackPts", parameters).getDataList());
  }

  /**
   * Road and numbered stops of a route of the route planner. The selected row of the tree can be the route or one of
   * its stops
   *
   * @param treeId Identifier of the selected row of the route tree
   * @return Service data with the fill action (stops) and the polyline action (road) of the route map
   * @throws AWException Query failed or the user cannot read routes
   */
  public ServiceData routeStops(Integer treeId) throws AWException {
    if (treeId == null) {
      return routeActions(new DataList(), new DataList());
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("Ide", treeId);
    // The road has more points than a page of rows
    parameters.put(AweConstants.COMPONENT_MAX, ALL_ROWS);
    DataList stops = queryService.launchQuery("FltRteMapStops", parameters).getDataList();
    DataList road = queryService.launchQuery("FltRteMapPts", parameters).getDataList();
    return routeActions(stops, road);
  }

  private static ServiceData routeActions(DataList stops, DataList road) {
    ServiceData data = markers(ROUTE_MAP, stops);
    polyline(ROUTE_MAP, road).getClientActionList().forEach(data::addClientAction);
    return data;
  }

  /**
   * Build the fill action of a map with the rows of a query (the map draws a marker per row)
   *
   * @param target Identifier of the map
   * @param points Rows with the latitude and the longitude columns (and the number and the label of each marker)
   * @return Service data with the action
   */
  static ServiceData markers(String target, DataList points) {
    return new ServiceData().addClientAction(new FillActionBuilder(target, points == null ? new DataList() : points).build());
  }

  /**
   * Build the polyline action of a map with the rows of a query
   *
   * @param target Identifier of the map
   * @param points Rows with the latitude and the longitude columns
   * @return Service data with the action. Rows without valid coordinates are skipped
   */
  static ServiceData polyline(String target, DataList points) {
    List<Map<String, Object>> rows = new ArrayList<>();
    int count = points == null || points.getRows() == null ? 0 : points.getRows().size();
    for (int row = 0; row < count; row++) {
      Double latitude = coordinate(DataListUtil.getData(points, row, LATITUDE));
      Double longitude = coordinate(DataListUtil.getData(points, row, LONGITUDE));
      if (latitude != null && longitude != null) {
        rows.add(Map.of(LATITUDE, latitude, LONGITUDE, longitude));
      }
    }
    ClientActionBuilder<?> builder = new ClientActionBuilder<>();
    builder.setType("polyline");
    builder.setTarget(target);
    builder.addParameter("rows", rows);
    ClientAction action = builder.build();
    return new ServiceData().addClientAction(action);
  }

  private static Double coordinate(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      log.debug("Map point skipped, the coordinate [{}] is not a number", value);
      return null;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
