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
    catalog.put("findGridCell(grid)", () -> i.findGridCell(GRID, TEXT));
    catalog.put("findGridCell(any grid)", () -> i.findGridCell(null, TEXT));
    catalog.put("getCheckbox", () -> i.getCheckbox(criterion));
    catalog.put("getCheckbox(cell)", () -> i.getCheckbox(cell));
    catalog.put("getCheckboxChecked(checked)", () -> i.getCheckboxChecked(CRITERION, true));
    catalog.put("getCheckboxChecked(unchecked)", () -> i.getCheckboxChecked(CRITERION, false));
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
