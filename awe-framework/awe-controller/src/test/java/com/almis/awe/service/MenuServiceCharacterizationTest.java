package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWESessionException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.panelable.Tab;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.type.LoadType;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for the public MenuService methods not covered by {@link MenuServiceTest}.
 * They pin the current behavior before MenuService is split into collaborators (#782).
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceCharacterizationTest {

  @InjectMocks
  private MenuService menuService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweElements aweElements;
  @Mock
  private AweSession aweSession;
  @Mock
  private AweRequest aweRequest;
  @Mock
  private QueryService queryService;
  @Mock
  private ScreenComponentGenerator screenComponentGenerator;
  @Mock
  private InitialLoadDao initialLoadDao;
  @Mock
  private BaseConfigProperties baseConfigProperties;
  @Mock
  private SecurityConfigProperties securityConfigProperties;
  @Mock
  private FavouriteService favouriteService;
  @Mock
  private LauncherService launcherService;
  @Mock
  private BaseConfigProperties.Files files;

  @BeforeEach
  void setUp() {
    menuService.setApplicationContext(context);
  }

  @AfterEach
  void clearInterruptFlag() {
    // A failed initial load interrupts the current thread; keep it from leaking into other tests
    Thread.interrupted();
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private static Option option(String name) {
    Option option = new Option();
    option.setName(name);
    return option;
  }

  private static Option option(String name, String screen) {
    Option option = option(name);
    option.setScreen(screen);
    return option;
  }

  private static Option labelled(Option option, String label) {
    option.setLabel(label);
    return option;
  }

  private static Option module(Option option, String module) {
    option.setModule(module);
    return option;
  }

  private static Screen screen(String id, boolean initialized) {
    Screen screen = new Screen();
    screen.setId(id);
    screen.setInitialized(initialized);
    return screen;
  }

  private static Menu menu(Option... options) {
    Menu menu = new Menu();
    for (Option option : options) {
      menu.addElement(option);
    }
    return menu;
  }

  private static List<String> column(ServiceData serviceData, String column) {
    return serviceData.getDataList().getRows().stream()
      .map(row -> row.get(column).getStringValue())
      .toList();
  }

  private void mockElements() {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
  }

  private void mockSession(boolean authenticated) {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.isAuthenticated()).thenReturn(authenticated);
  }

  private void mockLocaleEcho() {
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private void mockPublicMenuFile() {
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPublic()).thenReturn("public");
  }

  private void mockHomeScreen() {
    when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
  }

  private static ServiceData initialLoadResult(String... valueLabelPairs) {
    DataList dataList = new DataList();
    for (int i = 0; i < valueLabelPairs.length; i += 2) {
      dataList.addRow(Map.of(
        AweConstants.JSON_VALUE_PARAMETER, new CellData(valueLabelPairs[i]),
        AweConstants.JSON_LABEL_PARAMETER, new CellData(valueLabelPairs[i + 1])));
    }
    CellData data = new CellData();
    data.setValue(dataList);
    return new ServiceData().addVariable(AweConstants.ACTION_DATA, data);
  }

  // ---------------------------------------------------------------------------------------------
  // Screen retrieval and initialization
  // ---------------------------------------------------------------------------------------------

  @Test
  void getScreenInitializesPanelablesWithInitialLoadAndCachesTheScreen() throws AWException {
    Tab loaded = (Tab) new Tab().setInitialLoad("query").setTargetAction("tabQuery");
    loaded.setId("loaded");
    Tab plain = new Tab();
    plain.setId("plain");
    Screen stored = screen("screen", false);
    stored.addElement(loaded).addElement(plain);
    mockElements();
    when(aweElements.getScreen("screen")).thenReturn(stored);
    when(initialLoadDao.launchInitialLoad(any(AweThreadInitialization.class)))
      .thenReturn(CompletableFuture.completedFuture(initialLoadResult("1", "One", "2", "Two")));

    Screen result = menuService.getScreen("screen");

    assertThat(result).isNotSameAs(stored);
    assertThat(result.isInitialized()).isTrue();
    assertThat(stored.isInitialized()).isFalse();
    verify(aweElements).setScreen(result);
    List<Tab> tabs = result.getElementsByType(Tab.class);
    assertThat(tabs).extracting(Element::getId).containsExactly("loaded", "plain");
    assertThat(tabs.get(0).getTabValues()).containsExactlyInAnyOrderEntriesOf(Map.of("1", "One", "2", "Two"));
    assertThat(tabs.get(1).getTabValues()).isNull();
    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    assertThat(captor.getValue().getInitialLoadType()).isEqualTo(LoadType.QUERY);
    assertThat(captor.getValue().getTarget()).isEqualTo("tabQuery");
  }

  @Test
  void getScreenWithEmptyInitialLoadSetsEmptyTabValues() throws AWException {
    Tab loaded = (Tab) new Tab().setInitialLoad("enum").setTargetAction("tabEnum");
    Screen stored = screen("screen", false);
    stored.addElement(loaded);
    mockElements();
    when(aweElements.getScreen("screen")).thenReturn(stored);
    when(initialLoadDao.launchInitialLoad(any(AweThreadInitialization.class)))
      .thenReturn(CompletableFuture.completedFuture(initialLoadResult()));

    Screen result = menuService.getScreen("screen", "option");

    assertThat(result.getElementsByType(Tab.class).get(0).getTabValues()).isEmpty();
    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    assertThat(captor.getValue().getInitialLoadType()).isEqualTo(LoadType.ENUM);
  }

  @Test
  void getScreenFailedInitialLoadLogsWithoutInterruptingAndStillMarksTheScreenInitialized() throws AWException {
    Tab loaded = (Tab) new Tab().setInitialLoad("query").setTargetAction("broken");
    Screen stored = screen("screen", false);
    stored.addElement(loaded);
    mockElements();
    when(aweElements.getScreen("screen")).thenReturn(stored);
    when(initialLoadDao.launchInitialLoad(any(AweThreadInitialization.class)))
      .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("boom")));

    Screen result = menuService.getScreen("screen");

    assertThat(result.isInitialized()).isTrue();
    assertThat(result.getElementsByType(Tab.class).get(0).getTabValues()).isNull();
    assertThat(Thread.interrupted()).isFalse();
    verify(aweElements).getLocaleWithLanguage(eq("ERROR_MESSAGE_RETRIEVING_INITIAL_DATA"), any(), any());
    verify(aweElements).setScreen(result);
  }

  @Test
  void getScreenAlreadyInitializedReturnsACopyWithoutLoading() throws AWException {
    Screen stored = screen("screen", true);
    mockElements();
    when(aweElements.getScreen("screen")).thenReturn(stored);

    Screen result = menuService.getScreen("screen");

    assertThat(result).isNotSameAs(stored);
    assertThat(result.getId()).isEqualTo("screen");
    verify(aweElements, never()).setScreen(any(Screen.class));
    verify(initialLoadDao, never()).launchInitialLoad(any(AweThreadInitialization.class));
  }

  @Test
  void getScreenWithoutIdentifierThrowsNotFound() {
    mockElements();

    assertThatThrownBy(() -> menuService.getScreen(null, "option")).isExactlyInstanceOf(AWENotFoundException.class);
  }

  // ---------------------------------------------------------------------------------------------
  // Default and option screens
  // ---------------------------------------------------------------------------------------------

  @Test
  void getDefaultScreenReturnsTheMenuScreen() throws AWException {
    Menu menu = menu(option("opt", "other"));
    menu.setScreen("home-screen");
    mockElements();
    when(aweElements.getScreen("home-screen")).thenReturn(screen("home-screen", true));

    assertThat(menuService.getDefaultScreen(menu).getId()).isEqualTo("home-screen");
  }

  @Test
  void getDefaultScreenFromUserMenu() throws AWException {
    Menu menu = menu(option("opt", "other"));
    menu.setScreen("home-screen");
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu);
    when(aweElements.getScreen("home-screen")).thenReturn(screen("home-screen", true));

    assertThat(menuService.getDefaultScreen().getId()).isEqualTo("home-screen");
  }

  @Test
  void getDefaultScreenWithoutScreenThrowsAWException() {
    mockElements();

    assertThatThrownBy(() -> menuService.getDefaultScreen(menu(option("opt"))))
      .isExactlyInstanceOf(AWException.class);
  }

  @Test
  void getOptionScreenHomeReturnsTheMenuScreen() throws AWException {
    Menu menu = menu(option("opt", "other"));
    menu.setScreen("home-screen");
    mockElements();
    mockHomeScreen();
    when(aweElements.getScreen("home-screen")).thenReturn(screen("home-screen", true));

    assertThat(menuService.getOptionScreen("HOME", menu).getId()).isEqualTo("home-screen");
  }

  @Test
  void getOptionScreenStaticOptionReturnsItsScreen() throws AWException {
    Menu menu = menu(option("opt", "opt-screen"));
    mockElements();
    mockHomeScreen();
    when(aweElements.getScreen("opt-screen")).thenReturn(screen("opt-screen", true));

    assertThat(menuService.getOptionScreen("opt", menu).getId()).isEqualTo("opt-screen");
  }

  @Test
  void getOptionScreenDynamicOptionCallsTheScreenService() throws AWException {
    Option dynamic = option("dyn");
    dynamic.setDynamicScreen(true);
    dynamic.setDynamicScreenService("buildScreen");
    Screen generated = screen("generated", false);
    mockHomeScreen();
    when(launcherService.callService("buildScreen", null)).thenReturn(new ServiceData().setData(generated));

    assertThat(menuService.getOptionScreen("dyn", menu(dynamic))).isSameAs(generated);
  }

  @Test
  void getOptionScreenMenuScreenOptionGeneratesTheScreen() throws AWException {
    Option launcher = labelled(option("launcher", "launcher-screen"), "LAUNCHER");
    launcher.setMenuScreen(true);
    launcher.addElement(option("child", "child-screen"));
    Menu menu = menu(launcher);
    menu.defineRelationship();
    mockHomeScreen();

    Screen result = menuService.getOptionScreen("launcher", menu);

    assertThat(result.getId()).isEqualTo("launcher-screen");
    verify(context, never()).getBean(AweElements.class);
  }

  @Test
  void getOptionScreenUsesTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    mockHomeScreen();
    when(aweElements.getMenu("public")).thenReturn(menu(option("opt", "opt-screen")));
    when(aweElements.getScreen("opt-screen")).thenReturn(screen("opt-screen", true));

    assertThat(menuService.getOptionScreen("opt").getId()).isEqualTo("opt-screen");
  }

  @Test
  void getAvailableOptionScreenHomeReturnsTheMenuScreen() throws AWException {
    Menu menu = menu(option("opt", "other"));
    menu.setScreen("home-screen");
    mockElements();
    mockHomeScreen();
    when(aweElements.getScreen("home-screen")).thenReturn(screen("home-screen", true));

    assertThat(menuService.getAvailableOptionScreen("home", menu).getId()).isEqualTo("home-screen");
  }

  @Test
  void getAvailableOptionScreenUsesTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    mockHomeScreen();
    when(aweElements.getMenu("public")).thenReturn(menu(option("opt", "opt-screen")));
    when(aweElements.getScreen("opt-screen")).thenReturn(screen("opt-screen", true));

    assertThat(menuService.getAvailableOptionScreen("opt").getId()).isEqualTo("opt-screen");
  }

  // ---------------------------------------------------------------------------------------------
  // Option lookup
  // ---------------------------------------------------------------------------------------------

  @Test
  void getOptionByNameFindsTheOptionInTheGivenMenu() throws AWException {
    Option found = option("opt", "screen");

    assertThat(menuService.getOptionByName("opt", menu(found))).isSameAs(found);
  }

  @Test
  void getOptionByNameAuthenticatedFallsBackToThePublicMenu() throws AWException {
    mockElements();
    mockSession(true);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("public-opt", "public-screen")));

    Option result = menuService.getOptionByName("public-opt", menu(option("private-opt")));

    assertThat(result.getName()).isEqualTo("public-opt");
  }

  @Test
  void getOptionByNameUnknownOptionThrowsNotFound() throws AWException {
    mockElements();
    mockSession(false);
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPrivate()).thenReturn("private");
    when(aweElements.getMenu("private")).thenReturn(menu(option("private-opt")));

    assertThatThrownBy(() -> menuService.getOptionByName("unknown", menu(option("opt"))))
      .isExactlyInstanceOf(AWENotFoundException.class);
  }

  @Test
  void getOptionByNamePrivateOptionWithoutSessionThrowsSessionExpired() throws AWException {
    mockElements();
    mockSession(false);
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPrivate()).thenReturn("private");
    when(aweElements.getMenu("private")).thenReturn(menu(option("private-opt")));

    assertThatThrownBy(() -> menuService.getOptionByName("private-opt", menu(option("opt"))))
      .isExactlyInstanceOf(AWESessionException.class);
  }

  @Test
  void getOptionByNameAuthenticatedUnknownOptionThrowsNotFound() throws AWException {
    mockElements();
    mockSession(true);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("public-opt")));

    assertThatThrownBy(() -> menuService.getOptionByName("unknown", menu(option("opt"))))
      .isExactlyInstanceOf(AWENotFoundException.class);
  }

  @Test
  void getOptionByNameUsesTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("opt", "screen")));

    assertThat(menuService.getOptionByName("opt").getName()).isEqualTo("opt");
  }

  @Test
  void getAvailableOptionByNameSkipsRestrictedOptionsAndPrefersTheLastMatch() throws AWException {
    Option restricted = option("restricted");
    restricted.setRestricted(true);
    Menu privateMenu = menu(labelled(option("shared"), "PRIVATE"), restricted);
    mockElements();
    mockSession(false);
    when(aweElements.getMenu("public")).thenReturn(menu(labelled(option("shared"), "PUBLIC")));

    assertThat(menuService.getAvailableOptionByName("SHARED", privateMenu).getLabel()).isEqualTo("PUBLIC");
    assertThatThrownBy(() -> menuService.getAvailableOptionByName("restricted", privateMenu))
      .isExactlyInstanceOf(AWENotFoundException.class);
  }

  @Test
  void getAvailableOptionByNameUsesTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("opt", "screen")));

    assertThat(menuService.getAvailableOptionByName("opt").getName()).isEqualTo("opt");
  }

  @Test
  void checkOptionAddressPrivateScreenRequiresSessionAndAvailableOption() throws AWException {
    mockElements();
    mockSession(true);
    when(aweElements.getMenu("private")).thenReturn(menu(option("allowed"), option("denied")));
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_RESTRICTION_QUERY, "1", "0")).thenReturn(
      new ServiceData().setDataList(DataListUtil.fromBeanList(List.of(
        new ScreenRestriction().setOption("denied").setRestricted(true)))));

    assertThat(menuService.checkOptionAddress("screen/private/home/allowed")).isTrue();
    assertThat(menuService.checkOptionAddress("screen/private/home/denied")).isFalse();
    assertThat(menuService.checkOptionAddress("screen/private/home/unknown")).isFalse();
  }

  @Test
  void checkOptionAddressPrivateScreenWithoutSessionIsRejected() throws AWException {
    mockSession(false);

    assertThat(menuService.checkOptionAddress("screen/private/home/allowed")).isFalse();
    verify(context, never()).getBean(AweElements.class);
  }

  @Test
  void checkOptionAddressPublicScreenChecksThePublicMenu() throws AWException {
    mockElements();
    mockSession(false);
    when(aweElements.getMenu("public")).thenReturn(menu(option("signin")));

    assertThat(menuService.checkOptionAddress("screen/public/signin")).isTrue();
    assertThat(menuService.checkOptionAddress("screen/public/other")).isFalse();
  }

  @Test
  void checkOptionAddressOtherAddressesAreRejected() throws AWException {
    assertThat(menuService.checkOptionAddress("other/public/signin")).isFalse();
    assertThat(menuService.checkOptionAddress("screen")).isFalse();
  }

  // ---------------------------------------------------------------------------------------------
  // Suggestion lists
  // ---------------------------------------------------------------------------------------------

  @Test
  void getModuleListListsDistinctSortedModulesOfTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(
      module(option("one"), "beta"), module(option("two"), "alpha"), module(option("three"), "beta"), option("four")));

    ServiceData result = menuService.getModuleList();

    assertThat(column(result, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("alpha", "beta");
    assertThat(column(result, AweConstants.JSON_LABEL_PARAMETER)).containsExactly("alpha", "beta");
    assertThat(result.getDataList().getRecords()).isEqualTo(2);
  }

  @Test
  void getModuleListAuthenticatedAddsThePublicMenuOptions() throws AWException {
    mockElements();
    mockSession(true);
    when(baseConfigProperties.getFiles()).thenReturn(files);
    when(files.getMenuPrivate()).thenReturn("private-file");
    when(aweElements.getMenu("private-file")).thenReturn(menu(module(option("one"), "private-module")));
    when(aweElements.getMenu(AweConstants.PUBLIC_MENU)).thenReturn(menu(module(option("two"), "public-module")));

    ServiceData result = menuService.getModuleList();

    assertThat(column(result, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("private-module", "public-module");
  }

  @Test
  void getNameOptionListBuildsLabelsAndFiltersBySuggest() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    mockLocaleEcho();
    Screen labelledScreen = screen("screen-b", true);
    labelledScreen.setLabel("SCREEN_LABEL");
    when(aweElements.getMenu("public")).thenReturn(menu(
      labelled(option("zeta", "screen-a"), "OPTION_LABEL"),
      option("beta", "screen-b"),
      option("alpha"),
      labelled(option("zeta", "screen-c"), "DUPLICATED")));
    when(aweElements.getScreen("screen-b")).thenReturn(labelledScreen);

    ServiceData all = menuService.getNameOptionList(" ");

    assertThat(column(all, AweConstants.JSON_LABEL_PARAMETER))
      .containsExactly("OPTION_LABEL (zeta)", "SCREEN_LABEL (beta)", "alpha");
    assertThat(column(all, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("zeta", "beta", "alpha");
    assertThat(all.getDataList().getRecords()).isEqualTo(3);

    ServiceData filtered = menuService.getNameOptionList(" screen ");
    assertThat(column(filtered, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("beta");
  }

  @Test
  void getAvailableScreenListListsDistinctAvailableScreens() throws AWException {
    Option restricted = option("restricted", "restricted-screen");
    restricted.setRestricted(true);
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    mockLocaleEcho();
    when(aweElements.getMenu("public")).thenReturn(menu(
      labelled(option("one", "screen-b"), "LABEL_B"),
      labelled(option("two", "screen-a"), "LABEL_A"),
      labelled(option("three", "screen-a"), "OTHER_A"),
      option("no-screen"),
      restricted));

    ServiceData result = menuService.getAvailableScreenList("");

    assertThat(column(result, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("screen-a", "screen-b");
    assertThat(column(result, AweConstants.JSON_LABEL_PARAMETER))
      .containsExactly("LABEL_A (screen-a)", "LABEL_B (screen-b)");
    assertThat(result.getDataList().getRecords()).isEqualTo(2);
  }

  @Test
  void getAllScreenListFiltersAndSortsAllDefinedScreens() {
    mockElements();
    when(aweElements.getScreenList()).thenReturn(new LinkedHashSet<>(List.of("users", "Profiles", "audit")));

    ServiceData all = menuService.getAllScreenList("");
    ServiceData filtered = menuService.getAllScreenList("PRO");

    // Sorting is case-sensitive: upper-case labels come first
    assertThat(column(all, AweConstants.JSON_VALUE_PARAMETER)).containsExactly("Profiles", "audit", "users");
    assertThat(all.getDataList().getRecords()).isEqualTo(3);
    assertThat(column(filtered, AweConstants.JSON_LABEL_PARAMETER)).containsExactly("Profiles");
  }

  // ---------------------------------------------------------------------------------------------
  // Restrictions
  // ---------------------------------------------------------------------------------------------

  @Test
  void getScreenRestrictionsMergesDefaultProfileAndDatabaseRestrictions() throws AWException {
    Restriction restricted = new Restriction();
    restricted.setOption("users");
    Restriction allowed = new Restriction();
    allowed.setOption("audit");
    allowed.setRestrictionType(RestrictionType.A);
    Profile profile = new Profile().setRestrictions(new ArrayList<>(List.of(restricted, allowed)));
    DataList databaseRestrictions = new DataList();
    ServiceData queryOutput = new ServiceData().setDataList(databaseRestrictions);
    DataList merged = new DataList();
    DataListBuilder builder = mock(DataListBuilder.class, RETURNS_SELF);
    mockElements();
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("general");
    when(aweElements.getProfile("general")).thenReturn(profile);
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY, "1", "0"))
      .thenReturn(queryOutput);
    when(context.getBean(DataListBuilder.class)).thenReturn(builder);
    when(builder.build()).thenReturn(merged);

    ServiceData result = menuService.getScreenRestrictions(null);

    assertThat(result).isSameAs(queryOutput);
    assertThat(result.getDataList()).isSameAs(merged);
    ArgumentCaptor<DataList> captor = ArgumentCaptor.forClass(DataList.class);
    verify(builder, times(2)).addDataList(captor.capture());
    DataList base = captor.getAllValues().get(0);
    assertThat(base.getRecords()).isEqualTo(2);
    assertThat(base.getRows()).extracting(row -> row.get(AweConstants.JSON_OPTION).getStringValue())
      .containsExactly("users", "audit");
    assertThat(base.getRows()).extracting(row -> row.get(AweConstants.JSON_RESTRICTED).getObjectValue())
      .containsExactly(true, false);
    assertThat(captor.getAllValues().get(1)).isSameAs(databaseRestrictions);
  }

  @Test
  void getScreenRestrictionsUsesTheGivenProfileEvenWithoutRestrictions() throws AWException {
    DataListBuilder builder = mock(DataListBuilder.class, RETURNS_SELF);
    mockElements();
    when(aweElements.getProfile("custom")).thenReturn(new Profile());
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY, "1", "0"))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(context.getBean(DataListBuilder.class)).thenReturn(builder);
    when(builder.build()).thenReturn(new DataList());

    menuService.getScreenRestrictions("custom");

    ArgumentCaptor<DataList> captor = ArgumentCaptor.forClass(DataList.class);
    verify(builder, times(2)).addDataList(captor.capture());
    assertThat(captor.getAllValues().get(0).getRows()).isEmpty();
    verify(securityConfigProperties, never()).getDefaultRestriction();
  }

  @Test
  void getScreenRestrictionsEmptyRestrictionUsesTheDefaultProfile() throws AWException {
    DataListBuilder builder = mock(DataListBuilder.class, RETURNS_SELF);
    mockElements();
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("general");
    when(aweElements.getProfile("general")).thenReturn(new Profile());
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY, "1", "0"))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(context.getBean(DataListBuilder.class)).thenReturn(builder);
    when(builder.build()).thenReturn(new DataList());

    menuService.getScreenRestrictions("");

    verify(aweElements).getProfile("general");
  }

  @Test
  void getMenuWithAllRestrictionsAppliesRequestModuleAndScreenRestrictions() throws AWException {
    Option sameModule = module(option("same"), "sales");
    Option otherModule = module(option("other"), "admin");
    otherModule.addElement(option("other-child"));
    Option noModule = option("free");
    Option restrictedByProfile = option("profile-restricted");
    Menu menu = menu(sameModule, otherModule, noModule, restrictedByProfile);
    mockSession(true);
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweRequest.getParameter(AweConstants.SESSION_MODULE)).thenReturn(JsonNodeFactory.instance.stringNode("SALES"));
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_RESTRICTION_QUERY, "1", "0")).thenReturn(
      new ServiceData().setDataList(DataListUtil.fromBeanList(List.of(
        new ScreenRestriction().setOption("profile-restricted").setRestricted(true)))));

    Menu result = menuService.getMenuWithAllRestrictions(menu);

    assertThat(result).isSameAs(menu);
    assertThat(result.getElementsByType(Option.class).stream().filter(Option::isRestricted).map(Option::getName)
      .toList()).containsExactlyInAnyOrder("other", "other-child", "profile-restricted");
    // The session module is read eagerly (Optional.orElse) even when the request provides one
    verify(aweSession).getParameter(String.class, AweConstants.SESSION_MODULE);
  }

  @Test
  void getMenuWithAllRestrictionsFallsBackToTheSessionModule() throws AWException {
    Menu menu = menu(module(option("same"), "sales"), module(option("other"), "admin"));
    mockSession(true);
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweSession.getParameter(String.class, AweConstants.SESSION_MODULE)).thenReturn("admin");
    when(queryService.launchPrivateQuery(AweConstants.SCREEN_RESTRICTION_QUERY, "1", "0"))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    Menu result = menuService.getMenuWithAllRestrictions(menu);

    assertThat(result.getElementsByType(Option.class).stream().filter(Option::isRestricted).map(Option::getName)
      .toList()).containsExactly("same");
  }

  @Test
  void getMenuWithAllRestrictionsWithoutSessionReturnsTheMenuUntouched() throws AWException {
    Menu menu = menu(module(option("same"), "sales"));
    mockSession(false);

    assertThat(menuService.getMenuWithAllRestrictions(menu)).isSameAs(menu);
    verify(queryService, never()).launchPrivateQuery(anyString(), anyString(), anyString());
  }

  @Test
  void getMenuWithAllRestrictionsUsesTheUserMenu() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("opt")));

    Menu result = menuService.getMenuWithAllRestrictions();

    assertThat(result.getElementsByType(Option.class)).extracting(Option::getName).containsExactly("opt");
  }

  @Test
  void getMenuWithRestrictionsByTypeAndForTheUser() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu(anyString())).thenAnswer(invocation -> menu(option(invocation.getArgument(0))));

    assertThat(menuService.getMenuWithRestrictions("private").getElementsByType(Option.class))
      .extracting(Option::getName).containsExactly("private");
    assertThat(menuService.getMenuWithRestrictions().getElementsByType(Option.class))
      .extracting(Option::getName).containsExactly("public");
    verify(queryService, never()).launchPrivateQuery(anyString(), anyString(), anyString());
  }

  // ---------------------------------------------------------------------------------------------
  // Menu retrieval
  // ---------------------------------------------------------------------------------------------

  @Test
  void refreshMenuAppliesOptionActionsAndReturnsTheMenuOptions() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("one"), option("two")));

    ServiceData result = menuService.refreshMenu();

    verify(screenComponentGenerator).applyOptionActions(any(Menu.class));
    @SuppressWarnings("unchecked")
    List<Option> options = (List<Option>) result.getVariable(AweConstants.ACTION_MENU_OPTIONS).getObjectValue();
    assertThat(options).extracting(Option::getName).containsExactly("one", "two");
  }

  @Test
  void refreshMenuWrapsOptionActionFailures() throws AWException {
    mockElements();
    mockSession(false);
    mockPublicMenuFile();
    when(aweElements.getMenu("public")).thenReturn(menu(option("one")));
    IllegalStateException failure = new IllegalStateException("boom");
    doThrow(failure).when(screenComponentGenerator).applyOptionActions(any(Menu.class));

    assertThatThrownBy(() -> menuService.refreshMenu())
      .isExactlyInstanceOf(AWException.class)
      .hasCause(failure);
  }

  @Test
  void getMenuWithoutSessionThrowsAuthenticationError() {
    mockElements();
    mockPublicMenuFile();
    when(context.getBean(AweSession.class)).thenThrow(new IllegalStateException("no session"));

    assertThatThrownBy(() -> menuService.getMenu())
      .isExactlyInstanceOf(AWException.class)
      .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  void getMenuReturnsADefinedCopy() throws AWException {
    Option parent = option("parent");
    parent.addElement(option("child"));
    Menu stored = menu(parent);
    mockElements();
    when(aweElements.getMenu("private")).thenReturn(stored);

    Menu result = menuService.getMenu("private");

    assertThat(result).isNotSameAs(stored);
    Option child = result.getOptionByName("child");
    assertThat(child.getParent()).isSameAs(result.getOptionByName("parent"));
  }
}
