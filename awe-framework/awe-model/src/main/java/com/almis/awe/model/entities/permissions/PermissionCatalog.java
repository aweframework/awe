package com.almis.awe.model.entities.permissions;

import com.almis.awe.exception.AWERuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Permission catalog: the permissions declared by the Permissions.xml files of the modules, combined in module order.
 * A permission identifier is {@code <resource>.<verb>} or {@code <resource>.<action>}
 */
@Slf4j
public final class PermissionCatalog implements Serializable {

  @Serial
  private static final long serialVersionUID = 2871264190637364818L;

  public static final String WILDCARD = "*";
  public static final String SEPARATOR = ".";
  public static final String DEFAULT_RESOURCE = "default";
  public static final String WRITE_VERB = "write";
  private static final String WRITE_VERB_LABEL = "PERMISSION_VERB_WRITE";
  private static final String CRUD = "crud";
  private static final List<String> CRUD_VERBS = List.of("read", "create", "update", "delete");
  private static final String GRANTED_ALWAYS = "always";
  private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_-]+");
  private static final String LOAD_ERROR = "Permission catalog: ";

  // Permission identifier -> label, in declaration order
  private final Map<String, String> labels;
  // Resource identifier -> label
  private final Map<String, String> resourceLabels;
  private final Set<String> grantedAlways;
  // Permissions that are an own action of their resource, not a verb
  private final Set<String> actions;

  private PermissionCatalog(Map<String, String> labels, Map<String, String> resourceLabels, Set<String> grantedAlways,
                            Set<String> actions) {
    this.labels = Collections.unmodifiableMap(labels);
    this.resourceLabels = Collections.unmodifiableMap(resourceLabels);
    this.grantedAlways = Collections.unmodifiableSet(grantedAlways);
    this.actions = Collections.unmodifiableSet(actions);
  }

  /**
   * Catalog without permissions
   *
   * @return Empty catalog
   */
  public static PermissionCatalog empty() {
    return new PermissionCatalog(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(),
      new LinkedHashSet<>());
  }

  /**
   * Combine the Permissions.xml files of the modules. The verbs of every module are available to the resources of
   * every module; the same permission declared more than once keeps its first declaration (module order) and logs a
   * warning
   *
   * @param moduleFiles Permissions files, in module order
   * @return Permission catalog
   * @throws AWERuntimeException when a declaration is invalid (load error)
   */
  public static PermissionCatalog build(List<Permissions> moduleFiles) {
    Map<String, String> verbLabels = new LinkedHashMap<>();
    verbLabels.put(WRITE_VERB, WRITE_VERB_LABEL);
    moduleFiles.forEach(file -> file.getVerbList().forEach(verb -> {
      checkIdentifier(verb.getId(), "verbs");
      verbLabels.putIfAbsent(verb.getId(), verb.getLabel());
    }));

    Map<String, String> labels = new LinkedHashMap<>();
    Map<String, String> resourceLabels = new LinkedHashMap<>();
    Set<String> grantedAlways = new LinkedHashSet<>();
    Set<String> actions = new LinkedHashSet<>();
    moduleFiles.forEach(file -> file.getResourceList().forEach(resource ->
      addResource(resource, verbLabels, labels, resourceLabels, grantedAlways, actions)));
    return new PermissionCatalog(labels, resourceLabels, grantedAlways, actions);
  }

  /**
   * Add the permissions of a resource: one per verb it lists and one per action it declares
   *
   * @param resource       Resource
   * @param verbLabels     Declared verbs (identifier -> label)
   * @param labels         Permission labels, filled in declaration order
   * @param resourceLabels Resource labels
   * @param grantedAlways  Permissions granted to every authenticated user
   * @param actions        Permissions that are an own action of their resource
   */
  private static void addResource(PermissionResource resource, Map<String, String> verbLabels,
                                  Map<String, String> labels, Map<String, String> resourceLabels,
                                  Set<String> grantedAlways, Set<String> actions) {
    String resourceId = resource.getId();
    checkIdentifier(resourceId, "resources");
    resourceLabels.putIfAbsent(resourceId, resource.getLabel());
    boolean always = GRANTED_ALWAYS.equalsIgnoreCase(resource.getGranted());
    for (String verb : parseVerbs(resource)) {
      checkVerb(resourceId, verb, verbLabels);
      addPermission(labels, grantedAlways, resourceId + SEPARATOR + verb, verbLabels.get(verb), always);
    }
    for (PermissionAction action : resource.getActionList()) {
      checkIdentifier(action.getId(), "resource '" + resourceId + "'");
      String permissionId = resourceId + SEPARATOR + action.getId();
      if (!labels.containsKey(permissionId)) {
        actions.add(permissionId);
      }
      addPermission(labels, grantedAlways, permissionId, action.getLabel(), always);
    }
  }

  private static void checkVerb(String resourceId, String verb, Map<String, String> verbLabels) {
    if (!verbLabels.containsKey(verb)) {
      throw new AWERuntimeException(LOAD_ERROR + "resource '" + resourceId + "' lists the undeclared verb '" + verb
        + "'");
    }
    if (WRITE_VERB.equals(verb) && !DEFAULT_RESOURCE.equals(resourceId)) {
      throw new AWERuntimeException(LOAD_ERROR + "the 'write' verb exists only for the 'default' resource, "
        + "resource '" + resourceId + "' declares it");
    }
  }

  /**
   * Verbs listed by a resource ('crud' means read, create, update and delete)
   *
   * @param resource Resource
   * @return Verb identifiers
   */
  private static List<String> parseVerbs(PermissionResource resource) {
    if (resource.getVerbs() == null) {
      return List.of();
    }
    List<String> verbs = new ArrayList<>();
    Arrays.stream(resource.getVerbs().split(","))
      .map(String::trim)
      .filter(verb -> !verb.isEmpty())
      .forEach(verb -> verbs.addAll(CRUD.equals(verb) ? CRUD_VERBS : List.of(verb)));
    return verbs;
  }

  private static void checkIdentifier(String identifier, String location) {
    if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
      throw new AWERuntimeException(LOAD_ERROR + "invalid identifier '" + identifier + "' in " + location);
    }
  }

  private static void addPermission(Map<String, String> labels, Set<String> grantedAlways, String permissionId,
                                    String label, boolean always) {
    if (labels.containsKey(permissionId)) {
      log.warn("Permission catalog: '{}' is declared more than once, keeping the first declaration", permissionId);
      return;
    }
    labels.put(permissionId, label);
    if (always) {
      grantedAlways.add(permissionId);
    }
  }

  /**
   * Whether a permission is an own action of its resource ({@code users.unlock}) instead of a verb
   *
   * @param permissionId Permission identifier
   * @return true for an action of the catalog
   */
  public boolean isAction(String permissionId) {
    return actions.contains(permissionId);
  }

  /**
   * Permission identifiers, in declaration order
   *
   * @return Permission identifiers
   */
  public List<String> getPermissionIds() {
    return List.copyOf(labels.keySet());
  }

  /**
   * Label (locale key) of a permission: the label of its verb or its action
   *
   * @param permissionId Permission identifier
   * @return Label, or null when the permission is not in the catalog
   */
  public String getLabel(String permissionId) {
    return labels.get(permissionId);
  }

  /**
   * Label (locale key) of a resource
   *
   * @param resourceId Resource identifier
   * @return Label, or null when the resource is not in the catalog
   */
  public String getResourceLabel(String resourceId) {
    return resourceLabels.get(resourceId);
  }

  /**
   * Whether a permission is in the catalog
   *
   * @param permissionId Permission identifier
   * @return Permission is in the catalog
   */
  public boolean contains(String permissionId) {
    return permissionId != null && labels.containsKey(permissionId);
  }

  /**
   * Whether the catalog has no permissions
   *
   * @return Catalog is empty
   */
  public boolean isEmpty() {
    return labels.isEmpty();
  }

  /**
   * Permissions granted to every authenticated user (resources with granted="always")
   *
   * @return Permission identifiers
   */
  public Set<String> getGrantedAlways() {
    return grantedAlways;
  }

  /**
   * Expand an assignment against the catalog: {@code *} (everything), {@code users.*} (every permission of a
   * resource), {@code *.read} (one verb or action on every resource) or a permission identifier. An assignment that
   * matches nothing expands to nothing, and the caller logs it
   *
   * @param assignment Assigned permission or wildcard
   * @return Permission identifiers, in catalog order
   */
  public List<String> expand(String assignment) {
    if (assignment == null || assignment.isBlank()) {
      return List.of();
    }
    String value = assignment.trim();
    if (WILDCARD.equals(value)) {
      return getPermissionIds();
    }
    if (value.endsWith(SEPARATOR + WILDCARD)) {
      String resourcePrefix = value.substring(0, value.length() - WILDCARD.length());
      return labels.keySet().stream().filter(permissionId -> permissionId.startsWith(resourcePrefix)).toList();
    }
    if (value.startsWith(WILDCARD + SEPARATOR)) {
      String suffix = value.substring(WILDCARD.length());
      return labels.keySet().stream().filter(permissionId -> permissionId.endsWith(suffix)).toList();
    }
    return contains(value) ? List.of(value) : List.of();
  }

  /**
   * Expand the permissions of a profile file minus its exclusions. An exclusion is a subtraction inside the file, not a
   * denial: it only removes what the permissions of the same file expand to
   *
   * @param assignments Permissions or wildcards
   * @param exclusions  Excluded permissions or wildcards
   * @return Permission identifiers, in catalog order
   */
  public List<String> expand(List<String> assignments, List<String> exclusions) {
    Set<String> excluded = new LinkedHashSet<>();
    exclusions.forEach(exclusion -> excluded.addAll(expand(exclusion)));
    Set<String> granted = new LinkedHashSet<>();
    assignments.forEach(assignment -> granted.addAll(expand(assignment)));
    granted.removeAll(excluded);
    return labels.keySet().stream().filter(granted::contains).toList();
  }

  /**
   * Validate a permission or an exclusion of a profile file: a permission identifier of the catalog or a wildcard that
   * matches some permission ({@code *} is always valid)
   *
   * @param assignment Permission or wildcard
   * @return Problems found, empty when the entry is valid
   */
  public List<String> validateAssignment(String assignment) {
    if (assignment == null || assignment.isBlank()) {
      return List.of("empty permission");
    }
    if (WILDCARD.equals(assignment.trim()) || !expand(assignment).isEmpty()) {
      return List.of();
    }
    return List.of("unknown permission '" + assignment + "'");
  }

  /**
   * Validate the value of a {@code permission} attribute: permission identifiers of the catalog separated by spaces
   * (all of them are required). Wildcards are not allowed, the required permission is explicit
   *
   * @param attributeValue Attribute value (null when the attribute is not declared)
   * @param publicTarget   Whether the query or target is public (no session, so no permission to check)
   * @return Problems found, empty when the attribute is valid
   */
  public List<String> validatePermissionAttribute(String attributeValue, boolean publicTarget) {
    if (attributeValue == null) {
      return List.of();
    }
    if (publicTarget) {
      return List.of("a public target cannot declare a permission");
    }
    if (attributeValue.isBlank()) {
      return List.of("empty permission attribute");
    }
    return Arrays.stream(attributeValue.trim().split("\\s+"))
      .filter(permissionId -> !contains(permissionId))
      .map(permissionId -> "unknown permission '" + permissionId + "'")
      .toList();
  }
}
