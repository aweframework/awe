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

  /**
   * Check whether running a task after these parents closes a cycle: a parent is the task itself or already runs
   * after it, directly or through other tasks. A new task (without identifier) has nothing running after it yet
   *
   * @param taskId         Task being saved, null for a new task
   * @param parents        Parents it is saved with
   * @param childrenByTask Children of every task
   * @return True when the task would depend on itself
   */
  public static boolean closesCycleFromParents(Integer taskId, Collection<Integer> parents,
                                               Map<Integer, List<Integer>> childrenByTask) {
    if (taskId == null) {
      return false;
    }
    Set<Integer> descendants = descendants(taskId, childrenByTask);
    return parents.stream().anyMatch(parent -> taskId.equals(parent) || descendants.contains(parent));
  }

  /**
   * Tasks that run after a task, directly or through other tasks
   *
   * @param taskId         Task
   * @param childrenByTask Children of every task
   * @return Descendants of the task (the task itself only when a cycle already reaches it)
   */
  public static Set<Integer> descendants(Integer taskId, Map<Integer, List<Integer>> childrenByTask) {
    Set<Integer> visited = new HashSet<>();
    Deque<Integer> pending = new ArrayDeque<>(childrenByTask.getOrDefault(taskId, List.of()));
    while (!pending.isEmpty()) {
      Integer current = pending.pop();
      if (visited.add(current)) {
        pending.addAll(childrenByTask.getOrDefault(current, List.of()));
      }
    }
    return visited;
  }
}
