package com.almis.awe.security.authorization;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.services.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Inventory of the targets that the screens of the private menu let a client launch and that declare no
 * {@code permission}: what is left to declare before switching the authorization mode to {@code enforce}.
 *
 * <p>It lists the queries and maintain targets of the screens ({@link ScreenTargetIndex}) and the services that their
 * server actions call. A target that cannot take a permission is not listed: public and server-only queries and
 * maintains, targets used by a screen of the public menu, and services authorized by their target (dispatchers).</p>
 *
 * <p>When the authorization mode is {@code warn}, the inventory is logged once at startup. With
 * {@code awe.security.permissions.require-undeclared} the targets are still listed: they require {@code default.read}
 * or {@code default.write}, which a profile with {@code *} or {@code default.*} grants them all.</p>
 */
@Slf4j
public class UndeclaredTargetInventory extends ServiceConfig {

  private static final TargetKind[] LOG_ORDER = {TargetKind.QUERY, TargetKind.MAINTAIN, TargetKind.SERVICE};

  private final SecurityConfigProperties securityConfigProperties;
  private final BaseConfigProperties baseConfigProperties;
  private final ScreenTargetIndex screenTargetIndex;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   * @param baseConfigProperties     Base configuration properties
   * @param screenTargetIndex        Index of the targets used by each option
   */
  public UndeclaredTargetInventory(SecurityConfigProperties securityConfigProperties,
                                   BaseConfigProperties baseConfigProperties, ScreenTargetIndex screenTargetIndex) {
    this.securityConfigProperties = securityConfigProperties;
    this.baseConfigProperties = baseConfigProperties;
    this.screenTargetIndex = screenTargetIndex;
  }

  /**
   * Log once at startup, in {@code warn} mode, the targets of the private screens that declare no permission. A
   * failure is logged and never stops the startup
   */
  @EventListener(ApplicationReadyEvent.class)
  public void logUndeclaredTargets() {
    if (securityConfigProperties.getAuthorization().getMode() != AuthorizationMode.WARN) {
      return;
    }
    try {
      Map<TargetKind, Set<String>> undeclared = getUndeclaredTargets();
      if (undeclared.isEmpty()) {
        return;
      }
      int count = undeclared.values().stream().mapToInt(Set::size).sum();
      StringBuilder lines = new StringBuilder();
      undeclared.forEach((kind, ids) -> lines.append("\n  ").append(kind.label()).append(": ")
        .append(String.join(", ", ids)));
      log.warn("{} targets used by private screens declare no permission (authorization mode is warn{}):{}", count,
        securityConfigProperties.getPermissions().isRequireUndeclared()
          ? ", they require default.read or default.write" : "", lines);
    } catch (RuntimeException exc) {
      log.warn("The targets that declare no permission could not be listed: {}", exc.getMessage());
    }
  }

  /**
   * Retrieve the targets of the private screens that declare no permission
   *
   * @return Sorted identifiers by kind (query, maintain and service), only the kinds with some target
   */
  public Map<TargetKind, Set<String>> getUndeclaredTargets() {
    AweElements elements = getElements();
    Set<String> publicOptions = getPublicOptions(elements);
    Map<String, Query> queries = elements.getQueryMap();
    Map<String, Target> maintains = elements.getMaintainMap();
    Map<TargetKind, Set<String>> undeclared = new EnumMap<>(TargetKind.class);

    screenTargetIndex.getTargets().forEach((targetId, options) -> {
      if (Collections.disjoint(options, publicOptions)) {
        if (isUndeclaredQuery(queries.get(targetId))) {
          add(undeclared, TargetKind.QUERY, targetId);
        }
        if (isUndeclaredMaintain(maintains.get(targetId))) {
          add(undeclared, TargetKind.MAINTAIN, targetId);
        }
      }
    });

    screenTargetIndex.getServerActions().forEach((actionId, options) -> {
      if (Collections.disjoint(options, publicOptions)) {
        Optional.ofNullable(getActionService(elements, actionId))
          .filter(this::isUndeclaredService)
          .ifPresent(service -> add(undeclared, TargetKind.SERVICE, service.getId()));
      }
    });

    Map<TargetKind, Set<String>> ordered = new LinkedHashMap<>();
    Arrays.stream(LOG_ORDER).filter(undeclared::containsKey).forEach(kind -> ordered.put(kind, undeclared.get(kind)));
    return Collections.unmodifiableMap(ordered);
  }

  private Set<String> getPublicOptions(AweElements elements) {
    try {
      return elements.getMenu(baseConfigProperties.getFiles().getMenuPublic())
        .getElementsByType(Option.class).stream()
        .map(Option::getName)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    } catch (AWException exc) {
      throw new IllegalStateException(exc.getMessage(), exc);
    }
  }

  /**
   * Retrieve the service that a server action calls, if any
   */
  private Service getActionService(AweElements elements, String actionId) {
    try {
      Action action = elements.getAction(actionId);
      if (action == null || action.getCall() == null || action.getCall().getService() == null) {
        return null;
      }
      return elements.getService(action.getCall().getService());
    } catch (AWException exc) {
      log.debug("Action {} could not be read to list its service", actionId, exc);
      return null;
    }
  }

  /**
   * Whether a query exists, can take a permission (private and launched by clients) and declares none
   */
  private boolean isUndeclaredQuery(Query query) {
    return query != null && !query.isPublic() && !query.isServerOnly() && isUndeclared(query.getPermission());
  }

  /**
   * Whether a maintain target exists, can take a permission (private and launched by clients) and declares none
   */
  private boolean isUndeclaredMaintain(Target maintain) {
    return maintain != null && !maintain.isPublic() && !maintain.isServerOnly()
      && isUndeclared(maintain.getPermission());
  }

  /**
   * Whether a service is not authorized by its target and declares no permission
   */
  private boolean isUndeclaredService(Service service) {
    return !service.isAuthorizedByTarget() && isUndeclared(service.getPermission());
  }

  private boolean isUndeclared(String permission) {
    return permission == null || permission.isBlank();
  }

  private void add(Map<TargetKind, Set<String>> undeclared, TargetKind kind, String id) {
    undeclared.computeIfAbsent(kind, key -> new TreeSet<>()).add(id);
  }
}
