package com.almis.awe.model.entities.permissions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Permission catalog built from the Permissions.xml files of the modules
 */
class PermissionCatalogTest {

  private static final String AWE_CATALOG = """
    <permissions>
      <verb id="read" label="PERMISSION_VERB_READ" />
      <verb id="create" label="PERMISSION_VERB_CREATE" />
      <verb id="update" label="PERMISSION_VERB_UPDATE" />
      <verb id="delete" label="PERMISSION_VERB_DELETE" />
      <verb id="export" label="PERMISSION_VERB_EXPORT" />
      <resource id="users" label="PERMISSION_RESOURCE_USERS" verbs="crud">
        <action id="unlock" label="PERMISSION_USERS_UNLOCK" />
        <action id="reset-2fa" label="PERMISSION_USERS_RESET_2FA" />
      </resource>
      <resource id="profiles" label="PERMISSION_RESOURCE_PROFILES" verbs="crud" />
      <resource id="permissions" label="PERMISSION_RESOURCE_PERMISSIONS" verbs="read,update" />
      <resource id="self" label="PERMISSION_RESOURCE_SELF" verbs="read,update" granted="always" />
      <resource id="default" label="PERMISSION_RESOURCE_DEFAULT" verbs="read,write" />
    </permissions>
    """;

  private final XStreamSerializer serializer = XmlSerializerTestUtil.buildSerializer();
  private final Logger logger = (Logger) LoggerFactory.getLogger(PermissionCatalog.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  private Permissions parse(String xml) {
    return serializer.getObjectFromTemplate(Permissions.class, xml);
  }

  private PermissionCatalog awe() {
    return PermissionCatalog.build(List.of(parse(AWE_CATALOG)));
  }

  private List<String> warnings() {
    return appender.list.stream().filter(event -> event.getLevel() == Level.WARN)
      .map(ILoggingEvent::getFormattedMessage).toList();
  }

  /**
   * A permission is resource.verb for each listed verb ('crud' is read, create, update and delete) and
   * resource.action for each action, labelled with the label of the verb or the action
   */
  @Test
  void buildsResourceVerbAndResourceActionIdentifiers() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.getPermissionIds()).containsExactly(
      "users.read", "users.create", "users.update", "users.delete", "users.unlock", "users.reset-2fa",
      "profiles.read", "profiles.create", "profiles.update", "profiles.delete",
      "permissions.read", "permissions.update",
      "self.read", "self.update",
      "default.read", "default.write");
    assertThat(catalog.getLabel("users.delete")).isEqualTo("PERMISSION_VERB_DELETE");
    assertThat(catalog.getLabel("users.reset-2fa")).isEqualTo("PERMISSION_USERS_RESET_2FA");
    assertThat(catalog.getLabel("default.write")).isEqualTo("PERMISSION_VERB_WRITE");
    assertThat(catalog.getLabel("users.approve")).isNull();
    assertThat(catalog.getResourceLabel("users")).isEqualTo("PERMISSION_RESOURCE_USERS");
    assertThat(catalog.contains("profiles.update")).isTrue();
    assertThat(catalog.contains("profiles.export")).isFalse();
  }

  /**
   * The verbs of a resource are told apart from its own actions
   */
  @Test
  void actionsAreToldApartFromVerbs() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.isAction("users.unlock")).isTrue();
    assertThat(catalog.isAction("users.reset-2fa")).isTrue();
    assertThat(catalog.isAction("users.read")).isFalse();
    assertThat(catalog.isAction("default.write")).isFalse();
    assertThat(catalog.isAction("unknown.read")).isFalse();
  }

  @Test
  void resourceGrantedAlwaysGrantsItsPermissions() {
    assertThat(awe().getGrantedAlways()).containsExactlyInAnyOrder("self.read", "self.update");
  }

  /**
   * The write verb exists only for the default resource
   */
  @Test
  void writeVerbOnAnotherResourceIsALoadError() {
    Permissions permissions = parse("""
      <permissions>
        <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="read,write" />
      </permissions>
      """);
    List<Permissions> moduleFiles = List.of(parse(AWE_CATALOG), permissions);

    assertThatThrownBy(() -> PermissionCatalog.build(moduleFiles))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessage("Permission catalog: the 'write' verb exists only for the 'default' resource, "
        + "resource 'invoices' declares it");
  }

  /**
   * A resource that lists a verb no module declares is a load error
   */
  @Test
  void undeclaredVerbIsALoadError() {
    Permissions permissions = parse("""
      <permissions>
        <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="read,approve" />
      </permissions>
      """);
    List<Permissions> moduleFiles = List.of(parse(AWE_CATALOG), permissions);

    assertThatThrownBy(() -> PermissionCatalog.build(moduleFiles))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessage("Permission catalog: resource 'invoices' lists the undeclared verb 'approve'");
  }

  /**
   * An identifier with a dot, a space or a wildcard cannot be told apart from a permission or a wildcard
   */
  @Test
  void identifierWithReservedCharactersIsALoadError() {
    Permissions permissions = parse("""
      <permissions>
        <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="read">
          <action id="approve.all" label="PERMISSION_INVOICES_APPROVE" />
        </resource>
      </permissions>
      """);
    List<Permissions> moduleFiles = List.of(parse(AWE_CATALOG), permissions);

    assertThatThrownBy(() -> PermissionCatalog.build(moduleFiles))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessage("Permission catalog: invalid identifier 'approve.all' in resource 'invoices'");
  }

  /**
   * An application module reuses the verbs of AWE and adds its resources; the same permission declared by several
   * modules is kept from the first module of the module list, with a warning
   */
  @Test
  void modulesAreCombinedAndTheFirstDeclarationWins() {
    Permissions application = parse("""
      <permissions>
        <resource id="invoices" label="PERMISSION_RESOURCE_INVOICES" verbs="crud,export">
          <action id="approve" label="PERMISSION_INVOICES_APPROVE" />
        </resource>
      </permissions>
      """);
    Permissions other = parse("""
      <permissions>
        <resource id="invoices" label="OTHER_INVOICES" verbs="read">
          <action id="approve" label="OTHER_APPROVE" />
          <action id="cancel" label="PERMISSION_INVOICES_CANCEL" />
        </resource>
      </permissions>
      """);

    PermissionCatalog catalog = PermissionCatalog.build(List.of(application, parse(AWE_CATALOG), other));

    assertThat(catalog.expand("invoices.*")).containsExactly(
      "invoices.read", "invoices.create", "invoices.update", "invoices.delete", "invoices.export",
      "invoices.approve", "invoices.cancel");
    assertThat(catalog.getLabel("invoices.approve")).isEqualTo("PERMISSION_INVOICES_APPROVE");
    assertThat(catalog.getResourceLabel("invoices")).isEqualTo("PERMISSION_RESOURCE_INVOICES");
    assertThat(warnings()).containsExactly(
      "Permission catalog: 'invoices.read' is declared more than once, keeping the first declaration",
      "Permission catalog: 'invoices.approve' is declared more than once, keeping the first declaration");
  }

  /**
   * An application module that redeclares a resource of AWE adds its verbs, its own verb included, to the ones of AWE,
   * and its labels win; an action it redeclares stays an action
   */
  @Test
  void anEarlierModuleExtendsAndRelabelsAResourceOfALaterOne() {
    Permissions application = parse("""
      <permissions>
        <verb id="approve" label="PERMISSION_VERB_APPROVE" />
        <resource id="users" label="APP_USERS" verbs="export,approve">
          <action id="unlock" label="APP_USERS_UNLOCK" />
        </resource>
      </permissions>
      """);

    PermissionCatalog catalog = PermissionCatalog.build(List.of(application, parse(AWE_CATALOG)));

    assertThat(catalog.expand("users.*")).containsExactly(
      "users.export", "users.approve", "users.unlock", "users.read", "users.create", "users.update", "users.delete",
      "users.reset-2fa");
    assertThat(catalog.getResourceLabel("users")).isEqualTo("APP_USERS");
    assertThat(catalog.getLabel("users.approve")).isEqualTo("PERMISSION_VERB_APPROVE");
    assertThat(catalog.getLabel("users.unlock")).isEqualTo("APP_USERS_UNLOCK");
    assertThat(catalog.isAction("users.unlock")).isTrue();
    assertThat(catalog.isAction("users.approve")).isFalse();
    assertThat(warnings()).containsExactly(
      "Permission catalog: 'users.unlock' is declared more than once, keeping the first declaration");
  }

  /**
   * Wildcards are expanded against the catalog: everything, every permission of a resource, one verb on every
   * resource; an exact identifier expands to itself
   */
  @Test
  void expandsWildcardsAgainstTheCatalog() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.expand("*")).isEqualTo(catalog.getPermissionIds());
    assertThat(catalog.expand("users.*")).containsExactly(
      "users.read", "users.create", "users.update", "users.delete", "users.unlock", "users.reset-2fa");
    assertThat(catalog.expand("*.read")).containsExactly(
      "users.read", "profiles.read", "permissions.read", "self.read", "default.read");
    assertThat(catalog.expand("users.unlock")).containsExactly("users.unlock");
  }

  /**
   * An assignment that is not in the catalog, or a wildcard that matches nothing, expands to nothing
   */
  @Test
  void unknownOrEmptyAssignmentExpandsToNothing() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.expand("users.approve")).isEmpty();
    assertThat(catalog.expand("invoices.*")).isEmpty();
    assertThat(catalog.expand("*.approve")).isEmpty();
    assertThat(catalog.expand("")).isEmpty();
    assertThat(catalog.expand(null)).isEmpty();
    assertThat(PermissionCatalog.empty().expand("*")).isEmpty();
  }

  /**
   * The exclusions of a profile file subtract from what its permissions expand to; an exclusion of a permission the
   * file does not grant changes nothing
   */
  @Test
  void exclusionsSubtractFromTheExpandedPermissions() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.expand(List.of("*"), List.of("users.*", "profiles.*", "permissions.*")))
      .containsExactly("self.read", "self.update", "default.read", "default.write");
    assertThat(catalog.expand(List.of("*.read", "users.unlock"), List.of("users.read", "profiles.delete")))
      .containsExactly("users.unlock", "profiles.read", "permissions.read", "self.read", "default.read");
    assertThat(catalog.expand(List.of(), List.of("users.*"))).isEmpty();
  }

  /**
   * A permission or an exclusion of a profile file is a catalog identifier or a wildcard that matches some permission
   */
  @Test
  void validatesTheEntriesOfAProfileFile() {
    PermissionCatalog catalog = awe();

    List.of("*", "users.*", "*.read", "users.unlock")
      .forEach(entry -> assertThat(catalog.validateAssignment(entry)).as(entry).isEmpty());
    assertThat(PermissionCatalog.empty().validateAssignment("*")).isEmpty();
    assertThat(catalog.validateAssignment("users.aprove")).containsExactly("unknown permission 'users.aprove'");
    assertThat(catalog.validateAssignment("invoices.*")).containsExactly("unknown permission 'invoices.*'");
    assertThat(catalog.validateAssignment("*.approve")).containsExactly("unknown permission '*.approve'");
    assertThat(catalog.validateAssignment(" ")).containsExactly("empty permission");
    assertThat(catalog.validateAssignment(null)).containsExactly("empty permission");
  }

  /**
   * A permission attribute names catalog permissions separated by spaces; an unknown one is a problem
   */
  @Test
  void validatesThePermissionsOfAnAttribute() {
    PermissionCatalog catalog = awe();

    assertThat(catalog.validatePermissionAttribute("users.update", false)).isEmpty();
    assertThat(catalog.validatePermissionAttribute("users.update  users.unlock", false)).isEmpty();
    assertThat(catalog.validatePermissionAttribute(null, false)).isEmpty();
    assertThat(catalog.validatePermissionAttribute("users.update users.aprove users.*", false)).containsExactly(
      "unknown permission 'users.aprove'", "unknown permission 'users.*'");
    assertThat(catalog.validatePermissionAttribute(" ", false)).containsExactly("empty permission attribute");
  }

  /**
   * A public target has no session, so it cannot require a permission
   */
  @Test
  void permissionOnAPublicTargetIsAProblem() {
    assertThat(awe().validatePermissionAttribute("users.read", true))
      .containsExactly("a public target cannot declare a permission");
  }
}
