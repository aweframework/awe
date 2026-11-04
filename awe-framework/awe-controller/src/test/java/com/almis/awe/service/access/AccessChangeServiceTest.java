package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * The changes of the access window: the own rules of the subject and the rules of an option of any subject
 */
class AccessChangeServiceTest extends AccessServiceFixture {

  @Test
  void allowAndDenyStoreTheOwnRuleOfTheSubjectOnEveryOption() throws AWException {
    changeService.allowOptions("user", JMARTIN, OPE, List.of("users", "sites"));
    changeService.denyOptions("profile", JMARTIN, OPE, List.of("settings"));

    // A user rule has no profile, a profile rule has no user
    verify(menuRestrictionTreeService).allowMenuOption(JMARTIN, null, "users");
    verify(menuRestrictionTreeService).allowMenuOption(JMARTIN, null, "sites");
    verify(menuRestrictionTreeService).restrictMenuOption(null, OPE, "settings");
  }

  @Test
  void removeDeletesOnlyTheRulesThatExist() throws AWException {
    rules.add(new Object[]{null, OPE, "settings", "R"});

    changeService.removeOptionRules("profile", null, OPE, List.of("settings", "sites"));

    verify(menuRestrictionTreeService).removeRestriction(null, OPE, "settings");
    verify(menuRestrictionTreeService, never()).removeRestriction(null, OPE, "sites");
  }

  @Test
  void removeRulesDeletesEachRuleByItsIdentifier() throws AWException {
    // Rules of the why panel: of any profile, user or global, by identifier
    when(queryService.launchPrivateQuery(eq("access-rule"), any(ObjectNode.class))).thenAnswer(invocation ->
      switch (invocation.<ObjectNode>getArgument(1).get("rule").intValue()) {
        case 11 -> data(row("userId", null, "profileId", OPE, "option", "users", "rule", "R"));
        case 12 -> data(row("userId", JMARTIN, "profileId", null, "option", "users", "rule", "A"));
        case 13 -> data(row("userId", null, "profileId", null, "option", "sites", "rule", "R"));
        default -> data();
      });

    changeService.removeRules(List.of(11));
    changeService.removeRules(Arrays.asList(12, 13, null, 404));
    changeService.removeRules(null);

    // Only the row of the identifier, with its subject and option for the audit (another row of the same subject and
    // option stays)
    assertThat(maintained("delete-restriction-by-id")).containsExactly("11|null|3|users|R", "12|7|null|users|A",
      "13|null|null|sites|R");
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void switchRulesTurnsEachRuleIntoTheOtherOneByItsIdentifier() throws AWException {
    // Rules of the why panel: of any profile, user or global, by identifier
    when(queryService.launchPrivateQuery(eq("access-rule"), any(ObjectNode.class))).thenAnswer(invocation ->
      switch (invocation.<ObjectNode>getArgument(1).get("rule").intValue()) {
        case 11 -> data(row("userId", null, "profileId", OPE, "option", "users", "rule", "R"));
        case 12 -> data(row("userId", JMARTIN, "profileId", null, "option", "users", "rule", "A"));
        case 13 -> data(row("userId", null, "profileId", null, "option", "sites", "rule", "R"));
        default -> data();
      });

    changeService.switchRules(List.of(11));
    changeService.switchRules(Arrays.asList(12, 13, null, 404));
    changeService.switchRules(null);

    assertThat(maintained("update-restriction-by-id")).containsExactly("11|null|3|users|A", "12|7|null|users|R",
      "13|null|null|sites|A");
    verifyNoInteractions(menuRestrictionTreeService);
  }

  /**
   * Parameters of the calls to a maintain: id|user|profile|option|restriction
   */
  private List<String> maintained(String target) throws AWException {
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, atLeastOnce()).launchPrivateMaintain(eq(target), parameters.capture());
    verifyNoMoreInteractions(maintainService);
    return parameters.getAllValues().stream()
      .map(node -> String.join("|", Stream.of("id", "user", "profile", "option", "restriction")
        .map(name -> node.has(name) && !node.get(name).isNull() ? node.get(name).asString() : "null").toList()))
      .toList();
  }

  @Test
  void changesWithoutSubjectAreRefused() {
    assertThatThrownBy(() -> changeService.allowOptions("user", null, OPE, List.of("users")))
      .isInstanceOf(AWException.class)
      .hasMessage("ERROR_MESSAGE_ACCESS_NO_SUBJECT");
    assertThatThrownBy(() -> changeService.removeOptionRules("profile", null, null, List.of("users")))
      .isInstanceOf(AWException.class);
    verifyNoInteractions(menuRestrictionTreeService);
  }
}
