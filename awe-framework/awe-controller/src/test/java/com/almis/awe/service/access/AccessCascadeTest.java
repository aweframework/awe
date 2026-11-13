package com.almis.awe.service.access;

import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Rule;
import com.almis.awe.service.access.AccessCascade.Warning;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.service.access.AccessCascade.Layer.BASE_PROFILE;
import static com.almis.awe.service.access.AccessCascade.Layer.GLOBAL;
import static com.almis.awe.service.access.AccessCascade.Layer.MODULE;
import static com.almis.awe.service.access.AccessCascade.Layer.NONE;
import static com.almis.awe.service.access.AccessCascade.Layer.PROFILE;
import static com.almis.awe.service.access.AccessCascade.Layer.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cascade of the access window: the verdict of every option, the layer that decides it, the own rule of the subject
 * and the warnings, with the same order and propagation as the real menu of a session (ScreenRestrictionGenerator)
 */
class AccessCascadeTest {

  /**
   * tools (module Tools) > sites, users > user-detail; settings > security; reports (module Reports) > sales
   */
  private static Menu menu() {
    return menu(
      option("tools", "Tools", option("sites", null), option("users", null, option("user-detail", null))),
      option("settings", null, option("security", null)),
      option("reports", "Reports", option("sales", null)));
  }

