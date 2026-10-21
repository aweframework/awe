package com.almis.awe.observability.instrumentation;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Gauges of the users connected to this instance right now, read from Spring's {@link SimpUserRegistry} on every
 * scrape: {@value #USERS_METER} (authenticated users with at least one live WebSocket session) and
 * {@value #CONNECTIONS_METER} (their live WebSocket sessions, one per browser tab). The registry drops a session as
 * soon as its WebSocket closes. The login page ({@code PUBLIC}) and anonymous sessions are not counted. Users are
 * never tags.
 */
public class AweConnectionMetrics implements MeterBinder {

  /** Connected users gauge name */
  public static final String USERS_METER = "awe.users.connected";
  /** Open connections gauge name */
  public static final String CONNECTIONS_METER = "awe.connections.active";

  /** User names of the sessions opened before login */
  private static final Set<String> NOT_LOGGED_USERS = Set.of("PUBLIC", "anonymousUser");

  private final SimpUserRegistry userRegistry;

  /**
   * Constructor
   *
   * @param userRegistry Registry of the users with a live WebSocket session
   */
  public AweConnectionMetrics(SimpUserRegistry userRegistry) {
    this.userRegistry = userRegistry;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder(USERS_METER, userRegistry, AweConnectionMetrics::connectedUsers)
      .description("Logged users with at least one live WebSocket session")
      .register(registry);
    Gauge.builder(CONNECTIONS_METER, userRegistry, AweConnectionMetrics::openConnections)
      .description("Live WebSocket sessions of the logged users")
      .register(registry);
  }

  private static double connectedUsers(SimpUserRegistry userRegistry) {
    return loggedUsers(userRegistry).count();
  }

  private static double openConnections(SimpUserRegistry userRegistry) {
    return loggedUsers(userRegistry).mapToInt(user -> user.getSessions().size()).sum();
  }

  private static Stream<SimpUser> loggedUsers(SimpUserRegistry userRegistry) {
    return userRegistry.getUsers().stream()
      .filter(SimpUser::hasSessions)
      .filter(user -> !NOT_LOGGED_USERS.contains(user.getName()))
      .filter(user -> !(user.getPrincipal() instanceof AnonymousAuthenticationToken));
  }
}
