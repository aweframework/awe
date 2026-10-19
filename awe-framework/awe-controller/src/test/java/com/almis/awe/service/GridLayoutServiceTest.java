package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.GridColumnLayout;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Grid layout service")
class GridLayoutServiceTest {

  @InjectMocks
  private GridLayoutService gridLayoutService;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private MaintainService maintainService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    gridLayoutService.setApplicationContext(applicationContext);
  }

  private void mockSessionUser(String user) {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.getUser()).thenReturn(user);
  }

  private void mockScreenWithGrid(String screenId, String gridId) throws Exception {
    com.almis.awe.model.entities.screen.Screen screen = new com.almis.awe.model.entities.screen.Screen();
    com.almis.awe.model.entities.screen.component.grid.Grid grid = new com.almis.awe.model.entities.screen.component.grid.Grid();
    grid.setId(gridId);
    screen.setElementList(java.util.List.of(grid));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen(screenId)).thenReturn(screen);
  }

  private void mockLocale() {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("locale");
  }

  private void mockParameters() {
    when(queryUtil.getParameters(null, "1", "0")).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
  }

  private ServiceData rows(String... gridIdAndLayout) {
    DataList dataList = new DataList();
    for (int index = 0; index < gridIdAndLayout.length; index += 2) {
      Map<String, CellData> row = new HashMap<>();
      row.put("gridId", new CellData(gridIdAndLayout[index]));
      row.put("layout", new CellData(gridIdAndLayout[index + 1]));
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  private ServiceData noRows() {
    return new ServiceData().setDataList(new DataList());
  }

  @Test
  @DisplayName("Saving a layout without a stored one inserts it, normalized and without any user parameter")
  void saveInsertsWhenAbsent() throws Exception {
    mockSessionUser("john");
    mockScreenWithGrid("Scr", "Grd");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserGridLayout"), any(ObjectNode.class))).thenReturn(noRows());
    when(maintainService.launchPrivateMaintain(eq("insertGridLayout"), any(ObjectNode.class))).thenReturn(new ServiceData());

    gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[\"a\",\"b\"],\"hidden\":[\"b\"],\"extra\":1}");

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertGridLayout"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertEquals("Scr", parameters.get("screen").asText());
    assertEquals("Grd", parameters.get("gridId").asText());
    assertEquals("{\"order\":[\"a\",\"b\"],\"hidden\":[\"b\"]}", parameters.get("layout").asText());
    assertFalse(parameters.has("user"), "The user must come from the session, never from the parameters");
  }

  @Test
  @DisplayName("Saving a layout over a stored one updates it")
  void saveUpdatesWhenPresent() throws Exception {
    mockSessionUser("john");
    mockScreenWithGrid("Scr", "Grd");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserGridLayout"), any(ObjectNode.class))).thenReturn(rows("Grd", "{}"));
    when(maintainService.launchPrivateMaintain(eq("updateGridLayout"), any(ObjectNode.class))).thenReturn(new ServiceData());

    gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[\"a\"],\"hidden\":[]}");

    verify(maintainService).launchPrivateMaintain(eq("updateGridLayout"), any(ObjectNode.class));
    verify(maintainService, never()).launchPrivateMaintain(eq("insertGridLayout"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Losing an insert race converges by updating the row")
  void saveRetriesAsUpdateWhenTheInsertLosesTheRace() throws Exception {
    mockSessionUser("john");
    mockScreenWithGrid("Scr", "Grd");
    mockParameters();
    // Nothing stored when the save starts, a concurrent save stored the row when the insert fails
    when(queryService.launchPrivateQuery(eq("getUserGridLayout"), any(ObjectNode.class))).thenReturn(noRows(), rows("Grd", "{}"));
    when(maintainService.launchPrivateMaintain(eq("insertGridLayout"), any(ObjectNode.class))).thenThrow(new AWException("dup", "dup"));
    when(maintainService.launchPrivateMaintain(eq("updateGridLayout"), any(ObjectNode.class))).thenReturn(new ServiceData());

    gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[\"a\"],\"hidden\":[]}");

    verify(maintainService).launchPrivateMaintain(eq("updateGridLayout"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("An insert error that is not a lost race surfaces and nothing is updated")
  void saveSurfacesAnInsertErrorThatIsNotARace() throws Exception {
    mockSessionUser("john");
    mockScreenWithGrid("Scr", "Grd");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserGridLayout"), any(ObjectNode.class))).thenReturn(noRows());
    AWException failure = new AWException("db down", "db down");
    when(maintainService.launchPrivateMaintain(eq("insertGridLayout"), any(ObjectNode.class))).thenThrow(failure);

    AWException thrown = assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[\"a\"],\"hidden\":[]}"));

    assertEquals(failure, thrown);
    verify(maintainService, never()).launchPrivateMaintain(eq("updateGridLayout"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("A layout that is not valid is rejected and nothing is stored")
  void invalidLayoutIsRejected() {
    mockSessionUser("john");
    mockLocale();

    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "not json"));
    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "{\"hidden\":[]}"));
    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("", "Grd", "{\"order\":[],\"hidden\":[]}"));
    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", null, "{\"order\":[],\"hidden\":[]}"));
  }

  @Test
  @DisplayName("A grid that is not in the screen is rejected and nothing is stored")
  void unknownGridIsRejected() throws Exception {
    mockSessionUser("john");
    mockLocale();
    when(aweElements.getScreen("Scr")).thenReturn(new com.almis.awe.model.entities.screen.Screen());
    when(aweElements.getScreen("Missing")).thenThrow(new AWException("missing", "missing"));

    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[],\"hidden\":[]}"));
    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Missing", "Grd", "{\"order\":[],\"hidden\":[]}"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("A layout too large for its column is rejected")
  void largeLayoutIsRejected() {
    mockSessionUser("john");
    mockLocale();
    StringBuilder order = new StringBuilder("[");
    for (int index = 0; index < 400; index++) {
      order.append(index == 0 ? "" : ",").append("\"column").append(index).append("\"");
    }
    order.append("]");

    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":" + order + ",\"hidden\":[]}"));
  }

  @Test
  @DisplayName("Without a session user nothing is saved")
  void anonymousUserCannotSave() {
    mockSessionUser(null);
    mockLocale();

    assertThrows(AWException.class, () -> gridLayoutService.saveGridLayout("Scr", "Grd", "{\"order\":[],\"hidden\":[]}"));
    verifyNoMaintain();
  }

  private void verifyNoMaintain() {
    try {
      verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
    } catch (AWException exc) {
      throw new IllegalStateException(exc);
    }
  }

  @Test
  @DisplayName("Resetting removes the stored layout of the session user")
  void resetDeletesTheLayout() throws Exception {
    mockSessionUser("john");
    mockParameters();
    when(maintainService.launchPrivateMaintain(eq("deleteGridLayout"), any(ObjectNode.class))).thenReturn(new ServiceData());

    gridLayoutService.resetGridLayout("Scr", "Grd");

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("deleteGridLayout"), captor.capture());
    assertEquals("Grd", captor.getValue().get("gridId").asText());
    assertFalse(captor.getValue().has("user"));
  }

  @Test
  @DisplayName("The stored layouts of a screen are read by grid, skipping the ones that cannot be read")
  void layoutsOfAScreen() throws Exception {
    mockSessionUser("john");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserGridLayouts"), any(ObjectNode.class)))
      .thenReturn(rows("Good", "{\"order\":[\"b\",\"a\"],\"hidden\":[\"a\"]}", "Broken", "{oops"));

    Map<String, GridColumnLayout> layouts = gridLayoutService.getScreenLayouts("Scr");

    assertEquals(1, layouts.size());
    assertEquals(java.util.List.of("b", "a"), layouts.get("Good").getOrder());
    assertEquals(java.util.List.of("a"), layouts.get("Good").getHidden());
  }

  @Test
  @DisplayName("A failure reading the layouts never breaks the screen")
  void layoutReadFailureIsTolerated() throws Exception {
    mockSessionUser("john");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserGridLayouts"), any(ObjectNode.class))).thenThrow(new AWException("down", "down"));

    assertTrue(gridLayoutService.getScreenLayouts("Scr").isEmpty());
  }

  @Test
  @DisplayName("Without a session user there are no layouts")
  void noLayoutsWithoutUser() throws Exception {
    mockSessionUser(null);

    assertTrue(gridLayoutService.getScreenLayouts("Scr").isEmpty());
    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  @DisplayName("The layout of a grid is merged with its XML columns, and grids without one get none")
  void resolveLayoutMergesWithTheXml() {
    com.almis.awe.model.entities.screen.component.grid.Column first = new com.almis.awe.model.entities.screen.component.grid.Column();
    first.setName("a");
    com.almis.awe.model.entities.screen.component.grid.Column second = new com.almis.awe.model.entities.screen.component.grid.Column();
    second.setName("b");
    com.almis.awe.model.entities.screen.component.grid.Grid grid = new com.almis.awe.model.entities.screen.component.grid.Grid();
    grid.setId("Grd");
    grid.setElementList(java.util.List.of(first, second));
    Map<String, GridColumnLayout> layouts = Map.of("Grd", new GridColumnLayout(java.util.List.of("b", "gone", "a"), java.util.List.of("a")));

    GridColumnLayout resolved = gridLayoutService.resolveLayout(grid, layouts);

    assertEquals(java.util.List.of("b", "a"), resolved.getOrder());
    assertEquals(java.util.List.of("a"), resolved.getHidden());
    assertTrue(gridLayoutService.resolveLayout(grid, Map.of()) == null);

    grid.setShowToolbar(false);
    assertTrue(gridLayoutService.resolveLayout(grid, layouts) == null, "A grid without toolbar ignores the stored layout");
  }
}
