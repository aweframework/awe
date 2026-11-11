package com.almis.awe.model.entities.permissions;

import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.maintain.Maintain;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.entities.services.Services;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The permission catalog of AWE (awe/global/Permissions.xml) and the locale keys of its labels
 */
class AwePermissionCatalogDescriptorTest {

  private static final Path AWE_MODULE = Path.of("../awe-generic-screens/src/main/resources/application/awe")
    .normalize();

  private static PermissionCatalog aweCatalog() throws IOException {
    String xml = Files.readString(AWE_MODULE.resolve("global/Permissions.xml"));
    Permissions permissions = XmlSerializerTestUtil.buildSerializer().getObjectFromTemplate(Permissions.class, xml);
    return PermissionCatalog.build(List.of(permissions));
  }

  /**
   * AWE declares the users, profiles, permissions, administration areas, self and default resources
   */
  @Test
  void aweCatalogDeclaresItsResources() throws IOException {
    PermissionCatalog catalog = aweCatalog();

    assertThat(catalog.getPermissionIds()).containsExactly(
      "users.read", "users.create", "users.update", "users.delete", "users.unlock", "users.reset-2fa",
      "profiles.read", "profiles.create", "profiles.update", "profiles.delete",
      "permissions.read", "permissions.update",
      "system.read", "system.update",
      "access.read", "access.update",
      "audit.read",
      "themes.read", "themes.create", "themes.update", "themes.delete",
      "self.read", "self.update",
      "default.read", "default.write");
    assertThat(catalog.getGrantedAlways()).containsExactlyInAnyOrder("self.read", "self.update");
  }

  /**
   * The predefined profile files of AWE: the reader gets every read and the contributor everything except the users, the
   * profiles and the permissions. The administrator file grants nothing: applications reuse it as a screen restriction,
   * and the administrator profile gets everything from its own assignment
   */
  @Test
  void aweProfileFilesDeclareThePredefinedPermissions() throws IOException {
    PermissionCatalog catalog = aweCatalog();
    Map<String, List<String>> effective = new TreeMap<>();
    for (String file : List.of("administrator", "reader", "contributor", "general", "operator")) {
      Profile profile = XmlSerializerTestUtil.buildSerializer()
        .getObjectFromTemplate(Profile.class, Files.readString(AWE_MODULE.resolve("profile/" + file + ".xml")));
      Stream.concat(profile.getPermissionIds().stream(), profile.getExclusionIds().stream())
        .forEach(entry -> assertThat(catalog.validateAssignment(entry)).as(file + ": " + entry).isEmpty());
      effective.put(file, List.copyOf(catalog.expand(profile.getPermissionIds(), profile.getExclusionIds())));
    }

    assertThat(effective)
      .containsEntry("administrator", List.of())
      .containsEntry("reader", catalog.expand("*.read"))
      .containsEntry("contributor", catalog.getPermissionIds().stream()
        .filter(id -> !id.startsWith("users.") && !id.startsWith("profiles.") && !id.startsWith("permissions."))
        .toList())
      .containsEntry("general", List.of())
      .containsEntry("operator", List.of());
  }

  /**
   * Every label of the catalog is a locale key of every language of AWE
   */
  @Test
  void everyLabelIsALocaleKey() throws IOException {
    PermissionCatalog catalog = aweCatalog();
    List<String> labels = new ArrayList<>(catalog.getPermissionIds().stream().map(catalog::getLabel).toList());
    List.of("users", "profiles", "permissions", "system", "access", "audit", "themes", "self", "default")
      .forEach(resourceId -> labels.add(catalog.getResourceLabel(resourceId)));

    for (String language : List.of("en-GB", "es-ES", "fr-FR")) {
      String locales = Files.readString(AWE_MODULE.resolve("locale/Locale-" + language + ".xml"));
      assertThat(labels.stream().distinct().filter(label -> !locales.contains("name=\"" + label + "\"")).toList())
        .as("labels without a locale in %s", language).isEmpty();
    }
  }

