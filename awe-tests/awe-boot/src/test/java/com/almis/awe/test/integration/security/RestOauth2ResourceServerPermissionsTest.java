package com.almis.awe.test.integration.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * In the oauth2-resource-server mode of the REST API the profile names of the token claim, found as the single
 * sign-on finds them, get the permissions the local login gives to the same profiles: a target is launched with the
 * permission of a claim profile, and refused without it. The tokens are signed with a key published by a local JWK
 * set endpoint
 */
@Tag("integration")
@DisplayName("REST API oauth2-resource-server permissions tests")
@TestPropertySource(properties = {
  "awe.security.authorization.mode=enforce",
  "awe.rest.api.auth.mode=oauth2-resource-server",
  "awe.rest.api.oauth2-resource-server.jwt.audiences[0]=" + RestOauth2ResourceServerPermissionsTest.AUDIENCE,
  "awe.rest.api.oauth2-resource-server.principal.profile-claim=profiles",
  "awe.rest.api.oauth2-resource-server.principal.default-profile=general"
})
class RestOauth2ResourceServerPermissionsTest extends ScreenAccessSupport {

  static final String AUDIENCE = "api://awe-rest";
  private static final String ACRONYM = "O9R";
  private static final String PROFILE_NAME = "oauthreader";
  private static final RSAKey SIGNING_KEY = generateKey();
  private static HttpServer jwkSetServer;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void jwkSetUri(DynamicPropertyRegistry registry) throws IOException {
    jwkSetServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    byte[] jwkSet = new JWKSet(SIGNING_KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
    jwkSetServer.createContext("/jwks", exchange -> {
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, jwkSet.length);
      try (OutputStream body = exchange.getResponseBody()) {
        body.write(jwkSet);
      }
    });
    jwkSetServer.start();
    InetSocketAddress address = jwkSetServer.getAddress();
    registry.add("awe.rest.api.oauth2-resource-server.jwt.jwk-set-uri",
      () -> "http://" + address.getHostString() + ":" + address.getPort() + "/jwks");
  }

  @AfterAll
  static void stopJwkSetServer() {
    jwkSetServer.stop(0);
  }

  @BeforeEach
  void createReaderProfile() {
    deleteRows();
    int profile = jdbcTemplate.queryForObject("SELECT MAX(IdePro) FROM AwePro", Integer.class) + 1000;
    jdbcTemplate.update("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (?, ?, ?, 1, NULL, "
      + "'general', 1)", profile, ACRONYM, PROFILE_NAME);
    int permission = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(IdeAweProPer), 0) FROM AweProPer",
      Integer.class) + 1;
    jdbcTemplate.update("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) VALUES (?, ?, 'users.read')", permission,
      profile);
  }

  @AfterEach
  void deleteRows() {
    jdbcTemplate.update("DELETE FROM AweProPer WHERE IdePro IN (SELECT IdePro FROM AwePro WHERE Acr = ?)", ACRONYM);
    jdbcTemplate.update("DELETE FROM AwePro WHERE Acr = ?", ACRONYM);
  }

  @Test
  @DisplayName("A claim profile name, in any case, launches a query with its permission and not a target without it")
  void claimProfileNameGetsItsPermissions() throws Exception {
    String token = token(List.of("OAuthReader"));

    MockHttpServletResponse allowed = api("/api/data/UsrStaCnt", token);
    MockHttpServletResponse refused = api("/api/maintain/UsrDel", token);

    assertThat(allowed.getStatus()).as(allowed.getContentAsString()).isEqualTo(HttpStatus.OK.value());
    assertThat(allowed.getContentAsString()).doesNotContain(REFUSED);
    assertThat(refused.getStatus()).as(refused.getContentAsString()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(refused.getContentAsString()).contains(REFUSED);
  }

  @Test
  @DisplayName("A claim value that is not an AWE profile name, even a profile acronym, gets the default profile")
  void claimValueThatIsNotAProfileNameGetsNoPermission() throws Exception {
    for (String value : List.of("ghost", ACRONYM)) {
      MockHttpServletResponse refused = api("/api/data/UsrStaCnt", token(List.of(value)));

      assertThat(refused.getStatus()).as(value + ": " + refused.getContentAsString())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
      assertThat(refused.getContentAsString()).contains(REFUSED);
    }
  }

  private MockHttpServletResponse api(String path, String token) throws Exception {
    return mockMvc.perform(post(path)
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"parameters\":{}}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse();
  }

  private static String token(List<String> profiles) throws Exception {
    Instant now = Instant.now();
    SignedJWT jwt = new SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).type(JOSEObjectType.JWT).build(),
      new JWTClaimsSet.Builder()
        .subject("oauth-user")
        .claim("preferred_username", "oauth-user")
        .claim("profiles", profiles)
        .audience(AUDIENCE)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(300)))
        .build());
    jwt.sign(new RSASSASigner(SIGNING_KEY));
    return jwt.serialize();
  }

  private static RSAKey generateKey() {
    try {
      return new RSAKeyGenerator(2048).keyID("awe-test").generate();
    } catch (Exception exc) {
      throw new IllegalStateException(exc);
    }
  }
}
