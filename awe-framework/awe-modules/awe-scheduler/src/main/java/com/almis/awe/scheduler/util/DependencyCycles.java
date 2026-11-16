package com.almis.awe.scheduler.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cycle detection for task dependencies (a task launches its children when it ends)
 */
public final class DependencyCycles {

  private DependencyCycles() {
  }

  /**
   * Check whether giving a task these children closes a cycle: a child is the task itself or reaches it
   *
   * @param taskId         Task being saved
   * @param children       Children it is saved with
   * @param childrenByTask Children of every other task
   * @return True when the task would depend on itself
   */
  public static boolean closesCycle(Integer taskId, Collection<Integer> children,
                                    Map<Integer, List<Integer>> childrenByTask) {
    Set<Integer> visited = new HashSet<>();
    Deque<Integer> pending = new ArrayDeque<>(children);
    while (!pending.isEmpty()) {
      Integer current = pending.pop();
      if (taskId.equals(current)) {
        return true;
      }
      if (visited.add(current)) {
        pending.addAll(childrenByTask.getOrDefault(current, List.of()));
      }
    }
    return false;
  }
}
