package com.almis.awe.testing.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard for the Selenium instruction layer (Decision 4 of #765).
 *
 * <p>Every selector produced by a front end instruction class must identify components through the stable test hooks
 * ({@code data-testid} and AWE's own attributes and ids), never through the classes of the libraries that render the
 * component. If a library is replaced, the selectors must keep working.</p>
 *
 * <p>The guard calls every public selector-producing method of the interface {@link IAweFrontEndInstructions} with
 * representative arguments and fails when a selector (CSS, xpath or id) contains a forbidden library token.
 * The values of the {@code data-testid} hooks are masked before scanning, because the vocabulary legitimately reuses
 * names such as {@code datepicker} or {@code tabdrop}.</p>
 *
 * <p>To guard another implementation, extend this class, return the instructions from {@link #instructions()} and, if
 * the implementation renders a different component library, add its tokens in {@link #forbiddenTokens()}
 * (for instance the PrimeReact {@code p-} classes for {@code ReactAweInstructions}).</p>
 *
 * <p>Not coverable by a hook, and therefore not scanned: {@code containsText(clazz, text)}, because the caller supplies
 * the class, and the {@code fa-mouse-pointer} follower that {@code SeleniumUtilities.showMouse} injects, which is a
 * script and not a locator (see {@link #allowedSelectors()}).</p>
 */
public abstract class AbstractFrontEndSelectorGuardTest {

  /** Representative arguments */
  protected static final String CRITERION = "Crit";
  protected static final String GRID = "Grd";
  protected static final String WINDOW = "Win";
  protected static final String TABS = "Tabs";
  protected static final String ROW = "Row";
  protected static final String COLUMN = "Col";
  protected static final String LABEL = "Label";
  protected static final String TEXT = "Text";

  /** Matches the value of a test hook so it is not scanned as a library token */
  private static final Pattern HOOK_VALUE = Pattern.compile("data-testid(?:-owner)?\\s*=\\s*(['\"])[^'\"]*\\1");

  /** Matches the vocabulary value of a hook (not of an owner attribute) in a selector */
  private static final Pattern HOOK_ID = Pattern.compile("data-testid\\s*=\\s*(['\"])([^'\"]*)\\1");

  /** Matches the data attributes (the hook and the state attributes) that a selector uses */
  private static final Pattern DATA_ATTRIBUTE = Pattern.compile("data-[a-z]+(?:-[a-z]+)*");

  /**
   * Forbidden token
   *
   * @param name    Token as documented
   * @param pattern Pattern that detects it
   */
  record ForbiddenToken(String name, Pattern pattern) {
    static ForbiddenToken literal(String token) {
      return new ForbiddenToken(token, Pattern.compile(Pattern.quote(token)));
    }

    static ForbiddenToken regex(String name, String regex) {
      return new ForbiddenToken(name, Pattern.compile(regex));
    }
  }

  /**
   * Instructions to guard
   *
   * @return Front end instructions
   */
  protected abstract IAweFrontEndInstructions instructions();

  /**
   * Path of the JavaScript vocabulary of the client that renders the hooks (relative to the {@code awe-framework}
   * directory), whose {@code TestIds} and {@code TestAttributes} must declare everything the selectors use
   *
   * @return Path of the vocabulary file
   */
  protected abstract String clientVocabularyFile();

  /**
   * Library tokens that selectors cannot contain. Subclasses may add the tokens of their own component library
   *
   * @return Forbidden tokens
   */
  protected List<ForbiddenToken> forbiddenTokens() {
    return defaultForbiddenTokens();
  }

  /**
   * Library tokens shared by every implementation
   *
   * @return Forbidden tokens (a modifiable copy)
   */
  static List<ForbiddenToken> defaultForbiddenTokens() {
    return new ArrayList<>(List.of(
      ForbiddenToken.literal("select2"),
      ForbiddenToken.literal("datepicker"),
      ForbiddenToken.literal("tabdrop"),
      ForbiddenToken.literal("ui-grid"),
      ForbiddenToken.literal("fa-"),
      ForbiddenToken.literal(".modal"),
      ForbiddenToken.literal("modal-"),
      ForbiddenToken.literal(".nav-tabs"),
      ForbiddenToken.literal("nav-tabs"),
      ForbiddenToken.literal(".dropdown-menu"),
      ForbiddenToken.literal(".popover"),
      ForbiddenToken.literal(".alert"),
      // "loading-bar" is a valid hook value, but not as a class or id
      ForbiddenToken.regex("#loading-bar / .loading-bar / By.id(loading-bar)", "[#.]loading-bar|By\\.id: loading-bar"),
      ForbiddenToken.literal(".btn"),
      ForbiddenToken.literal(".active"),
      ForbiddenToken.literal(".selected"),
      ForbiddenToken.literal(".open"),
      ForbiddenToken.literal(".ng-hide"),
      // AWE classes and library defaults that now have a hook
      ForbiddenToken.literal(".dropdown-toggle"),
      ForbiddenToken.literal(".loader"),
      ForbiddenToken.literal(".grid-loader"),
      ForbiddenToken.literal(".grid-row-save"),
      ForbiddenToken.literal(".context-menu"),
      ForbiddenToken.literal(".tree-icon"),
      ForbiddenToken.literal(".alert-zone")
    ));
  }

  /**
   * Selectors that the guard cannot check, by method name, with the reason. Every entry must be justified
   *
   * @return Reason by method name
   */
  protected Map<String, String> allowedSelectors() {
    return Map.of("containsText", "The caller supplies the class to search, so it cannot be replaced by a hook");
  }

  /**
   * Fragments of selectors that are tolerated temporarily, with the reason. They are removed before scanning, so the
   * rest of the selector is still checked
   *
   * @return Reason by fragment
   */
  protected Map<String, String> allowedFragments() {
    return Map.of();
  }

  /**
   * Methods of the implementation that return a selector but are not part of the representative catalog
   *
   * @return Method names
   */
  protected Set<String> methodsOutsideCatalog() {
    return Set.of();
  }

  /**
   * Every selector-producing method of the interface, with representative arguments. It is explicit on purpose:
   * synthesizing arguments by reflection would make the guard fragile, and
   * {@link #shouldCoverEverySelectorMethodOfTheImplementation()} fails when a method is missing here
   *
   * @return Selectors by call
   */
  protected Map<String, Supplier<Object>> catalog() {
    IAweFrontEndInstructions i = instructions();
    String criterion = i.getCriterionCss(CRITERION);
    String cell = i.getParentCss(GRID, ROW, COLUMN);
    Map<String, Supplier<Object>> catalog = new LinkedHashMap<>();
    catalog.put("getCriterionCss", () -> criterion);
    catalog.put("getParentCss(grid, null, null)", () -> i.getParentCss(GRID, null, null));
    catalog.put("getParentCss(grid, null, column)", () -> i.getParentCss(GRID, null, COLUMN));
    catalog.put("getParentCss(grid, row, column)", () -> cell);
    catalog.put("getEditingParentCss", () -> i.getEditingParentCss(GRID, COLUMN));
    catalog.put("getCriterionInput", () -> i.getCriterionInput(criterion));
    catalog.put("getCriterionInput(cell)", () -> i.getCriterionInput(cell));
    catalog.put("getLoaderSelector", i::getLoaderSelector);
    catalog.put("getLoadingBar", i::getLoadingBar);
    catalog.put("getPopover", i::getPopover);
    catalog.put("containsText", () -> i.containsText(LABEL, TEXT));
    catalog.put("getMessage", () -> i.getMessage("danger"));
    catalog.put("getMenuOption", () -> i.getMenuOption(LABEL));
    catalog.put("getMenuOpenedChildren", () -> i.getMenuOpenedChildren(LABEL));
    catalog.put("getMenuDropdown", i::getMenuDropdown);
    catalog.put("getMenuActiveOption", () -> i.getMenuActiveOption(LABEL));
    catalog.put("getButton", () -> i.getButton("ButId"));
    catalog.put("getRequiredPostLoginShellControls", i::getRequiredPostLoginShellControls);
    catalog.put("getOptionalPostLoginShellControls", i::getOptionalPostLoginShellControls);
    catalog.put("getInfoButton", () -> i.getInfoButton("Info"));
    catalog.put("getTreeButton", () -> i.getTreeButton(GRID, ROW));
    catalog.put("getTreeButtonLoader", i::getTreeButtonLoader);
    catalog.put("getTab", () -> i.getTab("Tab"));
    catalog.put("getTab(label)", () -> i.getTab("Tab", LABEL));
    catalog.put("getTabActive", () -> i.getTabActive("Tab", LABEL));
    catalog.put("getTabMenu", () -> i.getTabMenu("Tab"));
    catalog.put("getTabMenuDropdown", () -> i.getTabMenuDropdown("Tab"));
    catalog.put("getTabMenuDropdownOption", () -> i.getTabMenuDropdownOption("Tab", LABEL));
    catalog.put("getContextButton", () -> i.getContextButton("Ctx"));
    catalog.put("getDatepicker", i::getDatepicker);
    catalog.put("getDateCriterion", () -> i.getDateCriterion(criterion));
    catalog.put("getActiveDatepicker", i::getActiveDatepicker);
    catalog.put("getCellFromDatepicker(day)", () -> i.getCellFromDatepicker("day", "23"));
    catalog.put("getCellFromDatepicker(month)", () -> i.getCellFromDatepicker("month", "Oct"));
    catalog.put("getCellFromDatepicker(year)", () -> i.getCellFromDatepicker("year", "1978"));
    catalog.put("getGridScrollZone", () -> i.getGridScrollZone(GRID));
    catalog.put("getGridLoaderSelector", i::getGridLoaderSelector);
    catalog.put("getGridHeader", () -> i.getGridHeader(GRID, COLUMN));
    catalog.put("getGridCell(row)", () -> i.getGridCell(GRID, ROW, COLUMN));
    catalog.put("getGridCell(selected)", () -> i.getGridCell(GRID, null, COLUMN));
    catalog.put("getGridSaveButton", i::getGridSaveButton);
    catalog.put("getGridSaveButton(grid)", () -> i.getGridSaveButton(GRID));
    catalog.put("getGridCellText(row)", () -> i.getGridCellText(GRID, ROW, COLUMN, TEXT));
    catalog.put("getGridCellText(selected)", () -> i.getGridCellText(GRID, null, COLUMN, TEXT));
    catalog.put("getGridCellButton", () -> i.getGridCellButton(GRID, ROW, COLUMN));
    catalog.put("findGridCell(grid)", () -> i.findGridCell(GRID, TEXT));
    catalog.put("findGridCell(any grid)", () -> i.findGridCell(null, TEXT));
    catalog.put("findGridRowSelection(grid)", () -> i.findGridRowSelection(GRID, TEXT));
    catalog.put("findGridRowSelection(any grid)", () -> i.findGridRowSelection(null, TEXT));
    catalog.put("findGridSelectedRow(grid)", () -> i.findGridSelectedRow(GRID, TEXT));
    catalog.put("findGridSelectedRow(any grid)", () -> i.findGridSelectedRow(null, TEXT));
    catalog.put("findSelectedGridRow(grid)", () -> i.findSelectedGridRow(GRID, TEXT));
    catalog.put("findSelectedGridRow(any grid)", () -> i.findSelectedGridRow(null, TEXT));
    catalog.put("getGridRowOfCell", i::getGridRowOfCell);
    catalog.put("getGridEditingRow(grid)", () -> i.getGridEditingRow(GRID, ROW));
    catalog.put("getGridEditingRow(any grid)", () -> i.getGridEditingRow(null, ROW));
    catalog.put("getCheckbox", () -> i.getCheckbox(criterion));
    catalog.put("getCheckbox(cell)", () -> i.getCheckbox(cell));
    catalog.put("getCheckboxChecked(checked)", () -> i.getCheckboxChecked(CRITERION, true));
    catalog.put("getCheckboxChecked(unchecked)", () -> i.getCheckboxChecked(CRITERION, false));
    catalog.put("getCheckboxOption", () -> i.getCheckboxOption(CRITERION, "Opt1"));
    catalog.put("getSelectChoice", () -> i.getSelectChoice(criterion));
    catalog.put("getSelectLoader", () -> i.getSelectLoader(criterion));
    catalog.put("getSelectDropdownList", i::getSelectDropdownList);
    catalog.put("getSelectDropdownListElements", i::getSelectDropdownListElements);
    catalog.put("getSelectDropdownListFirstElement", i::getSelectDropdownListFirstElement);
    catalog.put("getSelectDropdownListLastElement", i::getSelectDropdownListLastElement);
    catalog.put("getSelectResult", () -> i.getSelectResult(TEXT));
    catalog.put("getSelectChosen", () -> i.getSelectChosen(CRITERION));
    catalog.put("getSelectMultipleTextContainer", () -> i.getSelectMultipleTextContainer(CRITERION));
    catalog.put("getSuggestChoice", () -> i.getSuggestChoice(criterion));
    catalog.put("getSuggestLoader", () -> i.getSuggestLoader(criterion));
    catalog.put("getSuggest", () -> i.getSuggest(criterion));
    catalog.put("getSuggestInput", () -> i.getSuggestInput(criterion));
    catalog.put("getSuggestChosen", () -> i.getSuggestChosen(CRITERION));
    catalog.put("getSuggestDropdownList", i::getSuggestDropdownList);
    catalog.put("getSuggestDropdownListLastElement", i::getSuggestDropdownListLastElement);
    catalog.put("getSuggestMultipleInput", () -> i.getSuggestMultipleInput(criterion));
    catalog.put("getSuggestMultipleChoiceClose", () -> i.getSuggestMultipleChoiceClose(criterion));
    catalog.put("getSuggestResult", () -> i.getSuggestResult(TEXT));
    catalog.put("getLoggedUser", i::getLoggedUser);
    catalog.put("getUserMenuButtonId", i::getUserMenuButtonId);
    catalog.put("getLoginScreenMarker", i::getLoginScreenMarker);
    catalog.put("getLoginScreenText", i::getLoginScreenText);
    catalog.put("getMessageTitle", () -> i.getMessageTitle("warning"));
    catalog.put("getMessageText", () -> i.getMessageText("warning"));
    catalog.put("getMessagePopover", () -> i.getMessagePopover(CRITERION));
    catalog.put("getMessagePopoverTitle", () -> i.getMessagePopoverTitle(CRITERION));
    catalog.put("getMessagePopoverText", () -> i.getMessagePopoverText(CRITERION));
    catalog.put("getMessagePopoverMask", i::getMessagePopoverMask);
    catalog.put("getMenuOptionItem", () -> i.getMenuOptionItem(LABEL));
    catalog.put("getLauncherCardDescription", () -> i.getLauncherCardDescription("card"));
    catalog.put("getBreadcrumb", i::getBreadcrumb);
    catalog.put("getCriterionLabel", () -> i.getCriterionLabel(CRITERION));
    catalog.put("getCriterionUnit", () -> i.getCriterionUnit(CRITERION));
    catalog.put("getValidationError", i::getValidationError);
    catalog.put("getEnabledDatepickerDay", i::getEnabledDatepickerDay);
    catalog.put("getActiveWizardStepNumber", i::getActiveWizardStepNumber);
    catalog.put("getActiveWizardStep", () -> i.getActiveWizardStep("2"));
    catalog.put("getActiveWizardStepOf", () -> i.getActiveWizardStepOf("wizard"));
    catalog.put("getTagList", () -> i.getTagList("Tags"));
    catalog.put("getChart", () -> i.getChart("Chr"));
    catalog.put("getInvalidTab", () -> i.getInvalidTab(CRITERION));
    catalog.put("getInvalidWizardStep", () -> i.getInvalidWizardStep(CRITERION));
    catalog.put("getDateRangeFrom", () -> i.getDateRangeFrom(CRITERION));
    catalog.put("getDateRangeTo", () -> i.getDateRangeTo(CRITERION));
    catalog.put("getDateRangePresets", () -> i.getDateRangePresets(CRITERION));
    catalog.put("getDateRangePreset", () -> i.getDateRangePreset(CRITERION, "1M"));
    catalog.put("getDateRangePresetActive", () -> i.getDateRangePresetActive(CRITERION, "1M"));
    catalog.put("getUploaderDropZone", () -> i.getUploaderDropZone(CRITERION));
    catalog.put("getUploaderFilename", () -> i.getUploaderFilename(CRITERION));
    catalog.put("getUploaderPreview", () -> i.getUploaderPreview(CRITERION));
    catalog.put("getUploaderClear", () -> i.getUploaderClear(CRITERION));
    catalog.put("getUploaderCapture", () -> i.getUploaderCapture(CRITERION));
    catalog.put("getUploaderCaptureInput", () -> i.getUploaderCaptureInput(CRITERION));
    catalog.put("getMap", () -> i.getMap("Map"));
    catalog.put("getMapMarkers", () -> i.getMapMarkers("Map"));
    catalog.put("getMapMarker", () -> i.getMapMarker("Map", "1"));
    catalog.put("getMapMarkerSelected", () -> i.getMapMarkerSelected("Map", "1"));
    catalog.put("getMapRoute", () -> i.getMapRoute("Map"));
    catalog.put("getChartZoomed", () -> i.getChartZoomed("Chr"));
    catalog.put("getPivotTable", () -> i.getPivotTable("Pvt"));
    catalog.put("getLogViewer", i::getLogViewer);
    catalog.put("getFileViewer", i::getFileViewer);
    catalog.put("getEmbeddedFrame", i::getEmbeddedFrame);
    catalog.put("getOpenDialog", () -> i.getOpenDialog("Dia"));
    catalog.put("getDialogClose", () -> i.getDialogClose("Dia"));
    catalog.put("getAnyButton", () -> i.getAnyButton("ButId"));
    catalog.put("getDisabledButton", () -> i.getDisabledButton("ButId"));
    catalog.put("getGrid", () -> i.getGrid(GRID));
    catalog.put("getGridHeaderCheckboxSelected", () -> i.getGridHeaderCheckboxSelected(GRID));
    catalog.put("getContextMenu", i::getContextMenu);
    catalog.put("getContextMenuMask", i::getContextMenuMask);
    catalog.put("getGridPageSize", i::getGridPageSize);
    catalog.put("getGridPaginationInfo", i::getGridPaginationInfo);
    catalog.put("getGridPageNext", i::getGridPageNext);
    catalog.put("getGridGotoPage", i::getGridGotoPage);
    catalog.put("getGridEmpty", i::getGridEmpty);
    catalog.put("getGridEmptyTitle", i::getGridEmptyTitle);
    catalog.put("getGridEmptyText", i::getGridEmptyText);
    catalog.put("getGridEmptyAction", i::getGridEmptyAction);
    catalog.put("getGridRadioChecked", () -> i.getGridRadioChecked(GRID, ROW, COLUMN, "B", true));
    catalog.put("getGridRadioOption", () -> i.getGridRadioOption(GRID, ROW, COLUMN, "B"));
    catalog.put("getGridRadioInput", () -> i.getGridRadioInput(GRID, ROW, COLUMN));
    catalog.put("getGridSparkline", () -> i.getGridSparkline(GRID, ROW, COLUMN));
    catalog.put("getGridCellWithClass", () -> i.getGridCellWithClass(GRID, ROW, COLUMN, "scale-seq-1"));
    catalog.put("getGridRow", () -> i.getGridRow(GRID, ROW));
    catalog.put("getGridRowFirstCell", () -> i.getGridRowFirstCell(GRID, 2));
    catalog.put("getGridRowGrip", () -> i.getGridRowGrip(GRID, 2));
    catalog.put("getGridRowSelected", () -> i.getGridRowSelected(GRID, ROW, false));
    catalog.put("getGridRowDetailToggle(row)", () -> i.getGridRowDetailToggle(GRID, ROW));
    catalog.put("getGridRowDetailToggle(any row)", () -> i.getGridRowDetailToggle(GRID, null));
    catalog.put("getGridRowDetail(row)", () -> i.getGridRowDetail(GRID, ROW));
    catalog.put("getGridRowDetail(any row)", () -> i.getGridRowDetail(GRID, null));
    catalog.put("getGridRowDetailWithClass", () -> i.getGridRowDetailWithClass(GRID, ROW, "row-detail-card"));
    catalog.put("getGridRowDetailNestedGrid", () -> i.getGridRowDetailNestedGrid(GRID, ROW, "GrdCus-Row"));
    catalog.put("getResizable", () -> i.getResizable(GRID, "right"));
    catalog.put("getResizableHandle", () -> i.getResizableHandle(GRID, "bottom"));
    catalog.put("getResizableNeighbour(handle side)", () -> i.getResizableNeighbour(GRID, "right", true));
    catalog.put("getResizableNeighbour(other side)", () -> i.getResizableNeighbour(GRID, "top", false));
    catalog.put("getDialogPanel", () -> i.getDialogPanel("Dia"));
    catalog.put("getSidePanelCss", () -> i.getSidePanelCss("Pnl"));
    catalog.put("getOpenSidePanel", () -> i.getOpenSidePanel("Pnl"));
    catalog.put("getSidePanelClose", () -> i.getSidePanelClose("Pnl"));
    catalog.put("getSidePanelPanel", () -> i.getSidePanelPanel("Pnl"));
    catalog.put("getButtonCss", () -> i.getButtonCss("ButId"));
    catalog.put("getPopoverCss", () -> i.getPopoverCss("Pop"));
    catalog.put("getOpenPopover", () -> i.getOpenPopover("Pop"));
    catalog.put("getPopoverClose", () -> i.getPopoverClose("Pop"));
    catalog.put("getCardCss", () -> i.getCardCss("Crd"));
    catalog.put("getCardValue", () -> i.getCardValue("Crd"));
    catalog.put("getCardDelta", () -> i.getCardDelta("Crd"));
    catalog.put("getCardDeltaText", () -> i.getCardDeltaText("Crd"));
    catalog.put("getCardProgress", () -> i.getCardProgress("Crd"));
    catalog.put("getCardChip", () -> i.getCardChip("Crd"));
    catalog.put("getCardBodyComponent", () -> i.getCardBodyComponent("Crd", "Grd"));
    catalog.put("getGridColumnsButton", () -> i.getGridColumnsButton(GRID));
    catalog.put("getGridMaximizeButton", () -> i.getGridMaximizeButton(GRID));
    catalog.put("getGridToolbarMaximized", () -> i.getGridToolbarMaximized(GRID, true));
    catalog.put("getWindow", () -> i.getWindow(WINDOW));
    catalog.put("getWindowMaximized", () -> i.getWindow(WINDOW, true));
    catalog.put("getWindowMaximizeButton", () -> i.getWindowMaximizeButton(WINDOW));
    catalog.put("getTabComponent", () -> i.getTabComponent(TABS));
    catalog.put("getTabComponentMaximized", () -> i.getTabComponent(TABS, true));
    catalog.put("getTabMaximizeButton", () -> i.getTabMaximizeButton(TABS));
    catalog.put("getGridColumnsDialog", () -> i.getGridColumnsDialog(GRID));
    catalog.put("getGridColumnsPanel", () -> i.getGridColumnsPanel(GRID, "side-panel"));
    catalog.put("getGridColumnsBusy", () -> i.getGridColumnsBusy(GRID));
    catalog.put("getGridColumnsCounter", () -> i.getGridColumnsCounter(GRID, "24", "26"));
    catalog.put("getGridColumnsItem", () -> i.getGridColumnsItem(GRID, COLUMN));
    catalog.put("getGridColumnsCheckbox", () -> i.getGridColumnsCheckbox(GRID, COLUMN));
    catalog.put("getGridColumnsMoveUp", () -> i.getGridColumnsMoveUp(GRID, COLUMN));
    catalog.put("getGridColumnsMoveDown", () -> i.getGridColumnsMoveDown(GRID, COLUMN));
    catalog.put("getGridColumnsShowAll", () -> i.getGridColumnsShowAll(GRID));
    catalog.put("getGridColumnsReset", () -> i.getGridColumnsReset(GRID));
    catalog.put("getGridColumnsSave", () -> i.getGridColumnsSave(GRID));
    catalog.put("getGridColumnsCancel", () -> i.getGridColumnsCancel(GRID));
    catalog.put("getGridColumnsClose", () -> i.getGridColumnsClose(GRID));
    catalog.put("getGridGroupHeader", () -> i.getGridGroupHeader(GRID, "Group"));
    catalog.put("getGridIcon", () -> i.getGridIcon(GRID, COLUMN, "plus"));
    catalog.put("getGridCells", () -> i.getGridCells(GRID));
    catalog.put("getGridColumnCells", () -> i.getGridColumnCells(GRID, COLUMN));
    catalog.put("getGridCellsActiveContent", () -> i.getGridCellsActiveContent(GRID));
    catalog.put("getColumnSuccessIcon", () -> i.getColumnSuccessIcon(COLUMN));
    catalog.put("getTreeRow", () -> i.getTreeRow(GRID, ROW));
    catalog.put("getTreeRowIcon", () -> i.getTreeRowIcon(GRID, ROW));
    catalog.put("getDeletedTreeRow", () -> i.getDeletedTreeRow(GRID, ROW));
    catalog.put("getSelectOption", () -> i.getSelectOption(2));
    catalog.put("getSelectOptions", i::getSelectOptions);
    catalog.put("getUserMenuOption", () -> i.getUserMenuOption("dark-mode"));
    catalog.put("getThemedPage", () -> i.getThemedPage("sunset"));
    catalog.put("getColorModePage(dark)", () -> i.getColorModePage("dark"));
    catalog.put("getColorModePage(light)", () -> i.getColorModePage("light"));
    return catalog;
  }

  @Test
  void shouldNotProduceSelectorsWithLibraryTokens() {
    List<String> violations = new ArrayList<>();
    catalog().forEach((call, selector) -> {
      if (allowedSelectors().containsKey(methodName(call))) {
        return;
      }
      for (String description : describe(selector.get())) {
        String checked = description;
        for (String fragment : allowedFragments().keySet()) {
          checked = checked.replace(fragment, "");
        }
        findForbiddenTokens(checked, forbiddenTokens())
          .forEach(token -> violations.add(call + " -> " + description + "   [forbidden: " + token.name() + "]"));
      }
    });

    assertThat(violations)
      .as("Selectors that depend on library internals (%s): use data-testid hooks or AWE attributes", violations.size())
      .isEmpty();
  }

  @Test
  void shouldOnlyUseHooksAndStateAttributesThatTheClientRenders() throws IOException {
    Set<String> hooks = new TreeSet<>();
    Set<String> attributes = new TreeSet<>();
    for (String description : describeCatalog()) {
      Matcher hook = HOOK_ID.matcher(description);
      while (hook.find()) {
        hooks.add(hook.group(2));
      }
      Matcher attribute = DATA_ATTRIBUTE.matcher(description);
      while (attribute.find()) {
        attributes.add(attribute.group());
      }
    }

    Map<String, String> vocabulary = TestIdsVocabularyTest.readJavaScriptConstant(clientVocabularyFile(), "TestIds");
    Map<String, String> clientAttributes = TestIdsVocabularyTest.readJavaScriptConstant(clientVocabularyFile(), "TestAttributes");

    assertThat(hooks).as("Hooks used by the selectors").isNotEmpty();
    assertThat(vocabulary.values()).as("Hooks used by the selectors must be rendered by the client (%s)", clientVocabularyFile())
      .containsAll(hooks);
    assertThat(clientAttributes.values()).as("State attributes used by the selectors must be rendered by the client")
      .containsAll(attributes);
  }

  @Test
  void shouldCoverEverySelectorMethodOfTheImplementation() {
    Set<String> cataloged = catalog().keySet().stream().map(AbstractFrontEndSelectorGuardTest::methodName)
      .collect(Collectors.toSet());

    Set<String> missing = Arrays.stream(instructions().getClass().getMethods())
      .filter(method -> !Modifier.isStatic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass()))
      .filter(AbstractFrontEndSelectorGuardTest::producesSelector)
      .map(Method::getName)
      .filter(name -> !cataloged.contains(name) && !methodsOutsideCatalog().contains(name))
      .collect(Collectors.toCollection(TreeSet::new));

    assertThat(missing).as("Selector methods missing in the guard catalog").isEmpty();
  }

  @Test
  void shouldJustifyEveryAllowedSelector() {
    Set<String> cataloged = catalog().keySet().stream().map(AbstractFrontEndSelectorGuardTest::methodName)
      .collect(Collectors.toSet());

    allowedSelectors().forEach((name, reason) -> {
      assertThat(cataloged).as("Allowed selector %s must exist in the catalog", name).contains(name);
      assertThat(reason).as("Reason for %s", name).isNotBlank();
    });

    List<String> produced = catalog().values().stream().flatMap(selector -> describe(selector.get()).stream())
      .collect(Collectors.toList());
    allowedFragments().forEach((fragment, reason) -> {
      assertThat(produced).as("Allowed fragment %s must be produced by a selector", fragment)
        .anyMatch(description -> description.contains(fragment));
      assertThat(reason).as("Reason for %s", fragment).isNotBlank();
    });
  }

  /**
   * Find the forbidden tokens of a selector. The values of the hooks are masked first
   *
   * @param selector Selector (or its string form)
   * @param tokens   Forbidden tokens
   * @return Tokens found
   */
  static List<ForbiddenToken> findForbiddenTokens(String selector, List<ForbiddenToken> tokens) {
    String scanned = HOOK_VALUE.matcher(selector).replaceAll("data-testid='HOOK'");
    return tokens.stream().filter(token -> token.pattern().matcher(scanned).find()).collect(Collectors.toList());
  }

  /**
   * Description of the selectors of the catalog that are checked (the allowed selectors are left out)
   *
   * @return Descriptions
   */
  private List<String> describeCatalog() {
    List<String> descriptions = new ArrayList<>();
    catalog().forEach((call, selector) -> {
      if (!allowedSelectors().containsKey(methodName(call))) {
        descriptions.addAll(describe(selector.get()));
      }
    });
    return descriptions;
  }

  private static String methodName(String call) {
    int parenthesis = call.indexOf('(');
    return parenthesis < 0 ? call : call.substring(0, parenthesis);
  }

  private static boolean producesSelector(Method method) {
    Class<?> type = method.getReturnType();
    return By.class.equals(type) || String.class.equals(type) || List.class.equals(type);
  }

  private static List<String> describe(Object selector) {
    if (selector instanceof List<?> list) {
      return list.stream().map(String::valueOf).collect(Collectors.toList());
    }
    return List.of(String.valueOf(selector));
  }
}
