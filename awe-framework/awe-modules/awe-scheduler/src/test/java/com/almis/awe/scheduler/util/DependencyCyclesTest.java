package com.almis.awe.scheduler.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @Test
  void aParentThatIsTheTaskOrAlreadyRunsAfterItClosesACycle() {
    assertTrue(DependencyCycles.closesCycleFromParents(1, List.of(3), CHILDREN));
    assertTrue(DependencyCycles.closesCycleFromParents(1, List.of(9, 4), CHILDREN));
    assertTrue(DependencyCycles.closesCycleFromParents(2, List.of(2), CHILDREN));
  }

  @Test
  void parentsThatDoNotRunAfterTheTaskDoNotCloseACycle() {
    assertFalse(DependencyCycles.closesCycleFromParents(2, List.of(1, 4), CHILDREN));
    assertFalse(DependencyCycles.closesCycleFromParents(3, List.of(), CHILDREN));
    // A new task has no identifier and nothing runs after it yet
    assertFalse(DependencyCycles.closesCycleFromParents(null, List.of(1, 2), CHILDREN));
  }

  @Test
  void theDescendantsOfATaskAreTheTasksThatRunAfterItDirectlyOrThroughOthers() {
    assertEquals(Set.of(2, 3, 4), DependencyCycles.descendants(1, CHILDREN));
    assertEquals(Set.of(), DependencyCycles.descendants(3, CHILDREN));
    assertEquals(Set.of(7, 8), DependencyCycles.descendants(7, Map.of(7, List.of(8), 8, List.of(7))));
  }
}
