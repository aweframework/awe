package com.almis.awe.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tolerant merge of a stored grid column layout with the columns of the XML definition
 */
@DisplayName("Grid column layout merge")
class GridColumnLayoutTest {

  private static Map<String, String> columns(String... entries) {
    Map<String, String> map = new LinkedHashMap<>();
    for (String entry : entries) {
      String[] parts = entry.split(":");
      map.put(parts[parts.length - 1], parts.length > 1 ? parts[0] : null);
    }
    return map;
  }

  @Test
  @DisplayName("Without a stored layout the XML order is kept and nothing is hidden")
  void withoutStoredLayout() {
    GridColumnLayout merged = GridColumnLayout.merge(null, columns("a", "b", "c"));

    assertEquals(List.of("a", "b", "c"), merged.getOrder());
    assertTrue(merged.getHidden().isEmpty());
  }

  @Test
  @DisplayName("Ungrouped columns follow the stored order")
  void storedOrderApplies() {
    GridColumnLayout stored = new GridColumnLayout(List.of("c", "a", "b"), List.of("b"));

    GridColumnLayout merged = GridColumnLayout.merge(stored, columns("a", "b", "c"));

    assertEquals(List.of("c", "a", "b"), merged.getOrder());
    assertEquals(List.of("b"), merged.getHidden());
  }

  @Test
  @DisplayName("Unknown column ids are ignored")
  void unknownIdsAreIgnored() {
    GridColumnLayout stored = new GridColumnLayout(List.of("zzz", "b", "a"), List.of("ghost", "a"));

    GridColumnLayout merged = GridColumnLayout.merge(stored, columns("a", "b"));

    assertEquals(List.of("b", "a"), merged.getOrder());
    assertEquals(List.of("a"), merged.getHidden());
  }

  @Test
  @DisplayName("A column added to the XML appears visible at its XML position")
  void newColumnKeepsItsXmlPosition() {
    GridColumnLayout stored = new GridColumnLayout(List.of("c", "a"), List.of());

    assertEquals(List.of("c", "a", "b"), GridColumnLayout.merge(stored, columns("a", "c", "b")).getOrder());
    assertEquals(List.of("n", "c", "a"), GridColumnLayout.merge(stored, columns("n", "a", "c")).getOrder());
    assertTrue(GridColumnLayout.merge(stored, columns("n", "a", "c")).getHidden().isEmpty());
  }

  @Test
  @DisplayName("A column never leaves its group, even when the stored layout says so")
  void columnsStayInTheirGroup() {
    Map<String, String> xml = columns("a", "g1:b", "g1:c", "d");
    GridColumnLayout stored = new GridColumnLayout(List.of("c", "a", "d", "b"), List.of());

    GridColumnLayout merged = GridColumnLayout.merge(stored, xml);

    assertEquals(List.of("a", "c", "b", "d"), merged.getOrder());
  }

  @Test
  @DisplayName("Columns hidden in the XML are not part of the layout")
  void xmlHiddenColumnsAreIgnored() {
    // The XML-hidden column is not offered to the merge, so a stored id for it is unknown
    GridColumnLayout stored = new GridColumnLayout(List.of("key", "b", "a"), List.of("key"));

    GridColumnLayout merged = GridColumnLayout.merge(stored, columns("a", "b"));

    assertEquals(List.of("b", "a"), merged.getOrder());
    assertTrue(merged.getHidden().isEmpty());
  }

  @Test
  @DisplayName("A layout that would hide every column hides none")
  void neverHidesEveryColumn() {
    GridColumnLayout stored = new GridColumnLayout(List.of("a", "b"), List.of("a", "b"));

    assertTrue(GridColumnLayout.merge(stored, columns("a", "b")).getHidden().isEmpty());
  }

  @Test
  @DisplayName("Repeated ids in the stored layout are counted once")
  void repeatedIdsAreCountedOnce() {
    GridColumnLayout stored = new GridColumnLayout(List.of("b", "b", "a"), List.of("a", "a"));

    GridColumnLayout merged = GridColumnLayout.merge(stored, columns("a", "b"));

    assertEquals(List.of("b", "a"), merged.getOrder());
    assertEquals(List.of("a"), merged.getHidden());
  }
}
