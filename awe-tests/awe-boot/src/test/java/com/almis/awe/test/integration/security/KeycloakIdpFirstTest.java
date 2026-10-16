package com.almis.awe.test.integration.security;

import com.almis.awe.testing.database.TestDatabaseImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.CookieHandler;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdP-first login against a real Keycloak (#767): the identity provider is the only way in
 * ({@code awe.security.sso.local-login=false}), roles come from the ID token claims, and the provider can end AWE sessions
 * with back-channel logout.
 *
 * <p>Keycloak runs in a container started once per JVM from the image pinned in {@code awe-testing-images.properties}
 * (name, tag and digest, kept up to date by Renovate) with the test realm {@code keycloak/awe-test-realm.json}. AWE runs
 * as a real web application on a random free port, which is chosen only when the Spring context asks for it (once Keycloak
 * is up, to keep as short as possible the time between probing the port and binding it) and is then written into the
 * client of the running realm through the Keycloak administration API (redirect address and back-channel logout address).
 * Nothing listens on a fixed host port: Keycloak's own port 8080 is only the one inside its container and the host port is
 * assigned by Docker. Keycloak reaches AWE for the back-channel call through {@code host.docker.internal} (the Docker host gateway), or
 * through the host named by the environment variable {@code AWE_TESTING_KEYCLOAK_BACK_CHANNEL_HOST} when the test JVM is
 * not on the Docker host (CI with Docker in Docker).</p>
 *
 * <p>The browser is plain {@link HttpClient} with a cookie jar: it follows the authorization code flow and fills in the
 * Keycloak login form, so it exercises the same redirects as a user. Skipped when Docker is not available.</p>
 */
@Tag("Keycloak")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("IdP-first login with Keycloak")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class KeycloakIdpFirstTest {

  private static final String REALM = "awe-test";
  private static final String CLIENT_ID = "awe";
  // Throwaway values of a container and a realm that only live for the test run
  private static final String CLIENT_SECRET = "awe-test-client-secret";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASSWORD = "awe-test-admin-password";
  private static final String USER_PASSWORD = "awe-test-password";
  private static final String BACK_CHANNEL_HOST_VARIABLE = "AWE_TESTING_KEYCLOAK_BACK_CHANNEL_HOST";
  // Name of the Docker host inside the Keycloak container (extra host to the gateway), so the back-channel call can reach AWE
  private static final String DOCKER_HOST_ALIAS = "host.docker.internal";
  private static final int KEYCLOAK_PORT = 8080;
  // Every call of the test has a deadline: a hung Keycloak or AWE fails the test instead of blocking the build
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final String PRIVATE_PATH = "/screen/home";
  private static final Pattern FORM_TAG = Pattern.compile("<form[^>]*kc-form-login[^>]*>");
  private static final Pattern ACTION = Pattern.compile("action=\"([^\"]+)\"");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static GenericContainer<?> keycloak;
  private static volatile int awePort;
  private static String keycloakUrl;

  @Autowired
  private JdbcTemplate jdbc;

  /**
   * Starts Keycloak (once) and points AWE at it. It runs when the Spring context is built, before the application starts,
   * because the OIDC client registration reads the provider metadata at startup.
   */
  @DynamicPropertySource
  static void identityProvider(DynamicPropertyRegistry registry) {
    startKeycloak();
    String issuer = keycloakUrl + "/realms/" + REALM;
    String registration = "spring.security.oauth2.client.registration.keycloak.";
    registry.add("server.port", KeycloakIdpFirstTest::awePort);
    registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", () -> issuer);
    registry.add("spring.security.oauth2.client.provider.keycloak.user-name-attribute", () -> "preferred_username");
    registry.add(registration + "provider", () -> "keycloak");
    registry.add(registration + "client-id", () -> CLIENT_ID);
    registry.add(registration + "client-secret", () -> CLIENT_SECRET);
    registry.add(registration + "authorization-grant-type", () -> "authorization_code");
    registry.add(registration + "scope", () -> "openid,profile,email");
    registry.add(registration + "redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");
    registry.add("awe.security.sso.enabled", () -> "true");
    // IdP-first: no local login, so a role that maps to an AWE profile is required (fail closed)
    registry.add("awe.security.sso.local-login", () -> "false");
    registry.add("awe.security.sso.filter-authority-prefix", () -> "role_");
    registry.add("awe.security.sso.roles-claims", () -> "realm_access.roles,resource_access.awe.roles");
    registry.add("awe.security.sso.back-channel-logout.enabled", () -> "true");
    // Keycloak calls AWE with the address of the Docker host, which AWE cannot use to call itself
    registry.add("awe.security.sso.back-channel-logout.logout-uri",
      () -> "http://localhost:" + awePort() + "/logout/connect/back-channel/{registrationId}");
  }

  private static synchronized void startKeycloak() {
    if (keycloak != null) {
      return;
    }
    String realm = resource("/keycloak/awe-test-realm.json")
      .replace("__CLIENT_SECRET__", CLIENT_SECRET)
      .replace("__USER_PASSWORD__", USER_PASSWORD);
    GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(TestDatabaseImages.keycloak()))
      .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN_USER)
      .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN_PASSWORD)
      .withCopyToContainer(Transferable.of(realm), "/opt/keycloak/data/import/awe-test-realm.json")
      .withCommand("start-dev", "--import-realm")
      .withExposedPorts(KEYCLOAK_PORT)
      .withExtraHost(DOCKER_HOST_ALIAS, "host-gateway")
      .waitingFor(Wait.forHttp("/realms/" + REALM).forPort(KEYCLOAK_PORT).forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(3)));
    container.start();
    keycloakUrl = "http://" + container.getHost() + ":" + container.getMappedPort(KEYCLOAK_PORT);
    keycloak = container;
  }

  /**
   * Port of the AWE web server. It is chosen the first time the Spring context reads it (the server.port property), once
   * Keycloak is already up, so the probe socket is closed only a moment before the web server binds the port, and it is
   * written into the client of the realm then, not into the realm that Keycloak imports.
   */
  private static synchronized int awePort() {
    if (awePort == 0) {
      int port = freePort();
      String backChannelHost = System.getenv(BACK_CHANNEL_HOST_VARIABLE);
      if (backChannelHost == null || backChannelHost.isBlank()) {
        backChannelHost = DOCKER_HOST_ALIAS;
      }
      try {
        registerAweInKeycloak(port, backChannelHost);
      } catch (Exception exception) {
        throw new IllegalStateException("Cannot register AWE in the client of Keycloak", exception);
      }
      awePort = port;
    }
    return awePort;
  }

  /**
   * Point the client of the realm at the port of AWE (redirect, origin, logout) with the administration API
   */
  private static void registerAweInKeycloak(int port, String backChannelHost) throws Exception {
    HttpClient client = newClient();
    String token = adminToken(client);
    String clients = keycloakUrl + "/admin/realms/" + REALM + "/clients";
    JsonNode found = JSON.readTree(send(client, request(clients + "?clientId=" + CLIENT_ID).header("Authorization", "Bearer " + token)
      .GET().build()).body());
    ObjectNode awe = (ObjectNode) found.get(0);
    String base = "http://localhost:" + port;
    awe.put("rootUrl", base);
    awe.putArray("redirectUris").add(base + "/login/oauth2/code/keycloak");
    awe.putArray("webOrigins").add(base);
    ObjectNode attributes = (ObjectNode) awe.get("attributes");
    attributes.put("post.logout.redirect.uris", base + "/*");
    attributes.put("backchannel.logout.url", "http://" + backChannelHost + ":" + port + "/logout/connect/back-channel/keycloak");
    HttpResponse<String> updated = send(client, request(clients + "/" + awe.get("id").asString())
      .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
      .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(awe))).build());
    if (updated.statusCode() != 204) {
      throw new IllegalStateException("Keycloak answered " + updated.statusCode() + " updating the client: " + updated.body());
    }
  }

  private static HttpClient newClient() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  private static HttpRequest.Builder request(String uri) {
    return HttpRequest.newBuilder(URI.create(uri)).timeout(REQUEST_TIMEOUT);
  }

  private static HttpResponse<String> send(HttpClient client, HttpRequest request) throws Exception {
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static String adminToken(HttpClient client) throws Exception {
    return JSON.readTree(send(client, request(keycloakUrl + "/realms/master/protocol/openid-connect/token")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(HttpRequest.BodyPublishers.ofString(form("grant_type", "password", "client_id", "admin-cli",
        "username", ADMIN_USER, "password", ADMIN_PASSWORD))).build()).body()).get("access_token").asString();
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String resource(String path) {
    try (InputStream stream = KeycloakIdpFirstTest.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException("Resource " + path + " not found in the classpath");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  /**
   * AWE users that exist before the logins: bob with the operator profile (the role of the provider must replace it) and
   * dave, disabled in AWE. The other users of the realm are not in AWE and are provisioned at their first login.
   */
  @BeforeEach
  void aweUsers() {
    jdbc.update("delete from ope where l1_nom in ('alice', 'bob', 'carol', 'dave', 'erin', 'frank', 'grace')");
    insertUser(9001, "bob", 1);
    insertUser(9002, "dave", 0);
  }

  /**
   * The password hash is the literal that every user of the {@code testdata-*.sql} scripts has (RIPEMD-160, what AWE stores
   * in l1_pas). It only fills the column: with {@code awe.security.sso.local-login=false} nobody signs in with it.
   */
  private void insertUser(int id, String name, int enabled) {
    jdbc.update("insert into ope (IdeOpe, l1_nom, l1_pas, l1_con, l1_act, l1_uti, imp_nom, l1_lan, l1_sgn, EmlAdr, OpeNam, IdePro, IdeThm) "
        + "values (?, ?, '5e52fee47e6b070565f74372468cdc699de89107', 0, ?, 0, 'none', 'ENG', 1, ?, ?, "
        + "(select IdePro from AwePro where Nam = 'operator'), (select IdeThm from AweThm where Nam = 'sunset'))",
      id, name, enabled, name + "@awe-test.example", name);
  }

  private List<String> profilesOf(String user) {
    return jdbc.queryForList("select p.Nam from ope o join AwePro p on p.IdePro = o.IdePro where o.l1_nom = ?",
      String.class, user);
  }

  private int usersNamed(String user) {
    Integer count = jdbc.queryForObject("select count(*) from ope where l1_nom = ?", Integer.class, user);
    return count == null ? 0 : count;
  }

  @Test
  @DisplayName("A user of the provider whose role maps to an AWE profile signs in, and is provisioned with that profile")
  void loginProvisionsTheUserWithTheProfileOfTheRole() throws Exception {
    Browser browser = new Browser();
    assertThat(browser.privatePageStatus()).as("before signing in, the private page sends the browser away").isNotEqualTo(200);

    Browser.Result result = browser.signIn("alice");

    assertThat(result.status()).isEqualTo(200);
    assertThat(result.uri().getPort()).as("back in AWE").isEqualTo(awePort());
    assertThat(browser.privatePageStatus()).as("the AWE session is open").isEqualTo(200);
    assertThat(profilesOf("alice")).containsExactly("general");
  }

  @Test
  @DisplayName("The role of the provider replaces the profile stored in AWE for an existing user")
  void loginSynchronizesTheProfileOfAnExistingUser() throws Exception {
    assertThat(profilesOf("bob")).containsExactly("operator");

    Browser browser = new Browser();
    assertThat(browser.signIn("bob").status()).isEqualTo(200);

    assertThat(browser.privatePageStatus()).isEqualTo(200);
    assertThat(profilesOf("bob")).containsExactly("administrator");
  }

  @Test
  @DisplayName("The roles can come from a second configured claim (client roles)")
  void rolesAreReadFromTheConfiguredClaims() throws Exception {
    Browser browser = new Browser();

    assertThat(browser.signIn("frank").status()).isEqualTo(200);

    assertThat(browser.privatePageStatus()).isEqualTo(200);
    assertThat(profilesOf("frank")).containsExactly("operator");
  }

  @Test
  @DisplayName("Fails closed: a user whose roles map to no AWE profile is refused and nothing is provisioned")
  void loginWithoutMappableRoleIsRefused() throws Exception {
    for (String user : List.of("carol", "erin")) {
      Browser browser = new Browser();

      Browser.Result result = browser.signIn(user);

      assertThat(result.status()).as("login of %s", user).isEqualTo(401);
      assertThat(browser.privatePageStatus()).as("session of %s", user).isNotEqualTo(200);
      assertThat(usersNamed(user)).as("records of %s", user).isZero();
    }
  }

  @Test
  @DisplayName("A user disabled in AWE is refused on SSO even though the provider accepts the account")
  void loginOfAUserDisabledInAweIsRefused() throws Exception {
    Browser browser = new Browser();

    Browser.Result result = browser.signIn("dave");

    assertThat(result.status()).isEqualTo(401);
    assertThat(browser.privatePageStatus()).isNotEqualTo(200);
    assertThat(profilesOf("dave")).as("the stored profile is not touched").containsExactly("operator");
  }

  @Test
  @DisplayName("Back-channel logout: ending the user's sessions in the provider invalidates the AWE session")
  void backChannelLogoutEndsTheAweSession() throws Exception {
    Browser browser = new Browser();
    assertThat(browser.signIn("grace").status()).isEqualTo(200);
    assertThat(browser.privatePageStatus()).isEqualTo(200);

    int status = logoutInKeycloak("grace");

    assertThat(status).as("answer of the Keycloak administration API").isEqualTo(204);
    // The provider posts the logout token while it ends the sessions, but give a slow machine time to finish the call
    int statusAfter = 200;
    for (int attempt = 0; attempt < 20 && statusAfter == 200; attempt++) {
      statusAfter = browser.privatePageStatus();
      if (statusAfter == 200) {
        Thread.sleep(500);
      }
    }
    assertThat(statusAfter).as("the AWE session was ended by the back-channel logout").isNotEqualTo(200);
  }

  /**
   * Ends all the sessions of a user in Keycloak, as an administrator would. Keycloak then posts the logout token to the
   * back-channel logout address of the client.
   */
  private static int logoutInKeycloak(String userName) throws Exception {
    HttpClient client = newClient();
    String token = adminToken(client);
    String admin = keycloakUrl + "/admin/realms/" + REALM + "/users";
    JsonNode users = JSON.readTree(send(client, request(admin + "?exact=true&username=" + userName)
      .header("Authorization", "Bearer " + token).GET().build()).body());
    String id = users.get(0).get("id").asString();
    return client.send(request(admin + "/" + id + "/logout")
      .header("Authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.noBody()).build(),
      HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  private static String form(String... pairs) {
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < pairs.length; i += 2) {
      body.append(i == 0 ? "" : "&").append(encode(pairs[i])).append('=').append(encode(pairs[i + 1]));
    }
    return body.toString();
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static URI aweUri(String path) {
    return URI.create("http://localhost:" + awePort() + path);
  }

  /**
   * A browser: a cookie jar and two clients that share it, one that follows redirects (to sign in) and one that does not
   * (to see how AWE answers a page)
   */
  private static final class Browser {

    private final CookieHandler cookies = new PlainCookieJar();
    private final HttpClient following = HttpClient.newBuilder().cookieHandler(cookies)
      .followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(10)).build();
    private final HttpClient direct = HttpClient.newBuilder().cookieHandler(cookies)
      .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build();

    record Result(int status, URI uri, String body) {
    }

    /**
     * A cookie jar for plain HTTP. Keycloak marks its cookies {@code Secure} even on {@code http://localhost} (browsers treat
     * localhost as a secure context), and the JDK {@code CookieManager} then never sends them back, so the login form
     * is answered with "Restart login cookie not found" (HTTP 400). Cookies are kept by host and port (AWE and Keycloak
     * share the host name) and sent back to the paths they were set for.
     */
    private static final class PlainCookieJar extends CookieHandler {

      private record Stored(String name, String value, String path) {
      }

      private final Map<String, Map<String, Stored>> jar = new HashMap<>();

      private static String key(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
      }

      @Override
      public synchronized Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) {
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        String header = jar.getOrDefault(key(uri), Map.of()).values().stream()
          .filter(cookie -> path.startsWith(cookie.path()))
          .map(cookie -> cookie.name() + "=" + cookie.value())
          .collect(Collectors.joining("; "));
        return header.isEmpty() ? Map.of() : Map.of("Cookie", List.of(header));
      }

      @Override
      public synchronized void put(URI uri, Map<String, List<String>> responseHeaders) {
        Map<String, Stored> cookies = jar.computeIfAbsent(key(uri), host -> new HashMap<>());
        responseHeaders.forEach((header, values) -> {
          if (!"set-cookie".equalsIgnoreCase(header)) {
            return;
          }
          for (String line : values) {
            String[] parts = line.split(";");
            int equals = parts[0].indexOf('=');
            if (equals < 1) {
              continue;
            }
            String name = parts[0].substring(0, equals).trim();
            String value = parts[0].substring(equals + 1).trim();
            String path = "/";
            boolean expired = value.isEmpty() || value.equals("\"\"");
            for (int i = 1; i < parts.length; i++) {
              String attribute = parts[i].trim();
              if (attribute.regionMatches(true, 0, "Path=", 0, 5)) {
                path = attribute.substring(5);
              } else if (attribute.equalsIgnoreCase("Max-Age=0")) {
                expired = true;
              }
            }
            if (expired) {
              cookies.remove(name);
            } else {
              cookies.put(name, new Stored(name, value, path));
            }
          }
        });
      }
    }

    /**
     * Walk the authorization code flow: AWE sends the browser to Keycloak, the login form is filled in, and Keycloak sends
     * it back to AWE with the code
     */
    Result signIn(String user) throws Exception {
      HttpResponse<String> loginPage = following.send(HttpRequest.newBuilder(aweUri("/oauth2/authorization/keycloak")).timeout(REQUEST_TIMEOUT).GET().build(),
        HttpResponse.BodyHandlers.ofString());
      assertThat(loginPage.statusCode()).as("Keycloak login page: %s", loginPage.uri()).isEqualTo(200);
      Matcher tag = FORM_TAG.matcher(loginPage.body());
      assertThat(tag.find()).as("login form in the page of %s", loginPage.uri()).isTrue();
      Matcher action = ACTION.matcher(tag.group());
      assertThat(action.find()).as("action of the login form").isTrue();

      HttpResponse<String> result = following.send(HttpRequest.newBuilder(URI.create(action.group(1).replace("&amp;", "&"))).timeout(REQUEST_TIMEOUT)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(form("username", user, "password", USER_PASSWORD))).build(),
        HttpResponse.BodyHandlers.ofString());
      return new Result(result.statusCode(), result.uri(), result.body());
    }

    /**
     * Status of a private page without following redirects: 200 with an AWE session, a redirect to the sign-in without it
     */
    int privatePageStatus() throws Exception {
      return direct.send(HttpRequest.newBuilder(aweUri(PRIVATE_PATH)).timeout(REQUEST_TIMEOUT).GET().build(), HttpResponse.BodyHandlers.discarding())
        .statusCode();
    }
  }
}
