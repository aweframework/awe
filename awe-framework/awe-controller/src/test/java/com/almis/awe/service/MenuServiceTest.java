package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.*;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Menu service tests
 *
 * @author pgarcia
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

  @InjectMocks
  private MenuService menuService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweElements aweElements;
  @Mock
  private AweSession aweSession;
  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private BaseConfigProperties baseConfigProperties;
  @Mock
  private FavouriteService favouriteService;

  @Mock
  private BaseConfigProperties.Files files;

  @BeforeEach
  void setUp() {
    menuService.setApplicationContext(context);
  }

  @Test
  void testGetMenu() throws AWException {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweSession.isAuthenticated()).thenReturn(false);
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPublic()).thenReturn("public");
    when(aweElements.getMenu(anyString())).thenReturn(new Menu());
    assertNotNull(menuService.getMenu());
  }

  @Test
  void testGetPrivateMenuEmpty() throws AWException {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getMenu(anyString())).thenReturn(new Menu());
    assertNotNull(menuService.getMenu("private"));
  }

  @Test
  void testUpdateMenuRestrictionTree() throws Exception {
    // When
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "restrictions", 4);
    when(queryService.launchPrivateQuery(anyString(), anyString(), anyString())).thenReturn(new ServiceData().setDataList(dataList));

    // Do
    ServiceData serviceData = menuService.updateMenuRestrictionTree("option");

    // Check
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(1, serviceData.getClientActionList().size());
    assertEquals(4, ((JsonNode) serviceData.getClientActionList().get(0).getParameterMap().get("data")).get("value").asInt());
  }

  @Test
  void testUpdateMenuRestrictionTreeNoData() throws Exception {
    // When
    when(queryService.launchPrivateQuery(anyString(), anyString(), anyString())).thenReturn(new ServiceData().setDataList(new DataList()));

    // Do
    ServiceData serviceData = menuService.updateMenuRestrictionTree("option");

    // Check
    assertEquals(AnswerType.OK, serviceData.getType());
  }

  @Test
  void testUpdateMenuRestrictionTreeCheckAction() throws Exception {
    // Arrange
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("restrictions", new CellData(2));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), anyString(), anyString())).thenReturn(new ServiceData().setDataList(dataList));
    String option = "option1";

    // Act
    ServiceData result = menuService.updateMenuRestrictionTree(option);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getClientActionList().size());
    ClientAction clientAction = result.getClientActionList().get(0);
    assertEquals("update-cell", clientAction.getType());
    assertEquals("report", clientAction.getAddress().getView());
    assertEquals("menu-option-tree", clientAction.getAddress().getComponent());
    assertEquals(option, clientAction.getAddress().getRow());
    assertEquals("number-of-restrictions", clientAction.getAddress().getColumn());
    assertNotNull(clientAction.getParameterMap());
    assertEquals(1, clientAction.getParameterMap().size());
    assertTrue(clientAction.getParameterMap().containsKey("data"));
    assertNotNull(clientAction.getParameterMap().get("data"));
  }

  @Test
  void testUpdateMenuRestrictionTreeThrowsException() throws Exception {
    // Arrange
    when(queryService.launchPrivateQuery(anyString(), anyString(), anyString())).thenThrow(new AWException("Error retrieving data"));
    String option = "option1";

    // Act & Assert
    assertThrows(AWException.class, () -> menuService.updateMenuRestrictionTree(option));
  }

  @Test
  void testGetMenuOptionTree() throws Exception {
    // Arrange
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restrictions", new CellData(2));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), anyString(), anyString())).thenReturn(new ServiceData().setDataList(dataList));
    Menu menu = new Menu();
    menu
      .addElement(new Option().setName("option-1"))
      .addElement(new Option().setSeparator(true).setName("option-2"))
      .addElement(new Option().setName("option-3")
        .addElement(new Option().setName("option-4"))
        .addElement(new Option().setName("option-5")
          .addElement(new Option().setName("option-6"))
          .addElement(new Option().setRestricted(true).setName("option-7")))
        .addElement(new Option().setName("option-8")))
      .addElement(new Option().setName("option-9")
        .addElement(new Option().setScreen("screen").setName("option-10"))
        .addElement(new Option().setSeparator(true).setName("option-11")))
      .addElement(new Option().setName("option-12")
        .addElement(new Option().setScreen("screen").setName("option-13")));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getMenu(anyString())).thenReturn(menu);

    // Act
    ServiceData serviceData = menuService.getMenuOptionTree();

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(13, serviceData.getDataList().getRecords());
  }

  @Test
  void testGetMenuOptionTreeByModule() throws Exception {
    // Arrange
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restriction", new CellData("R"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
    Menu menu = new Menu();
    menu
      .addElement(new Option().setScreen("screen").setName("option-1"))
      .addElement(new Option().setSeparator(true).setName("option-2"))
      .addElement(new Option().setName("option-3")
        .addElement(new Option().setName("option-4"))
        .addElement(new Option().setName("option-5")
          .addElement(new Option().setScreen("screen").setName("option-6"))
          .addElement(new Option().setRestricted(true).setName("option-7")))
        .addElement(new Option().setName("option-8")))
      .addElement(new Option().setName("option-9")
        .addElement(new Option().setName("option-10"))
        .addElement(new Option().setName("option-11")))
      .addElement(new Option().setName("option-12")
        .addElement(new Option().setName("option-13")));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getMenu(anyString())).thenReturn(menu);

    // Act
    ServiceData serviceData = menuService.getMenuOptionTreeByModule(null, null, null);

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(12, serviceData.getDataList().getRecords());
  }

  @Test
  void testGetMenuOptionTreeByModuleWithProfile() throws Exception {
    // Arrange
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restriction", new CellData("R"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
    Menu menu = new Menu();
    menu
      .addElement(new Option().setName("option-1"))
      .addElement(new Option().setSeparator(true).setName("option-2"))
      .addElement(new Option().setScreen("screen").setName("option-3")
        .addElement(new Option().setName("option-4"))
        .addElement(new Option().setName("option-5")
          .addElement(new Option().setName("option-6"))
          .addElement(new Option().setScreen("screen").setName("option-7")))
        .addElement(new Option().setName("option-8")))
      .addElement(new Option().setName("option-9")
        .addElement(new Option().setName("option-10"))
        .addElement(new Option().setName("option-11")))
      .addElement(new Option().setRestricted(true).setName("option-12")
        .addElement(new Option().setName("option-13")));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getMenu(anyString())).thenReturn(menu);

    // Act
    ServiceData serviceData = menuService.getMenuOptionTreeByModule(null, 1, "test");

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(12, serviceData.getDataList().getRecords());
  }

  @Test
  void testGetMenuOptionTreeByModuleWithUser() throws Exception {
    // Arrange
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restriction", new CellData("R"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
    Menu menu = new Menu();
    menu
      .addElement(new Option().setName("option-1"))
      .addElement(new Option().setName("option-2"))
      .addElement(new Option().setName("option-3")
        .addElement(new Option().setRestricted(true).setName("option-4"))
        .addElement(new Option().setName("option-5")
          .addElement(new Option().setName("option-6"))
          .addElement(new Option().setScreen("screen").setName("option-7")))
        .addElement(new Option().setName("option-8")))
      .addElement(new Option().setName("option-9")
        .addElement(new Option().setScreen("screen").setName("option-10"))
        .addElement(new Option().setSeparator(true).setName("option-11")))
      .addElement(new Option().setName("option-12")
        .addElement(new Option().setName("option-13")));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getMenu(anyString())).thenReturn(menu);

    // Act
    ServiceData serviceData = menuService.getMenuOptionTreeByModule(1, null, "test");

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(12, serviceData.getDataList().getRecords());
    assertEquals("R", ((ObjectNode) serviceData.getDataList().getRows().get(1).get("restriction").getObjectValue()).get("value").asText());
  }

  @Test
  void testAllowMenuOption() throws Exception {
    // Arrange
    changeOptionRestriction();
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restriction", new CellData("R"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));

    // Act
    ServiceData serviceData = menuService.allowMenuOption(null, null, "test");

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void testRestrictMenuOption() throws Exception {
    // Arrange
    changeOptionRestriction();
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));

    // Act
    ServiceData serviceData = menuService.restrictMenuOption(null, null, "test");

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void testRemoveRestriction() throws Exception {
    // Arrange
    changeOptionRestriction();
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("option", new CellData("option-2"));
    row.put("restriction", new CellData("R"));
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));

    // Act
    ServiceData serviceData = menuService.removeRestriction(null, null, "test");

    // Assert
    assertEquals(AnswerType.OK, serviceData.getType());
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  private void changeOptionRestriction() throws AWException {
    // Arrange
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
  }

  @Test
  void testGetMenuWithFavourites() throws AWException {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(aweSession.getUser()).thenReturn("user");
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPrivate()).thenReturn("private");
    when(aweElements.getMenu(anyString())).thenReturn(new Menu()
      .addElement(new Option().setName("test"))
      .addElement(new Option().setName("test2"))
      .addElement(new Option().setName("test3"))
      .addElement(new Option().setName("test4"))
    );
    when(favouriteService.getFavourites(anyString())).thenReturn(Arrays.asList(
      new Favourite().setOption("test"),
      new Favourite().setOption("test4")
    ));
    Menu menu = menuService.getMenu();
    assertNotNull(menu);
    assertEquals(8, menu.getElementsByType(Option.class).size());
    assertEquals("favourites - favourite-test - favourite-test4 - favourites-separator - test - test2 - test3 - test4",
      menu.getElementsByType(Option.class).stream().map(Option::getId).collect(Collectors.joining(" - ")));
  }

  @Test
  void getMenuWithRestrictions() throws AWException {
    Menu menu = new Menu()
      .addElement(new Option().setName("test"))
      .addElement(new Option().setName("test2"))
      .addElement(new Option().setName("test3")
        .addElement(new Option().setName("test3-1")
          .addElement(new Option().setName("test3-1-1"))))
      .addElement(new Option().setName("test4"))
      .addElement(new Option().setName("test5")
        .addElement(new Option().setName("test5-1")
          .addElement(new Option().setName("test5-1-1"))
          .addElement(new Option().setName("test5-1-2")))
        .addElement(new Option().setName("test5-2")))
      .addElement(new Option().setName("test6"))
      .addElement(new Option().setName("test7"));

    List<ScreenRestriction> screenRestrictionList = Arrays.asList(
      new ScreenRestriction().setOption("test").setRestricted(true),
      new ScreenRestriction().setOption("test").setRestricted(true).setProfile("1"),
      new ScreenRestriction().setOption("test").setRestricted(false).setUser("test"),
      new ScreenRestriction().setOption("test2").setRestricted(true),
      new ScreenRestriction().setOption("test2").setRestricted(false).setProfile("1"),
      new ScreenRestriction().setOption("test3").setRestricted(true),
      new ScreenRestriction().setOption("test4").setRestricted(true).setUser("test"),
      new ScreenRestriction().setOption("test6").setRestricted(true).setProfile("1"),
      new ScreenRestriction().setOption("test7").setRestricted(false),
      new ScreenRestriction().setOption("test7").setRestricted(true).setUser("test"),
      new ScreenRestriction().setOption("test5-1").setRestricted(true).setProfile("1")
    );

    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_RESTRICTION_QUERY, "1", "0")).thenReturn(new ServiceData().setDataList(
      DataListUtil.fromBeanList(screenRestrictionList)
    ));

    menu = menuService.getMenuWithRestrictions(menu);

    assertEquals(13, menu.getElementsByType(Option.class).size());
    assertEquals(9, menu.getElementsByType(Option.class).stream().filter(Option::isRestricted).count());
  }
 }