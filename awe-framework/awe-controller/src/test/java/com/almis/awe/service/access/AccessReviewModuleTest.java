package com.almis.awe.service.access;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Rule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.almis.awe.service.access.AccessCascade.Layer.PROFILE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rule is redundant only when it changes nothing in every module the real menu can be evaluated in (no module and the
 * module of each option): a rule wins over the module restriction, so an allow can show an option of another module.
 */
class AccessReviewModuleTest {

  /**
   * tools (module Tools) > sites; reports (module Reports) > sales
   */
  private static Menu menu() {
    Menu menu = Menu.builder().elementList(new ArrayList<>(Arrays.<Element>asList(
      option("tools", "Tools", option("sites", null)),
      option("reports", "Reports", option("sales", null))))).build();
    menu.defineRelationship();
    return menu;
  }

  @Test
  void anAllowThatShowsAnOptionOfAnotherModuleIsNotRedundant() {
    List<Rule> rules = List.of(new Rule(PROFILE, "reports", false));

    // In module Tools the rule shows reports, which the module hides without it
    assertThat(AccessCascade.evaluate(menu(), "Tools", rules, PROFILE).find("reports"))
      .hasValueSatisfying(access -> assertThat(access.allowed()).isTrue());
    assertThat(AccessCascade.evaluate(menu(), "Tools", List.of(), PROFILE).find("reports"))
      .hasValueSatisfying(access -> assertThat(access.allowed()).isFalse());

    // The review evaluates without module: the rule changes the access in module Tools, so it is not a finding
    Result result = AccessCascade.evaluate(menu(), null, rules, PROFILE);
    assertThat(result.find("reports")).hasValueSatisfying(access -> assertThat(access.warning()).isNull());
    assertThat(AccessReview.review(result, PROFILE, "reports")).isEmpty();
  }

  private static Option option(String name, String module, Option... children) {
    return Option.builder().name(name).module(module)
      .elementList(new ArrayList<>(Arrays.<Element>asList(children))).build();
  }
}
