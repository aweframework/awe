package com.almis.awe.security.authorization;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.Frame;
import com.almis.awe.model.entities.screen.component.action.AbstractAction;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Index of the targets (maintains and queries) used by the screens of the menu options: for each target identifier,
 * the names of the options whose screen uses it.
 *
 * <p>It is built from the screens of the options of the private and public menus on first use, and built again when
 * the screens, menus, queries or maintains are loaded again. A screen is read with its includes resolved and its
 * frames followed. The options with a dynamic screen (generated each time it is launched) cannot be enumerated and are
 * skipped, and a screen that cannot be read is logged and skipped. The targets of the screen of the menu (the
 * application shell, shown whatever the option) are not indexed.</p>
 *
 * <p>The target identifiers are collected from the {@code target-action} of components, button actions and
 * dependencies, and from the {@code check-target} of criteria. Some actions (such as {@code server-action="screen"})
 * hold an option name in {@code target-action}: it is collected too, and it only matters if a maintain or a query is
 * launched with that same identifier.</p>
 */
@Slf4j
public class ScreenTargetIndex extends ServiceConfig {

  private final BaseConfigProperties baseConfigProperties;
  private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();

  /**
   * Autowired constructor
   *
   * @param baseConfigProperties Base configuration properties
   */
  public ScreenTargetIndex(BaseConfigProperties baseConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve the names of the options whose screen uses a target
   *
   * @param targetId Maintain or query identifier
   * @return Option names (empty when no screen uses the target)
   */
  public Set<String> getOptionsUsing(String targetId) {
    if (targetId == null) {
      return Collections.emptySet();
    }
    return getSnapshot().targets().getOrDefault(targetId, Collections.emptySet());
  }

  /**
   * Retrieve the index, building it again if the definitions have been loaded again since it was built
   */
  private Snapshot getSnapshot() {
    AweElements elements = getElements();
    long version = elements.getReloadVersion();
    Snapshot current = snapshot.get();
    if (current == null || current.version() != version) {
      synchronized (this) {
        current = snapshot.get();
        if (current == null || current.version() != version) {
          current = new Snapshot(version, build(elements));
          snapshot.set(current);
        }
      }
    }
    return current;
  }

  /**
   * Build the index from the options of the private and public menus
   */
  private Map<String, Set<String>> build(AweElements elements) {
    Map<String, Set<String>> targets = new HashMap<>();
    Set<String> shellTargets = new HashSet<>();
    for (String menuId : new String[]{baseConfigProperties.getFiles().getMenuPrivate(),
      baseConfigProperties.getFiles().getMenuPublic()}) {
      Menu menu = readMenu(elements, menuId);
      if (menu != null) {
        for (Option option : menu.getElementsByType(Option.class)) {
          indexOption(elements, option, targets);
        }
        collectScreenTargets(elements, menu.getScreen(), new HashSet<>(), shellTargets);
      }
    }
    // The screen of the menu (the application shell) is shown whatever the option: its targets are never restricted
    shellTargets.forEach(targets::remove);
    return targets;
  }

  private Menu readMenu(AweElements elements, String menuId) {
    try {
      return elements.getMenu(menuId);
    } catch (Exception exc) {
      log.error("Menu '{}' could not be read to index the screen targets", menuId, exc);
      return null;
    }
  }

  private void indexOption(AweElements elements, Option option, Map<String, Set<String>> targets) {
    if (option.getScreen() == null || option.getScreen().isBlank() || option.getName() == null
      || option.isDynamic() || option.getDynamicScreenService() != null) {
      return;
    }
    Set<String> optionTargets = new HashSet<>();
    collectScreenTargets(elements, option.getScreen(), new HashSet<>(), optionTargets);
    optionTargets.forEach(target -> targets.computeIfAbsent(target, key -> new TreeSet<>()).add(option.getName()));
  }

  private void collectScreenTargets(AweElements elements, String screenId, Set<String> visited, Set<String> targets) {
    if (screenId == null || screenId.isBlank() || !visited.add(screenId)) {
      return;
    }
    Screen screen;
    try {
      screen = elements.getScreen(screenId);
    } catch (Exception exc) {
      log.warn("Screen '{}' could not be read to index its targets: {}", screenId, exc.getMessage());
      return;
    }
    if (screen == null) {
      return;
    }
    for (Element element : screen.getElementsByType(Element.class)) {
      if (element instanceof Component component) {
        add(targets, component.getTargetAction());
        if (element instanceof Frame frame) {
          collectScreenTargets(elements, frame.getScreen(), visited, targets);
        }
        if (element instanceof AbstractCriteria criteria) {
          add(targets, criteria.getCheckTarget());
        }
      } else if (element instanceof AbstractAction action) {
        add(targets, action.getTargetAction());
      } else if (element instanceof Dependency dependency) {
        add(targets, dependency.getTargetAction());
      }
    }
  }

  private void add(Set<String> targets, String target) {
    if (target != null && !target.isBlank()) {
      targets.add(target);
    }
  }

  /**
   * Index built for a version of the loaded definitions
   */
  private record Snapshot(long version, Map<String, Set<String>> targets) {
  }
}
