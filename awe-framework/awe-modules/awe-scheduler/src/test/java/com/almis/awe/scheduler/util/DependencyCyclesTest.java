package com.almis.awe.scheduler.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of the cycle detection for the dependencies a task is saved with.
 */
class DependencyCyclesTest {

  // 1 -> 2 -> 3 and 1 -> 4
  private static final Map<Integer, List<Integer>> CHILDREN = Map.of(1, List.of(2, 4), 2, List.of(3));

  @Test
  void taskDependingOnItselfClosesACycle() {
    assertTrue(DependencyCycles.closesCycle(5, List.of(5), CHILDREN));
  }

  @Test
  void childThatReachesTheTaskClosesACycle() {
    assertTrue(DependencyCycles.closesCycle(1, List.of(3), Map.of(3, List.of(2), 2, List.of(1))));
    assertTrue(DependencyCycles.closesCycle(3, List.of(1), CHILDREN));
  }

  @Test
  void childrenThatDoNotReachTheTaskDoNotCloseACycle() {
    assertFalse(DependencyCycles.closesCycle(2, List.of(3, 4), CHILDREN));
    assertFalse(DependencyCycles.closesCycle(9, List.of(1), CHILDREN));
    assertFalse(DependencyCycles.closesCycle(1, List.of(), CHILDREN));
  }

  @Test
  void existingCyclesElsewhereDoNotLoopForever() {
    assertFalse(DependencyCycles.closesCycle(9, List.of(7), Map.of(7, List.of(8), 8, List.of(7))));
  }
}
