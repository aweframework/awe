package com.almis.awe.model.entities.permissions;

import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.permissions.EffectivePermissions.Exclusion;
import com.almis.awe.model.entities.permissions.EffectivePermissions.Origin;
import com.almis.awe.model.entities.permissions.EffectivePermissions.OriginKind;
import com.almis.awe.model.entities.permissions.EffectivePermissions.ProfileSource;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Effective permissions of some profiles with their provenance: per profile, (profile file - exclusions) + assignments,
 * plus the permissions granted always
 */
class EffectivePermissionsTest {

  private static final String CATALOG = """
    <permissions>
      <verb id="read" label="PERMISSION_VERB_READ" />
      <verb id="create" label="PERMISSION_VERB_CREATE" />
      <verb id="update" label="PERMISSION_VERB_UPDATE" />
      <verb id="delete" label="PERMISSION_VERB_DELETE" />
      <resource id="users" label="PERMISSION_RESOURCE_USERS" verbs="crud">
        <action id="unlock" label="PERMISSION_USERS_UNLOCK" />
      </resource>
      <resource id="audit" label="PERMISSION_RESOURCE_AUDIT" verbs="read" />
      <resource id="self" label="PERMISSION_RESOURCE_SELF" verbs="read,update" granted="always" />
    </permissions>
    """;

  private final XStreamSerializer serializer = XmlSerializerTestUtil.buildSerializer();
  private final PermissionCatalog catalog = PermissionCatalog.build(
    List.of(serializer.getObjectFromTemplate(Permissions.class, CATALOG)));

  // Manager: every permission of its file but users.*, and users.* assigned
  private static final ProfileSource MGR = new ProfileSource("MGR", "manager", List.of("*"), List.of("users.*"),
    List.of("users.*"));
  private static final ProfileSource RDR = new ProfileSource("RDR", "reader", List.of("*.read"), List.of(), List.of());
  private static final ProfileSource SOP = new ProfileSource("SOP", "general", List.of(), List.of(),
    List.of("users.read", "users.unlock"));

  /**
   * Every profile adds its origins: the file entry that grants it, the first assignment that matches it
   */
  @Test
  void theOriginsOfAPermissionAreEveryProfileThatGrantsIt() {
    EffectivePermissions effective = EffectivePermissions.compute(catalog, List.of(MGR, RDR, SOP));

    assertThat(effective.getOrigins("users.read")).containsExactly(
      new Origin(OriginKind.ASSIGNMENT, "MGR", "manager", "users.*"),
      new Origin(OriginKind.FILE, "RDR", "reader", "*.read"),
      new Origin(OriginKind.ASSIGNMENT, "SOP", "general", "users.read"));
    assertThat(effective.getOrigins("audit.read")).containsExactly(
      new Origin(OriginKind.FILE, "MGR", "manager", "*"),
      new Origin(OriginKind.FILE, "RDR", "reader", "*.read"));
    assertThat(effective.getProfiles()).containsExactly(MGR, RDR, SOP);
  }

  /**
   * An exclusion subtracts only inside its profile file: it is kept as a warning when another origin grants it, and the
   * permission is not granted when nothing else does
   */
  @Test
  void anExclusionSubtractsOnlyFromItsOwnProfileFile() {
    ProfileSource manager = new ProfileSource("MGR", "manager", List.of("*"), List.of("users.*"), List.of());

    EffectivePermissions alone = EffectivePermissions.compute(catalog, List.of(manager));
    EffectivePermissions withSupport = EffectivePermissions.compute(catalog, List.of(manager, SOP));

    assertThat(alone.isGranted("users.delete")).isFalse();
    assertThat(alone.getOrigins("users.delete")).isEmpty();
    assertThat(alone.getExclusions("users.delete")).containsExactly(new Exclusion("MGR", "manager", "users.*"));
    assertThat(withSupport.isGranted("users.unlock")).isTrue();
    assertThat(withSupport.getExclusions("users.unlock")).containsExactly(new Exclusion("MGR", "manager", "users.*"));
    // An exclusion of something its file does not grant is no exclusion
    assertThat(EffectivePermissions.compute(catalog, List.of(new ProfileSource("RDR", "reader", List.of("*.read"),
      List.of("users.*"), List.of()))).getExclusions("users.delete")).isEmpty();
  }

  /**
   * The assignments are expanded against the catalog: everything, a resource, a verb on every resource or one
   */
  @Test
  void wildcardAssignmentsAreExpandedAgainstTheCatalog() {
    ProfileSource assigned = new ProfileSource("ASG", "general", List.of(), List.of(), List.of("*.read", "users.*"));
    ProfileSource everything = new ProfileSource("ALL", "general", List.of(), List.of(), List.of("*"));

    assertThat(EffectivePermissions.compute(catalog, List.of(assigned)).getGranted())
      .containsExactlyInAnyOrder("users.read", "audit.read", "self.read", "users.create", "users.update",
        "users.delete", "users.unlock", "self.update");
    assertThat(EffectivePermissions.compute(catalog, List.of(everything)).getGranted())
      .containsExactlyInAnyOrderElementsOf(catalog.getPermissionIds());
  }

  /**
   * The permissions granted always are granted without profiles, with their own origin
   */
  @Test
  void thePermissionsGrantedAlwaysHaveTheirOwnOrigin() {
    EffectivePermissions effective = EffectivePermissions.compute(catalog, List.of());

    assertThat(effective.getGranted()).containsExactly("self.read", "self.update");
    assertThat(effective.getOrigins("self.update")).containsExactly(new Origin(OriginKind.ALWAYS, null, null, null));
    assertThat(effective.isGranted("users.read")).isFalse();
  }

  /**
   * An assignment that matches no permission of the catalog grants nothing and is no origin
   */
  @Test
  void anUnknownAssignmentIsIgnored() {
    ProfileSource unknown = new ProfileSource("UNK", "general", List.of(), List.of(), List.of("ghost.read", "users.read"));

    EffectivePermissions effective = EffectivePermissions.compute(catalog, List.of(unknown));

    assertThat(effective.getGranted()).containsExactly("users.read", "self.read", "self.update");
    assertThat(effective.getOrigins("users.read"))
      .containsExactly(new Origin(OriginKind.ASSIGNMENT, "UNK", "general", "users.read"));
  }
}