  @Test
  void withoutRulesEveryOptionIsAllowedByDefault() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(), PROFILE);

    assertThat(result.options()).extracting(access -> access.option().getName())
      .containsExactly("tools", "sites", "users", "user-detail", "settings", "security", "reports", "sales");
    assertThat(result.options()).allSatisfy(access -> {
      assertThat(access.allowed()).isTrue();
      assertThat(access.layer()).isEqualTo(NONE);
      assertThat(access.ruleOption()).isNull();
      assertThat(access.inherited()).isFalse();
      assertThat(access.ownRule()).isEmpty();
      assertThat(access.warning()).isNull();
    });
    assertThat(access(result, "users").parent().getName()).isEqualTo("tools");
    assertThat(access(result, "tools").parent()).isNull();
  }

  @Test
  void aRuleDecidesItsOptionAndIsInheritedByTheDescendants() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(deny(GLOBAL, "tools")), PROFILE);

    assertThat(access(result, "tools")).extracting(OptionAccess::allowed, OptionAccess::layer, OptionAccess::inherited)
      .containsExactly(false, GLOBAL, false);
    assertThat(access(result, "user-detail")).extracting(OptionAccess::allowed, OptionAccess::layer, OptionAccess::inherited)
      .containsExactly(false, GLOBAL, true);
    assertThat(access(result, "user-detail").ruleOption().getName()).isEqualTo("tools");
    assertThat(access(result, "settings").layer()).isEqualTo(NONE);
  }

  @Test
  void theLastLayerWinsInTheOrderBaseGlobalProfileUser() {
    // Given out of order: the cascade applies them by layer
    List<Rule> rules = List.of(allow(USER, "users"), deny(PROFILE, "users"), allow(GLOBAL, "tools"),
      deny(BASE_PROFILE, "tools"));

    Result result = AccessCascade.evaluate(menu(), null, rules, USER);

    assertThat(access(result, "tools")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(true, GLOBAL);
    assertThat(access(result, "sites")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(true, GLOBAL);
    assertThat(access(result, "users")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(true, USER);
    assertThat(access(result, "user-detail"))
      .extracting(OptionAccess::allowed, OptionAccess::layer, OptionAccess::inherited).containsExactly(true, USER, true);
  }

  @Test
  void aRuleOnAChildOverridesTheInheritedRuleOfItsParent() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(deny(GLOBAL, "tools"), allow(PROFILE, "users")),
      PROFILE);

    assertThat(access(result, "sites").allowed()).isFalse();
    assertThat(access(result, "users")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(true, PROFILE);
    assertThat(access(result, "user-detail").allowed()).isTrue();
  }

  @Test
  void optionsOfAnotherModuleAreHiddenWithTheirDescendants() {
    Result result = AccessCascade.evaluate(menu(), "tools", List.of(), PROFILE);

    // The module of an option is compared ignoring case; an option without module is never hidden
    assertThat(access(result, "tools").allowed()).isTrue();
    assertThat(access(result, "settings").allowed()).isTrue();
    assertThat(access(result, "reports")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(false, MODULE);
    assertThat(access(result, "sales")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(false, MODULE);
    assertThat(access(result, "sales").moduleOption().getName()).isEqualTo("reports");
    assertThat(access(result, "sites").moduleOption()).isNull();
  }

  @Test
  void aLaterRuleShowsAnOptionHiddenByTheModule() {
    Result result = AccessCascade.evaluate(menu(), "Tools", List.of(allow(GLOBAL, "sales")), PROFILE);

    assertThat(access(result, "reports").allowed()).isFalse();
    assertThat(access(result, "sales")).extracting(OptionAccess::allowed, OptionAccess::layer).containsExactly(true, GLOBAL);
    assertThat(access(result, "sales").moduleOption().getName()).isEqualTo("reports");
  }

  @Test
  void theOwnRuleIsTheRuleOfTheSubjectLayerOnTheOption() {
    List<Rule> rules = List.of(deny(PROFILE, "sites"), allow(USER, "users"), deny(USER, "USERS"));

    Result asUser = AccessCascade.evaluate(menu(), null, rules, USER);
    Result asProfile = AccessCascade.evaluate(menu(), null, rules, PROFILE);

    // The last rule of the layer, matching the name ignoring case
    assertThat(access(asUser, "users").ownRule()).isEqualTo("R");
    assertThat(access(asUser, "sites").ownRule()).isEmpty();
    assertThat(access(asUser, "user-detail").ownRule()).isEmpty();
    assertThat(access(asProfile, "sites").ownRule()).isEqualTo("R");
    assertThat(access(asProfile, "users").ownRule()).isEmpty();
    assertThat(asUser.ruleOf(USER, "users")).isEqualTo("R");
    assertThat(asUser.ruleOf(PROFILE, "Sites")).isEqualTo("R");
    assertThat(asUser.ruleOf(GLOBAL, "sites")).isEmpty();
  }

  @Test
  void anAllowedOptionUnderADeniedParentIsWarned() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(deny(GLOBAL, "tools"), allow(PROFILE, "users")),
      PROFILE);

    assertThat(access(result, "users").warning()).isEqualTo(Warning.PARENT_DENIED);
    // Its child is allowed too, under a denied grandparent
    assertThat(access(result, "user-detail").warning()).isEqualTo(Warning.PARENT_DENIED);
    assertThat(access(result, "sites").warning()).isNull();
    assertThat(access(result, "tools").warning()).isNull();
  }

  @Test
  void anOwnRuleThatDoesNotChangeTheAccessIsRedundant() {
    List<Rule> rules = List.of(deny(GLOBAL, "settings"), allow(GLOBAL, "sites"), allow(PROFILE, "sites"),
      allow(PROFILE, "settings"), deny(PROFILE, "security"), allow(PROFILE, "reports"), deny(GLOBAL, "sales"),
      deny(PROFILE, "user-detail"), allow(PROFILE, "users"));

    Result result = AccessCascade.evaluate(menu(), null, rules, PROFILE);

    // Allowed anyway without it, by the global rule also in the modules that hide its parent
    assertThat(access(result, "sites").warning()).isEqualTo(Warning.REDUNDANT);
    // Overridden by a later own rule on its parent
    assertThat(access(result, "user-detail").warning()).isEqualTo(Warning.REDUNDANT);
    // Without the own allow on the parent, the global deny would hide it: the own deny of the child is needed too
    assertThat(access(result, "security").warning()).isNull();
    assertThat(access(result, "settings").warning()).isNull();
    // The parent is allowed anyway, but its own rule changes the access of a child
    assertThat(access(result, "reports").warning()).isNull();
  }

  @Test
  void onlyTheOwnRulesAreCheckedForRedundancy() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(allow(GLOBAL, "sites"), allow(PROFILE, "sites")),
      USER);

    assertThat(access(result, "sites").warning()).isNull();
  }

  @Test
  void theMenuIsNotModified() {
    Menu menu = menu();

    AccessCascade.evaluate(menu, "Reports", List.of(deny(GLOBAL, "settings")), PROFILE);

    assertThat(menu.getOptionByName("tools").isRestricted()).isFalse();
    assertThat(menu.getOptionByName("security").isRestricted()).isFalse();
  }

  @Test
  void findLooksUpAnOptionIgnoringCase() {
    Result result = AccessCascade.evaluate(menu(), null, List.of(), PROFILE);

    assertThat(result.find("User-Detail")).map(access -> access.option().getName()).hasValue("user-detail");
    assertThat(result.find("missing")).isEmpty();
  }

  /**
   * A change of the own rules of a subject is simulated as the restriction maintains store it: an existing rule changes
   * in its place, a new one goes last in its layer, and remove drops it; the other layers are untouched
   */
  @Test
  void aChangeOfTheOwnRulesIsSimulatedAsTheMaintainsStoreIt() {
    List<Rule> rules = List.of(deny(BASE_PROFILE, "settings"), deny(PROFILE, "Users"), allow(PROFILE, "tools"),
      allow(USER, "sites"), deny(GLOBAL, "users"));

    assertThat(AccessCascade.change(rules, PROFILE, List.of("users", "sites", "sites"), true))
      .containsExactly(deny(BASE_PROFILE, "settings"), deny(PROFILE, "Users"), allow(PROFILE, "tools"),
        allow(USER, "sites"), deny(GLOBAL, "users"), deny(PROFILE, "sites"));
    assertThat(AccessCascade.change(rules, PROFILE, List.of("users"), false))
      .containsExactly(deny(BASE_PROFILE, "settings"), allow(PROFILE, "Users"), allow(PROFILE, "tools"),
        allow(USER, "sites"), deny(GLOBAL, "users"));
    assertThat(AccessCascade.change(rules, USER, List.of("sites", "settings"), null))
      .containsExactly(deny(BASE_PROFILE, "settings"), deny(PROFILE, "Users"), allow(PROFILE, "tools"),
        deny(GLOBAL, "users"));
    // The new rule is the last one of its layer: it wins over the earlier deny of its parent
    assertThat(access(AccessCascade.evaluate(menu(), null, AccessCascade.change(List.of(deny(PROFILE, "sites"),
      deny(PROFILE, "tools")), PROFILE, List.of("sites"), false), PROFILE), "sites").allowed()).isFalse();
    assertThat(access(AccessCascade.evaluate(menu(), null, AccessCascade.change(List.of(deny(PROFILE, "tools")),
      PROFILE, List.of("sites"), false), PROFILE), "sites").allowed()).isTrue();
  }

  /**
   * The cascade gives the same restricted flag as the generator of the real menu for every option
   */
  @Test
  void sameRestrictionsAsTheRealMenu() {
    Map<String, List<Rule>> scenarios = new LinkedHashMap<>();
    scenarios.put("rule on a parent, opposite rule on a child", List.of(deny(GLOBAL, "tools"), allow(PROFILE, "users"),
      deny(USER, "user-detail")));
    scenarios.put("rule on a child, opposite rule on its parent later", List.of(deny(PROFILE, "user-detail"),
      allow(USER, "tools")));
    scenarios.put("user rule over a profile rule", List.of(deny(PROFILE, "settings"), allow(USER, "settings"),
      deny(PROFILE, "sales"), allow(USER, "reports")));
    scenarios.put("base profile rules", List.of(deny(BASE_PROFILE, "tools"), deny(BASE_PROFILE, "settings"),
      allow(GLOBAL, "security"), deny(PROFILE, "sites")));
    scenarios.put("same layer, child then parent", List.of(deny(GLOBAL, "sites"), allow(GLOBAL, "tools"),
      deny(GLOBAL, "reports"), allow(GLOBAL, "sales")));
    scenarios.put("names ignoring case", List.of(deny(GLOBAL, "TOOLS"), allow(USER, "Users")));

    for (String module : Arrays.asList(null, "Tools", "reports", "Other")) {
      scenarios.forEach((name, rules) -> {
        Map<String, Boolean> expected = realMenu(menu(), module, rules);
        Map<String, Boolean> actual = new LinkedHashMap<>();
        AccessCascade.evaluate(menu(), module, rules, USER).options()
          .forEach(access -> actual.put(access.option().getName(), !access.allowed()));

        assertThat(actual).as("%s with module %s", name, module).containsExactlyEntriesOf(expected);
      });
    }
  }

  /**
   * Restricted flag of every option in the real menu: module restriction, then the screen restrictions as the
   * getScreenRestrictions service lists them (base profile rules first, then the database rules)
   */
  private static Map<String, Boolean> realMenu(Menu menu, String module, List<Rule> rules) {
    List<ScreenRestriction> restrictions = new ArrayList<>();
    rules.stream().filter(rule -> rule.layer() == BASE_PROFILE).forEach(rule -> restrictions.add(restriction(rule)));
    rules.stream().filter(rule -> rule.layer() != BASE_PROFILE).forEach(rule -> restrictions.add(restriction(rule)));
    ScreenRestrictionGenerator generator = new ScreenRestrictionGenerator();
    generator.applyModuleRestriction(module, menu);
    generator.applyScreenRestriction(restrictions, menu);

    Map<String, Boolean> restricted = new LinkedHashMap<>();
    collect(menu.getElementList(), restricted);
    return restricted;
  }

  private static void collect(List<Option> options, Map<String, Boolean> restricted) {
    options.forEach(option -> {
      restricted.put(option.getName(), option.isRestricted());
      collect(option.getOptions(), restricted);
    });
  }

  private static ScreenRestriction restriction(Rule rule) {
    return new ScreenRestriction()
      .setOption(rule.option())
      .setRestricted(rule.restricted())
      .setProfile(rule.layer() == PROFILE ? "OPE" : null)
      .setUser(rule.layer() == USER ? "jmartin" : null);
  }

  private static OptionAccess access(Result result, String name) {
    return result.options().stream().filter(access -> access.option().getName().equals(name)).findFirst()
      .orElseThrow(() -> new AssertionError("No option " + name));
  }

  private static Rule allow(Layer layer, String option) {
    return new Rule(layer, option, false);
  }

  private static Rule deny(Layer layer, String option) {
    return new Rule(layer, option, true);
  }

  private static Menu menu(Option... options) {
    Menu menu = Menu.builder().elementList(new ArrayList<>(Arrays.<Element>asList(options))).build();
    menu.defineRelationship();
    return menu;
  }

  private static Option option(String name, String module, Option... children) {
    return Option.builder().name(name).module(module)
      .elementList(new ArrayList<>(Arrays.<Element>asList(children))).build();
  }
}
