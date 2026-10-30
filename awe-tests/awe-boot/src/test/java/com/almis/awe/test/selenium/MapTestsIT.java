package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Map of the test screens (Leaflet and OpenStreetMap): its markers, their selection and the route that a client action
 * draws. Every test opens its own screen.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class MapTestsIT extends AbstractSessionTests {

  MapTestsIT() {
    super(Session.TEST_MODULE);
  }

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout();
  }

  /**
   * Map: it draws one marker per point of the query
   */
  @Test
  void t030_mapMarkers() {
    // Title
    setTestTitle("Map test: markers");

    // Go to screen
    gotoScreen("test", "chart", "map-test");

    // The map is drawn with the four points of the query
    checkMapVisible("MapTst");
    checkMapMarkerCount("MapTst", 4);
  }

  /**
   * Map: selecting a marker copies the place to a criterion through a dependency
   */
  @Test
  void t031_mapSelection() {
    // Title
    setTestTitle("Map test: selection");

    // Go to screen
    gotoScreen("test", "chart", "map-test");
    checkMapVisible("MapTst");

    // Select the second marker: the dependency copies its place
    clickMapMarker("MapTst", "2");
    checkMapMarkerSelected("MapTst", "2");
    checkCriterionContents("MapSel", "Barcelona");

    // Select the last marker
    clickMapMarker("MapTst", "4");
    checkMapMarkerSelected("MapTst", "4");
    checkCriterionContents("MapSel", "Sevilla");
  }

  /**
   * Map: the server action draws a route and the route replaces the markers
   */
  @Test
  void t032_mapRoute() {
    // Title
    setTestTitle("Map test: route");

    // Go to screen
    gotoScreen("test", "chart", "map-test");
    checkMapVisible("MapTst");
    checkMapMarkerCount("MapTst", 4);

    // Draw the route
    clickButton("MapRoute");
    checkMapRouteVisible("MapTst");
    checkMapMarkerCount("MapTst", 0);
  }
}
