package com.almis.awe.test.integration.security;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.security.authorization.ScreenTargetIndex;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The administration areas of AWE and of its modules declare one permission resource each, and the targets that only
 * the screens of one area launch declare a permission of that resource. Targets shared by the screens of several areas
 * (suggests and selectors of users, profiles, themes...) are not checked here.
 */
@Tag("integration")
@DisplayName("Administration target permissions tests")
class AdministrationTargetPermissionsTest extends AbstractSpringAppIntegrationTest {

  private static final String OTHER = "other";

  // Menu options of the test application -> resource of their administration area
  private static final Map<String, String> AREAS = new HashMap<>();

  static {
    area("users", "users");
    area("profiles", "profiles");
    area("system", "sites", "modules", "databases", "email-servers", "queues", "sequences", "application-parameters",
      "broadcast-messages", "log", "view-log", "encrypt-tools");
    area("access", "screen-access", "screen-configuration");
    area("audit", "security-audit", "view-security-audit-event");
    area("themes", "themes");
    area("scheduler", "scheduler-management", "scheduler-tasks", "new-scheduler-task", "update-scheduler-task",
      "scheduler-servers", "new-scheduler-server", "update-scheduler-server", "scheduler-calendars",
      "new-scheduler-calendar", "update-scheduler-calendar");
    area("subscriptions", "subscriptions", "new-subscription", "update-subscription");
    area("developer", "path-manager", "local-manager");
    area("sql-extractor", "sqlExtractor");
    area("self", "user-settings", "notifications");
  }

  // AWE targets launched only by module screens: the AWE catalog cannot name a resource of a module
  private static final Set<String> AWE_TARGETS_OF_MODULE_SCREENS = Set.of("GetEmaFrmArr", "GetUsrFrmArr");

  // Checks of the value of a selector shared by several areas (ProSug, the suggest of profiles)
  private static final Set<String> SHARED_SELECTOR_CHECKS = Set.of("ProIni");

  // Other resources an area administers: the permissions of the profiles are edited in the profiles screen, and the
  // Permissions view of the access window shows the permissions of a user or a profile (users.read permissions.read)
  private static final Map<String, Set<String>> OTHER_RESOURCES = Map.of("profiles", Set.of("permissions"),
    "access", Set.of("users", "permissions"));

  @Autowired
  private AweElements elements;

  @Autowired
  private ScreenTargetIndex screenTargetIndex;

  private static void area(String resource, String... options) {
    Arrays.stream(options).forEach(option -> AREAS.put(option, resource));
  }

  @Test
  @DisplayName("Every target launched only by the screens of one administration area declares its resource")
  void administrationTargetsDeclareTheResourceOfTheirArea() {
    Map<String, String> permissions = new TreeMap<>();
    elements.getQueryMap().forEach((id, query) -> permissions.put(id, query.getPermission()));
    elements.getMaintainMap().forEach((id, target) -> permissions.put(id, target.getPermission()));

    Map<String, String> wrong = new TreeMap<>();
    permissions.forEach((target, permission) -> {
      Set<String> areas = screenTargetIndex.getOptionsUsing(target).stream()
        .map(option -> AREAS.getOrDefault(option, OTHER))
        .collect(Collectors.toCollection(TreeSet::new));
      if (areas.size() != 1 || areas.contains(OTHER) || AWE_TARGETS_OF_MODULE_SCREENS.contains(target)
        || SHARED_SELECTOR_CHECKS.contains(target)) {
        return;
      }
      String resource = areas.iterator().next();
      Set<String> resources = new TreeSet<>(OTHER_RESOURCES.getOrDefault(resource, Set.of()));
      resources.add(resource);
      if (permission == null || !Arrays.stream(permission.trim().split("\\s+"))
        .allMatch(id -> resources.stream().anyMatch(area -> id.startsWith(area + PermissionCatalog.SEPARATOR)))) {
        wrong.put(target, resource + " <- " + permission);
      }
    });

    assertThat(wrong).as("Targets of one administration area without a permission of its resource "
      + "(target -> resource <- permission)").isEmpty();
  }

  @Test
  @DisplayName("Every resource and permission of the combined catalog has a label in every language")
  void everyCatalogLabelIsALocale() {
    PermissionCatalog catalog = elements.getPermissionCatalog();
    List<String> labels = new ArrayList<>(catalog.getPermissionIds().stream().map(catalog::getLabel).toList());
    catalog.getPermissionIds().stream()
      .map(id -> id.substring(0, id.indexOf(PermissionCatalog.SEPARATOR)))
      .distinct()
      .forEach(resource -> labels.add(catalog.getResourceLabel(resource)));

    assertThat(catalog.getPermissionIds()).contains("system.update", "access.update", "audit.read", "themes.delete",
      "scheduler.execute", "subscriptions.delete", "developer.update", "sql-extractor.read");
    elements.getLocales().forEach((language, locales) ->
      assertThat(labels.stream().distinct().filter(label -> !locales.containsKey(label)).toList())
        .as("labels without a locale in %s", language).isEmpty());
  }
}
