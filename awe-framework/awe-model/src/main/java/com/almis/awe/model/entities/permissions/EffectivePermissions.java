package com.almis.awe.model.entities.permissions;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Effective permissions of some profiles with their provenance: the union of the permissions of each profile, plus the
 * permissions granted always. The permissions of a profile are those of its profile file minus the exclusions of that
 * file, plus the permissions assigned to it (AweProPer), expanded against the catalog. An exclusion only subtracts
 * inside its profile file: it is no denial. An assignment that matches no permission of the catalog grants nothing.
 * The login and the permissions view of the access window read the same computation
 */
public final class EffectivePermissions implements Serializable {

  @Serial
  private static final long serialVersionUID = -3264028317465015512L;

  /**
   * Where a permission comes from
   */
  public enum OriginKind {
    /** A permission of the profile file, not excluded by it */
    FILE,
    /** A permission assigned to the profile (AweProPer) */
    ASSIGNMENT,
    /** A permission of a resource granted always to every authenticated user */
    ALWAYS
  }

  /**
   * What a profile declares
   *
   * @param profile     Profile acronym
   * @param file        Profile file (null when it does not exist)
   * @param permissions Permissions of the profile file (identifiers or wildcards)
   * @param exclusions  Exclusions of the profile file (identifiers or wildcards)
   * @param assignments Permissions assigned to the profile (identifiers or wildcards)
   */
  public record ProfileSource(String profile, String file, List<String> permissions, List<String> exclusions,
                              List<String> assignments) implements Serializable {
  }

  /**
   * Origin of a granted permission
   *
   * @param kind    Kind of origin
   * @param profile Profile acronym (null when granted always)
   * @param file    Profile file of the profile (null when granted always)
   * @param entry   Entry of the profile file or assignment that grants it (null when granted always)
   */
  public record Origin(OriginKind kind, String profile, String file, String entry) implements Serializable {
  }

  /**
   * Exclusion of a profile file that removes a permission the same file grants
   *
   * @param profile Profile acronym
   * @param file    Profile file
   * @param entry   Exclusion entry that removes it
   */
  public record Exclusion(String profile, String file, String entry) implements Serializable {
  }

  private final List<ProfileSource> profiles;
  private final Set<String> granted;
  private final Map<String, List<Origin>> origins;
  private final Map<String, List<Exclusion>> exclusions;

  private EffectivePermissions(List<ProfileSource> profiles, Set<String> granted, Map<String, List<Origin>> origins,
                               Map<String, List<Exclusion>> exclusions) {
    this.profiles = List.copyOf(profiles);
    this.granted = Collections.unmodifiableSet(granted);
    this.origins = origins;
    this.exclusions = exclusions;
  }

  /**
   * Compute the effective permissions of some profiles
   *
   * @param catalog  Permission catalog
   * @param profiles What each profile declares, in the order of the profiles of the user (main profile first)
   * @return Effective permissions with their provenance
   */
  public static EffectivePermissions compute(PermissionCatalog catalog, List<ProfileSource> profiles) {
    Set<String> granted = new LinkedHashSet<>();
    Map<String, List<Origin>> origins = new LinkedHashMap<>();
    Map<String, List<Exclusion>> exclusions = new LinkedHashMap<>();
    for (ProfileSource profile : profiles) {
      List<String> fileGranted = catalog.expand(profile.permissions(), profile.exclusions());
      granted.addAll(fileGranted);
      profile.assignments().forEach(assignment -> granted.addAll(catalog.expand(assignment)));
      for (String permission : catalog.getPermissionIds()) {
        Optional<String> fileEntry = firstMatch(catalog, profile.permissions(), permission);
        if (fileGranted.contains(permission)) {
          add(origins, permission, new Origin(OriginKind.FILE, profile.profile(), profile.file(), fileEntry.orElse(null)));
        } else if (fileEntry.isPresent()) {
          firstMatch(catalog, profile.exclusions(), permission).ifPresent(exclusion ->
            add(exclusions, permission, new Exclusion(profile.profile(), profile.file(), exclusion)));
        }
        firstMatch(catalog, profile.assignments(), permission).ifPresent(assignment ->
          add(origins, permission, new Origin(OriginKind.ASSIGNMENT, profile.profile(), profile.file(), assignment)));
      }
    }
    granted.addAll(catalog.getGrantedAlways());
    catalog.getGrantedAlways().forEach(permission ->
      add(origins, permission, new Origin(OriginKind.ALWAYS, null, null, null)));
    return new EffectivePermissions(profiles, granted, origins, exclusions);
  }

  private static Optional<String> firstMatch(PermissionCatalog catalog, List<String> entries, String permission) {
    return entries.stream().filter(entry -> catalog.expand(entry).contains(permission)).findFirst();
  }

  private static <T> void add(Map<String, List<T>> map, String permission, T value) {
    map.computeIfAbsent(permission, key -> new ArrayList<>()).add(value);
  }

  /**
   * What each profile declares, in their order
   *
   * @return Profile sources
   */
  public List<ProfileSource> getProfiles() {
    return profiles;
  }

  /**
   * Granted permissions: those of each profile in its order, then the ones granted always
   *
   * @return Permission identifiers, without repetitions
   */
  public Set<String> getGranted() {
    return granted;
  }

  /**
   * Whether a permission is granted
   *
   * @param permission Permission identifier
   * @return true when some origin grants it
   */
  public boolean isGranted(String permission) {
    return granted.contains(permission);
  }

  /**
   * Origins of a permission: for each profile, its profile file and its first matching assignment, then granted always
   *
   * @param permission Permission identifier
   * @return Origins, empty when it is not granted
   */
  public List<Origin> getOrigins(String permission) {
    return List.copyOf(origins.getOrDefault(permission, List.of()));
  }

  /**
   * Exclusions that remove a permission from the profile file that grants it
   *
   * @param permission Permission identifier
   * @return Exclusions, empty when no profile file excludes it
   */
  public List<Exclusion> getExclusions(String permission) {
    return List.copyOf(exclusions.getOrDefault(permission, List.of()));
  }
}
