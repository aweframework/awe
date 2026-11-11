package com.almis.awe.service.screen;

import com.almis.awe.service.screen.ScreenConfigurationHistory.Entry;
import com.almis.awe.service.screen.ScreenConfigurationHistory.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenConfigurationHistoryTest {

  private static Entry entry(String action, int id, String component, String attribute, String value) {
    return new Entry("01/01/2026 10:00:00", "ana", action, id, component, attribute, "Scr", "", "", "", "", value);
  }

  private static Entry profileEntry(String action, int id, String profileId, String value) {
    return new Entry("01/01/2026 10:00:00", "ana", action, id, "Btn", "label", "Scr", "", "", profileId, "Admin", value);
  }

  @Test
  void theRowsComeNewestFirst() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(entry("I", 1, "Btn", "label", "a"), entry("I", 2, "Btn", "visible", "b")));

    assertThat(rows).extracting(row -> row.entry().id()).containsExactly(2, 1);
  }

  @Test
  void aCreatedRuleHasNoValueBefore() {
    Row row = ScreenConfigurationHistory.build(List.of(entry("I", 1, "Btn", "label", "a"))).get(0);

    assertThat(row.action()).isEqualTo("created");
    assertThat(row.valueBefore()).isEmpty();
    assertThat(row.valueAfter()).isEqualTo("a");
  }

  @Test
  void aChangedRuleHasTheValueOfItsPreviousRowBefore() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(entry("I", 1, "Btn", "label", "a"), entry("U", 1, "Btn", "label", "b"),
      entry("U", 1, "Btn", "label", "c")));

    assertThat(rows).extracting(Row::action).containsExactly("changed", "changed", "created");
    assertThat(rows).extracting(Row::valueBefore).containsExactly("b", "a", "");
    assertThat(rows).extracting(Row::valueAfter).containsExactly("c", "b", "a");
  }

  @Test
  void aDeletedRuleShowsTheValueItHad() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(entry("I", 1, "Btn", "label", "a"), entry("U", 1, "Btn", "label", "b"),
      entry("D", 1, "Btn", "label", "b")));

    assertThat(rows.get(0).action()).isEqualTo("deleted");
    assertThat(rows.get(0).valueBefore()).isEqualTo("b");
    assertThat(rows.get(0).valueAfter()).isEmpty();
  }

  @Test
  void aDeletedRuleWithoutPreviousRowsKeepsTheValueSentWithTheDelete() {
    assertThat(ScreenConfigurationHistory.build(List.of(entry("D", 5, "Btn", "label", "z"))).get(0).valueBefore()).isEqualTo("z");
  }

  @Test
  void onlyTheLatestRowOfEachRuleCanBeUndone() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(entry("I", 1, "Btn", "label", "a"), entry("U", 1, "Btn", "label", "b"),
      entry("I", 2, "Btn", "visible", "c")));

    assertThat(rows).extracting(Row::undoable).containsExactly(true, true, false);
  }

  @Test
  void aDeletedRuleCannotBeUndoneOnceTheSameRuleWasCreatedAgain() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(profileEntry("I", 1, "7", "a"), profileEntry("D", 1, "7", "a")));
    assertThat(rows.get(0).undoable()).isTrue();

    rows = ScreenConfigurationHistory.build(List.of(profileEntry("I", 1, "7", "a"), profileEntry("D", 1, "7", "a"), profileEntry("I", 2, "7", "a")));
    assertThat(rows).extracting(Row::undoable).containsExactly(true, false, false);
  }

  @Test
  void aRuleOfAnotherScopeDoesNotBlockTheUndo() {
    List<Row> rows = ScreenConfigurationHistory.build(List.of(profileEntry("D", 1, "7", "a"), profileEntry("I", 2, "8", "a")));

    assertThat(rows).extracting(Row::undoable).containsExactly(true, true);
  }

  @Test
  void aChangeOfARuleNotAuditedBeforeHasNothingToGoBackTo() {
    Row row = ScreenConfigurationHistory.build(List.of(entry("U", 1, "Btn", "label", "b"))).get(0);

    assertThat(row.valueBefore()).isEmpty();
    assertThat(row.undoable()).isFalse();
  }
}
