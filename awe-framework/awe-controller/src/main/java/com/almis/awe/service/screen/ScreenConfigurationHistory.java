package com.almis.awe.service.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the audit rows of the screen personalization rules as a history: what each change did, the value before and after,
 * and which changes can still be undone. Pure rules, no Spring: the audit rows are given by the caller.
 */
public final class ScreenConfigurationHistory {

  /**
   * An audit row. The action is the audit one: I (insert), U (update) or D (delete)
   *
   * @param date        Date and time of the change
   * @param operator    User who made the change
   * @param action      I, U or D
   * @param id          Rule id
   * @param component   Component id
   * @param attribute   Attribute name
   * @param screen      Screen id
   * @param userId      User id, empty for everyone or a profile
   * @param userName    User name
   * @param profileId   Profile id, empty for everyone or a user
   * @param profileName Profile name
   * @param value       Value written by the change
   */
  public record Entry(String date, String operator, String action, int id, String component, String attribute, String screen,
                      String userId, String userName, String profileId, String profileName, String value) {
  }

  /**
   * A change with what it did
   *
   * @param entry       Audit row
   * @param action      created, changed or deleted
   * @param valueBefore Value of the rule before the change, empty when it was created
   * @param valueAfter  Value of the rule after the change, empty when it was deleted
   * @param undoable    Whether nothing changed the rule afterwards
   */
  public record Row(Entry entry, String action, String valueBefore, String valueAfter, boolean undoable) {
  }

  private ScreenConfigurationHistory() {
  }

  /**
   * Describe the changes. A change is undoable when it is the last one of its rule; a deletion is also undoable only while
   * no later change touched the same element, option and scope (the undo creates a new rule, so it would be done twice)
   *
   * @param entries Audit rows, oldest first
   * @return One row per audit row, newest first
   */
  public static List<Row> build(List<Entry> entries) {
    Map<Integer, Integer> lastOfRule = new HashMap<>();
    Map<String, Integer> lastOfScope = new HashMap<>();
    Map<Integer, String> previousValue = new HashMap<>();
    for (int i = 0; i < entries.size(); i++) {
      lastOfRule.put(entries.get(i).id(), i);
      lastOfScope.put(scope(entries.get(i)), i);
    }

    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      Entry entry = entries.get(i);
      String before = previousValue.get(entry.id());
      previousValue.put(entry.id(), entry.value());
      boolean undoable = isUndoable(entry, before, i, lastOfRule, lastOfScope);
      rows.add(new Row(entry, actionName(entry), valueBefore(entry, before), "D".equals(entry.action()) ? "" : entry.value(), undoable));
    }
    Collections.reverse(rows);
    return rows;
  }

  private static boolean isUndoable(Entry entry, String before, int index, Map<Integer, Integer> lastOfRule,
                                    Map<String, Integer> lastOfScope) {
    boolean deleted = "D".equals(entry.action());
    boolean created = "I".equals(entry.action());
    // A change of a rule that was not audited before has no value to go back to
    boolean lastChange = lastOfRule.get(entry.id()) == index && (!deleted || lastOfScope.get(scope(entry)) == index);
    return lastChange && (created || deleted || before != null);
  }

  private static String actionName(Entry entry) {
    return "I".equals(entry.action()) ? "created" : "D".equals(entry.action()) ? "deleted" : "changed";
  }

  private static String valueBefore(Entry entry, String before) {
    if ("I".equals(entry.action())) {
      return "";
    }
    return before != null ? before : "D".equals(entry.action()) ? entry.value() : "";
  }

  private static String scope(Entry entry) {
    return String.join("|", entry.screen(), entry.component(), entry.attribute(), entry.userId(), entry.profileId());
  }
}