  /**
   * The targets of the administration screens declare the permission of their area, and every permission declared by
   * an AWE query or maintain target is valid against the AWE catalog
   */
  @Test
  void aweTargetsDeclareValidPermissions() throws IOException {
    PermissionCatalog catalog = aweCatalog();
    Map<String, String> queries = new TreeMap<>();
    XmlSerializerTestUtil.buildSerializer()
      .getObjectFromTemplate(Queries.class, Files.readString(AWE_MODULE.resolve("global/Queries.xml")))
      .getBaseElementList().stream().filter(query -> query.getPermission() != null)
      .forEach(query -> {
        assertThat(catalog.validatePermissionAttribute(query.getPermission(), query.isPublic())).as(query.getId())
          .isEmpty();
        queries.put(query.getId(), query.getPermission());
      });
    Map<String, String> targets = new TreeMap<>();
    XmlSerializerTestUtil.buildSerializer()
      .getObjectFromTemplate(Maintain.class, Files.readString(AWE_MODULE.resolve("global/Maintain.xml")))
      .getBaseElementList().stream().filter(target -> target.getPermission() != null)
      .forEach(target -> {
        assertThat(catalog.validatePermissionAttribute(target.getPermission(), target.isPublic())).as(target.getName())
          .isEmpty();
        targets.put(target.getName(), target.getPermission());
      });

    assertThat(queries).containsEntry("UsrLst", "users.read").containsEntry("UsrChkUni", "users.read")
      .containsEntry("ProLst", "profiles.read").containsEntry("SitLst", "system.read")
      .containsEntry("access-tree", "access.read")
      .containsAllEntriesOf(Map.of("SecAudEvtLst", "audit.read", "SecAudResCnt", "audit.read",
        "SecAudResCntRes", "audit.read", "SecAudTotEvt", "audit.read", "SecAudTotKo", "audit.read",
        "SecAudTotUsr", "audit.read", "SecAudTotLck", "audit.read"))
      .containsEntry("ThmLst", "themes.read").containsEntry("connectedUser", "self.read")
      // The helper sources of the audit charts and of the quick filter counts are only read by AWE targets
      .doesNotContainKeys("SecAudActSrc", "SecAudUsrSrc");
    // The screen personalization editor (#792) is for who can read the access: the runtime query stays public
    assertThat(List.of("getScreenStructure", "getScreenElementAttributes", "getScreenOption", "getScreenImportUsers",
      "getScreenConfigurationOverview", "getScreenConfigurationHistory", "getScreenConfigurationHistoryRows",
      "getScreenConfigurationExport")).allSatisfy(id -> assertThat(queries).containsEntry(id, "access.read"));
    assertThat(queries).doesNotContainKey("getScreenConfiguration");
    assertThat(targets).containsAllEntriesOf(Map.of(
      "UsrNew", "users.create", "UsrUpd", "users.update", "UsrDel", "users.delete", "UsrUnl", "users.unlock",
      "reset2fa", "users.reset-2fa", "ProNew", "profiles.create", "ProUpd", "profiles.update",
      "ProDel", "profiles.delete"))
      .containsAllEntriesOf(Map.of(
        "SitNew", "system.update", "access-allow", "access.update", "ThmNew", "themes.create",
        "ThmDel", "themes.delete", "saveGridLayout", "self.update"));
  }

  /**
   * The AWE services that launch the query or the maintain target a client asks for are authorized by that target,
   * the services that the administration screens launch declare the permission of their area, and no AWE service
   * declares a permission the AWE catalog does not have
   */
  @Test
  void aweDispatcherServicesAreAuthorizedByTheirTarget() throws IOException {
    PermissionCatalog catalog = aweCatalog();
    List<Service> services = XmlSerializerTestUtil.buildSerializer()
      .getObjectFromTemplate(Services.class, Files.readString(AWE_MODULE.resolve("global/Services.xml")))
      .getBaseElementList();

    assertThat(services.stream().filter(Service::isAuthorizedByTarget).map(Service::getId))
      .containsExactlyInAnyOrder("launchMaintain", "launchQuery", "launchColumnValues", "updateModel", "checkUnique",
        "checkEmpty", "subscribe");
    services.forEach(service -> assertThat(catalog.validatePermissionAttribute(service.getPermission(), false))
      .as(service.getId()).isEmpty());
    assertThat(services.stream().filter(service -> service.getPermission() != null)
      .collect(Collectors.toMap(Service::getId, Service::getPermission)))
      .containsEntry("ChgLogLvl", "system.update").containsEntry("EncTxt", "system.read")
      .containsEntry("refreshMenu", "self.read");
  }
}
