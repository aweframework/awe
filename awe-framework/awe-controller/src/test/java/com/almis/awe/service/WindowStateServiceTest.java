package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Advanced;
import com.almis.awe.model.entities.screen.component.Window;
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
import java.util.List;
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
@DisplayName("Window state service")
class WindowStateServiceTest {

  @InjectMocks
  private WindowStateService windowStateService;

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
    windowStateService.setApplicationContext(applicationContext);
  }

  private void mockSessionUser(String user) {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.getUser()).thenReturn(user);
  }

  /**
   * Screen "Scr" with the window "Win", which holds an advanced block, and the window "Plain", which does not
   */
  private void mockScreen() throws Exception {
    Window window = new Window();
    window.setId("Win");
    window.setElementList(List.of(new Advanced()));
    Window plain = new Window();
    plain.setId("Plain");
    Screen screen = new Screen();
    screen.setElementList(List.of(window, plain));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("Scr")).thenReturn(screen);
  }

  private void mockLocale() {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("locale");
  }

  private void mockParameters() {
    when(queryUtil.getParameters(null, "1", "0")).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
  }

  private ServiceData rows(Object... windowIdAndOpen) {
    DataList dataList = new DataList();
    for (int index = 0; index < windowIdAndOpen.length; index += 2) {
      Map<String, CellData> row = new HashMap<>();
      row.put("windowId", new CellData((String) windowIdAndOpen[index]));
      row.put("advancedOpen", new CellData((Integer) windowIdAndOpen[index + 1]));
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  private ServiceData noRows() {
    return new ServiceData().setDataList(new DataList());
  }

  private void verifyNoMaintain() {
    try {
      verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
    } catch (AWException exc) {
      throw new IllegalStateException(exc);
    }
  }

  @Test
  @DisplayName("Saving a state without a stored one inserts it, without any user parameter")
  void saveInsertsWhenAbsent() throws Exception {
    mockSessionUser("john");
    mockScreen();
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserWindowState"), any(ObjectNode.class))).thenReturn(noRows());
    when(maintainService.launchPrivateMaintain(eq("insertWindowAdvancedState"), any(ObjectNode.class))).thenReturn(new ServiceData());

    windowStateService.saveAdvancedState("Scr", "Win", "1");

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertWindowAdvancedState"), captor.capture());
    ObjectNode parameters = captor.getValue();
    assertEquals("Scr", parameters.get("screen").asText());
    assertEquals("Win", parameters.get("windowId").asText());
    assertEquals(1, parameters.get("advancedOpen").asInt());
    assertFalse(parameters.has("user"), "The user must come from the session, never from the parameters");
    verify(maintainService, never()).launchPrivateMaintain(eq("updateWindowAdvancedState"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Saving a state over a stored one updates it")
  void saveUpdatesWhenPresent() throws Exception {
    mockSessionUser("john");
    mockScreen();
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserWindowState"), any(ObjectNode.class))).thenReturn(rows("Win", 1));
    when(maintainService.launchPrivateMaintain(eq("updateWindowAdvancedState"), any(ObjectNode.class))).thenReturn(new ServiceData());

    windowStateService.saveAdvancedState("Scr", "Win", "0");

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("updateWindowAdvancedState"), captor.capture());
    assertEquals(0, captor.getValue().get("advancedOpen").asInt());
    verify(maintainService, never()).launchPrivateMaintain(eq("insertWindowAdvancedState"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Losing an insert race converges by updating the row")
  void saveRetriesAsUpdateWhenTheInsertLosesTheRace() throws Exception {
    mockSessionUser("john");
    mockScreen();
    mockParameters();
    // Nothing stored when the save starts, a concurrent save stored the row when the insert fails
    when(queryService.launchPrivateQuery(eq("getUserWindowState"), any(ObjectNode.class))).thenReturn(noRows(), rows("Win", 0));
    when(maintainService.launchPrivateMaintain(eq("insertWindowAdvancedState"), any(ObjectNode.class))).thenThrow(new AWException("dup", "dup"));
    when(maintainService.launchPrivateMaintain(eq("updateWindowAdvancedState"), any(ObjectNode.class))).thenReturn(new ServiceData());

    windowStateService.saveAdvancedState("Scr", "Win", "1");

    verify(maintainService).launchPrivateMaintain(eq("updateWindowAdvancedState"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("An insert error that is not a lost race surfaces and nothing is updated")
  void saveSurfacesAnInsertErrorThatIsNotARace() throws Exception {
    mockSessionUser("john");
    mockScreen();
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserWindowState"), any(ObjectNode.class))).thenReturn(noRows());
    AWException failure = new AWException("db down", "db down");
    when(maintainService.launchPrivateMaintain(eq("insertWindowAdvancedState"), any(ObjectNode.class))).thenThrow(failure);

    AWException thrown = assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Win", "1"));

    assertEquals(failure, thrown);
    verify(maintainService, never()).launchPrivateMaintain(eq("updateWindowAdvancedState"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Without a session user nothing is saved")
  void anonymousUserCannotSave() {
    mockSessionUser(null);
    mockLocale();

    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Win", "1"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("A window that is not in the screen, or that holds no advanced block, is rejected and nothing is stored")
  void unknownWindowIsRejected() throws Exception {
    mockSessionUser("john");
    mockScreen();
    mockLocale();
    when(aweElements.getScreen("Missing")).thenThrow(new AWException("missing", "missing"));

    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "NoSuchWindow", "1"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Plain", "1"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Missing", "Win", "1"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("A value other than \"1\" or \"0\", or blank identifiers, are rejected and nothing is stored")
  void invalidValuesAreRejected() {
    mockSessionUser("john");
    mockLocale();

    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Win", "true"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Win", "2"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "Win", null));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("", "Win", "1"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", null, "1"));
    assertThrows(AWException.class, () -> windowStateService.saveAdvancedState("Scr", "W".repeat(101), "1"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The stored states of a screen are read by window")
  void statesOfAScreen() throws Exception {
    mockSessionUser("john");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserWindowStates"), any(ObjectNode.class))).thenReturn(rows("Open", 1, "Closed", 0));

    Map<String, Boolean> states = windowStateService.getScreenAdvancedStates("Scr");

    assertEquals(Map.of("Open", true, "Closed", false), states);
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("getUserWindowStates"), captor.capture());
    assertEquals("Scr", captor.getValue().get("screen").asText());
  }

  @Test
  @DisplayName("A failure reading the states never breaks the screen")
  void stateReadFailureIsTolerated() throws Exception {
    mockSessionUser("john");
    mockParameters();
    when(queryService.launchPrivateQuery(eq("getUserWindowStates"), any(ObjectNode.class))).thenThrow(new AWException("down", "down"));

    assertTrue(windowStateService.getScreenAdvancedStates("Scr").isEmpty());
  }

  @Test
  @DisplayName("Without a session user there are no states")
  void noStatesWithoutUser() throws Exception {
    mockSessionUser(null);

    assertTrue(windowStateService.getScreenAdvancedStates("Scr").isEmpty());
    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }
}
