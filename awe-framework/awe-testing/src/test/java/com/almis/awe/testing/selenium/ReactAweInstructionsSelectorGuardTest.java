package com.almis.awe.testing.selenium;

import java.util.List;
import java.util.Map;

/**
 * Selector guard for {@link ReactAweInstructions}: no selector can depend on PrimeReact internals (its {@code p-}
 * classes, its icons, its ARIA roles and labels) or on the AngularJS libraries. Components are located through the
 * {@code data-testid} hooks and AWE's own attributes and ids.
 */
class ReactAweInstructionsSelectorGuardTest extends AbstractFrontEndSelectorGuardTest {

  @Override
  protected IAweFrontEndInstructions instructions() {
    return new ReactAweInstructions();
  }

  @Override
  protected String clientVocabularyFile() {
    return TestIdsVocabularyTest.REACT_VOCABULARY_FILE;
  }

  @Override
  protected List<ForbiddenToken> forbiddenTokens() {
    List<ForbiddenToken> tokens = defaultForbiddenTokens();
    // Any class of PrimeReact ("p-" prefix) and the ones that the previous selectors used
    tokens.add(ForbiddenToken.regex("p- class prefix (PrimeReact)", "(?<![\\w-])p-[a-z]"));
    tokens.add(ForbiddenToken.regex("pi- icon prefix (PrimeIcons)", "(?<![\\w-])pi-[a-z]"));
    List.of("p-datatable", "p-treetable", "p-dropdown", "p-autocomplete", "p-datepicker", "p-monthpicker", "p-yearpicker",
        "p-menuitem", "p-submenu-list", "p-highlight", "p-checkbox", "p-toast", "p-progress-spinner", "p-button-label",
        "p-inputgroup", "p-tab-title", "p-overlay-badge", "p-disabled")
      .forEach(token -> tokens.add(ForbiddenToken.literal(token)));
    // Accessibility attributes of the library (they change with its version) and its structural markup
    tokens.add(ForbiddenToken.literal("aria-"));
    tokens.add(ForbiddenToken.regex("[role=...] / @role (ARIA roles and AWE roles of the library markup)", "\\[role\\s*[=~|^$*]|@role"));
    tokens.add(ForbiddenToken.regex("table / list tags (th, td, tr, tbody, li, ul)", "(?<![\\w-])(?:th|td|tr|tbody|li|ul)(?![\\w-])"));
    return tokens;
  }

  @Override
  protected Map<String, String> allowedSelectors() {
    return Map.of(
      "containsText", "The caller supplies the class to search, so it cannot be replaced by a hook",
      "getPopover", "The React client has no popover (its help is a PrimeReact tooltip that ignores the mouse), so the "
        + "Bootstrap selector of the AngularJS client never matches and the mouse is never moved. There is nothing to hook",
      "getTabMenu", "The React tab list has no \"more\" menu (no tabdrop), so there is no element to locate",
      "getTabMenuDropdown", "The React tab list has no \"more\" menu (no tabdrop), so there is no element to locate",
      "getTabMenuDropdownOption", "The React tab list has no \"more\" menu (no tabdrop), so there is no element to locate");
  }
}
