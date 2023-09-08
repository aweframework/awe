package com.almis.awe.test.integration.controller;

import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Class used for testing /api rest controller
 */
@Tag("integration")
@DisplayName("API rest controller tests")
class ApiRestControllerTest extends AbstractSpringFixedEnvironmentIT {

  @LocalServerPort
  private int port;

  TestRestTemplate restTemplate = new TestRestTemplate();
  HttpHeaders headers = new HttpHeaders();

  @Autowired
  private JWTTokenService jwtTokenService;

  // Constants
  private final String queryIdAuth = "SimpleEnum";

  /**
   * Creates an url with the local port
   *
   * @param uri Uri
   * @return localhost url
   */
  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }

  /**
   * Authenticate user on headers
   * @param headers Headers to add authentication
   */
  private void authenticateUser(HttpHeaders headers) {
    String jwtToken = JWT.create()
      .withSubject("test")
      .withExpiresAt(new Date(System.currentTimeMillis() + 60000)) // 1 min
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
    headers.setBearerAuth(jwtToken);
  }

  @Nested
  @DisplayName("API Data Tests")
  class ApiDataRestTest {

    // Authenticate
    @Test
    void authenticate_success() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"));
      LoginRequest loginRequest = new LoginRequest()
        .setUsername("test")
        .setPassword("test");
      HttpEntity<LoginRequest> loginRequestEntity = new HttpEntity<>(loginRequest, headers);

      ResponseEntity<LoginResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, loginRequestEntity, LoginResponse.class);
      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertNotNull(Objects.requireNonNull(response.getBody()).getToken());
    }

    @Test
    void authenticateWithBadCredentials_unauthorized() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"));
      LoginRequest loginRequest = new LoginRequest()
        .setUsername("test")
        .setPassword("dummy");
      HttpEntity<LoginRequest> loginRequestEntity = new HttpEntity<>(loginRequest, headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, loginRequestEntity, AweRestResponse.class);
      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals("Bad credentials", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void authenticateWithUserNotFound_unauthorized() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"));
      LoginRequest loginRequest = new LoginRequest()
        .setUsername("foo")
        .setPassword("dummy");
      HttpEntity<LoginRequest> loginRequestEntity = new HttpEntity<>(loginRequest, headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, loginRequestEntity, AweRestResponse.class);
      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals("User not found or not active", Objects.requireNonNull(response.getBody()).getMessage());
    }

    // Queries

    @Test
    void protectedQuery_unauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/data/" + queryIdAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void unknownProtectedQuery_badRequest() {
      //Authenticate users
      authenticateUser(headers);

      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
        createURLWithPort("/api/data/unknownQuery"),
        HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
      assertEquals("Query 'unknownQuery' has not been defined", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void protectedQuery_success() {
      //Authenticate users
      authenticateUser(headers);

      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/data/" + queryIdAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals(2, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void protectedQueryWithParameters_success() {
      //Authenticate users
      authenticateUser(headers);
      // Build request
      headers.setContentType(MediaType.APPLICATION_JSON);
      RequestParameter parameters = new RequestParameter();
      Map<String, Object> paramMap = new HashMap<>();
      paramMap.put("var", 1);
      parameters.setParameters(paramMap);

      HttpEntity<RequestParameter> entity = new HttpEntity<>(parameters, headers);
      String queryWithVariable = "QueryVariableInField";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/data/" + queryWithVariable),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals(4, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void publicQuery_success() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String queryIdNoAuth = "SimpleEnumPub";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + queryIdNoAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals(2, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void protectedQueryWithPublicApi_unauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String queryProtected = "getApplicationParameters";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + queryProtected),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals(AnswerType.ERROR, Objects.requireNonNull(response.getBody()).getType());
    }

    @Test
    void unknownPublicQuery_unauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String unknownQuery = "unknownQuery";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + unknownQuery),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals(AnswerType.ERROR, Objects.requireNonNull(response.getBody()).getType());
    }
  }

  @Nested
  @DisplayName("API Maintain Tests")
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  class ApiMaintainRestTest {

    // Constants
    private static final String TEST_INSERT = "testInsert";
    private static final String TEST_UPDATE = "testUpdate";
    private static final String TEST_UPDATE_PARAMETERS = "testUpdateParameters";
    private static final String TEST_DELETE = "testDelete";
    private static final String TEST_PUBLIC_MAINTAIN = "testPublicDelete";

    @Test
    @Order(1)
    void maintainAInsert_unauthorized() {
      HttpEntity<String> entity = new HttpEntity<>(null, headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_INSERT),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    @Order(2)
    void maintainAInsert_success() {
      // Authenticate user
      authenticateUser(headers);
      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_INSERT),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    @Order(3)
    void maintainBUpdate_success() {
      // Authenticate user
      authenticateUser(headers);
      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_UPDATE),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    @Order(4)
    void maintainCUpdateWithParameters_success() {
      // Authenticate user
      authenticateUser(headers);

      // Build request
      headers.setContentType(MediaType.APPLICATION_JSON);
      RequestParameter parameters = new RequestParameter();
      Map<String, Object> paramMap = new HashMap<>();
      paramMap.put("nam", "testIncludeTargetUpd");
      parameters.setParameters(paramMap);

      HttpEntity<RequestParameter> entity = new HttpEntity<>(parameters, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_UPDATE_PARAMETERS),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    @Order(5)
    void maintainDelete_success() {
      // Authenticate user
      authenticateUser(headers);

      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_DELETE),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    // API Public maintain

    @Test
    @Order(6)
    void publicMaintain_success() {

      // Build request
      headers.setContentType(MediaType.APPLICATION_JSON);
      RequestParameter parameters = new RequestParameter();
      Map<String, Object> paramMap = new HashMap<>();
      paramMap.put("User", "foo");
      parameters.setParameters(paramMap);

      HttpEntity<RequestParameter> entity = new HttpEntity<>(parameters, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/maintain/" + TEST_PUBLIC_MAINTAIN),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    @Order(7)
    void unknownPublicMaintain_unauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String maintain = "unknownMaintain";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
        createURLWithPort("/api/public/maintain/" + maintain),
        HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
      assertEquals(AnswerType.ERROR, Objects.requireNonNull(response.getBody()).getType());
    }
  }
}
