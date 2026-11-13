package com.almis.awe.test.integration.security;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.Frame;
import com.almis.awe.model.entities.screen.component.action.AbstractAction;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.security.authorization.ScreenTargetIndex;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the screens of the public menu: they are open to users without permissions (login, change password), so a
 * query, maintain target or service they launch never declares a permission. Otherwise the screen breaks for the users
 * without it, or the screen access lets any signed-in user launch an administration target through it.
 */
@Tag("integration")
@DisplayName("Public screen targets tests")
class PublicScreenTargetsTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private AweElements elements;

  @Autowired
  private ScreenTargetIndex screenTargetIndex;

  @Autowired
  private BaseConfigProperties baseConfigProperties;

  @Test
  @DisplayName("A screen of an option of the public menu never launches a target that declares a permission")
  void publicScreens_neverLaunchTargetsWithPermission() throws Exception {
    Set<String> publicOptions = elements.getMenu(baseConfigProperties.getFiles().getMenuPublic())
      .getElementsByType(Option.class).stream()
      .map(Option::getName)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    Map<String, String> permissions = new TreeMap<>();
    elements.getQueryMap().forEach((id, query) -> permissions.put(id, query.getPermission()));
    elements.getMaintainMap().forEach((id, target) -> permissions.put(id, target.getPermission()));

    Map<String, Set<String>> launchedFromPublicScreens = new TreeMap<>();
    permissions.forEach((target, permission) -> {
      if (permission != null && !permission.isBlank()) {
        Set<String> options = new TreeSet<>(screenTargetIndex.getOptionsUsing(target));
        options.retainAll(publicOptions);
        if (!options.isEmpty()) {
          launchedFromPublicScreens.put(target, options);
        }
      }
    });

    assertThat(launchedFromPublicScreens)
      .as("Targets with a permission launched from public menu options (target -> options)")
      .isEmpty();
  }

  @Test
  @DisplayName("A screen of an option of the public menu never launches a service that declares a permission")
  void publicScreens_neverLaunchServicesWithPermission() throws Exception {
    Menu publicMenu = elements.getMenu(baseConfigProperties.getFiles().getMenuPublic());
    Map<String, Set<String>> serverActions = new TreeMap<>();
    collectServerActions(publicMenu.getScreen(), "public menu", serverActions);
    for (Option option : publicMenu.getElementsByType(Option.class)) {
      collectServerActions(option.getScreen(), option.getName(), serverActions);
    }

    Set<String> launchedServices = new TreeSet<>();
    Map<String, Set<String>> servicesWithPermission = new TreeMap<>();
    serverActions.forEach((actionId, options) -> {
      Service service = getActionService(actionId);
      if (service != null) {
        launchedServices.add(service.getId());
        if (service.getPermission() != null && !service.getPermission().isBlank()) {
          servicesWithPermission.computeIfAbsent(service.getId(), id -> new TreeSet<>()).addAll(options);
        }
      }
    });

    assertThat(launchedServices).as("Services launched from public menu options").isNotEmpty();
    assertThat(servicesWithPermission)
      .as("Services with a permission launched from public menu options (service -> options)")
      .isEmpty();
  }

  private Service getActionService(String actionId) {
    try {
      Action action = elements.getAction(actionId);
      if (action == null || action.getCall() == null || action.getCall().getService() == null) {
        return null;
      }
      return elements.getService(action.getCall().getService());
    } catch (AWException exc) {
      return null;
    }
  }

  /**
   * Collect the server actions of a screen, with its includes resolved and its frames followed
   */
  private void collectServerActions(String screenId, String option, Map<String, Set<String>> serverActions)
    throws AWException {
    Set<String> visited = new HashSet<>();
    Set<String> pending = new TreeSet<>();
    if (screenId != null) {
      pending.add(screenId);
    }
    while (!pending.isEmpty()) {
      String current = pending.iterator().next();
      pending.remove(current);
      if (!visited.add(current)) {
        continue;
      }
      Screen screen = elements.getScreen(current);
      for (Element element : screen.getElementsByType(Element.class)) {
        String serverAction = null;
        if (element instanceof Component component) {
          serverAction = component.getServerAction();
          if (element instanceof Frame frame && frame.getScreen() != null) {
            pending.add(frame.getScreen());
          }
        } else if (element instanceof AbstractAction action) {
          serverAction = action.getServerAction();
        } else if (element instanceof Dependency dependency) {
          serverAction = dependency.getServerAction();
        }
        if (serverAction != null && !serverAction.isBlank()) {
          serverActions.computeIfAbsent(serverAction, id -> new TreeSet<>()).add(option);
        }
      }
    }
  }
}
