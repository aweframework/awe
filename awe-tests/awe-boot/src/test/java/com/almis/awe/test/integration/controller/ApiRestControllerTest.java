package com.almis.awe.test.integration.controller;

import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
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

  @Nested
  @DisplayName("API Data Tests")
  class ApiDataRestTest {

    // Authenticate
    @Test
    void authenticateSuccess() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
              .queryParam("username", "test")
              .queryParam("password", "test");
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<LoginResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, entity, LoginResponse.class);
      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertNotNull(Objects.requireNonNull(response.getBody()).getToken());
    }

    @Test
    void authenticateUnauthorizedWithBadCredentials() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
              .queryParam("username", "test")
              .queryParam("password", "dummy");
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, entity, AweRestResponse.class);
      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
      assertEquals("Bad credentials", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void authenticateUnauthorizedWithUserNotFound() {
      // Build entity and variable maps
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
              .queryParam("username", "foo")
              .queryParam("password", "dummy");
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
              HttpMethod.POST, entity, AweRestResponse.class);
      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
      assertEquals("User not found or not active", Objects.requireNonNull(response.getBody()).getMessage());
    }

    // Queries

    @Test
    void protectedQueryUnauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/data/" + queryIdAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
      assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void protectedQueryAuthorized() {
      //Authenticate users
      autenticateUser(headers);

      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/data/" + queryIdAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals(2, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void protectedQueryWithParametersAuthorized() {
      //Authenticate users
      autenticateUser(headers);
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

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals(4, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void publicQuerySuccess() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String queryIdNoAuth = "SimpleEnumPub";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + queryIdNoAuth),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals(2, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
    }

    @Test
    void protectedQueryWithPublicApiUnauthorized() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String queryProtected = "getApplicationParameters";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + queryProtected),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
      assertEquals(AnswerType.ERROR, Objects.requireNonNull(response.getBody()).getType());
    }

    @Test
    void unknownQueryWithPublicBarRequest() {
      // Build entity and variable maps
      HttpEntity<String> entity = new HttpEntity<>(headers);
      String unknownQuery = "unknownQuery";
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/public/data/" + unknownQuery),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
      assertEquals(AnswerType.ERROR, Objects.requireNonNull(response.getBody()).getType());
    }


    private void autenticateUser(HttpHeaders headers) {
      String jwtToken = JWT.create()
              .withSubject("test")
              .withExpiresAt(new Date(System.currentTimeMillis() + 60000)) // 1 min
              .withIssuer("AWE ISSUER")
              .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
      headers.add("Authorization", jwtTokenService.getPrefix() + " " + jwtToken);
    }
  }

  @Nested
  @DisplayName("API Maintain Tests")
  class ApiMaintainRestTest {

    // Constants
    private static final String TEST_INSERT = "testInsert";
    private static final String TEST_UPDATE = "testUpdate";
    private static final String TEST_UPDATE_PARAMETERS = "testUpdateParameters";
    private static final String TEST_DELETE = "testDelete";
    private static final String TEST_PUBLIC_MAINTAIN = "testPublicDelete";

    @Test
    void maintainAInsertNoAuth() {
      HttpEntity<String> entity = new HttpEntity<>(null, headers);
      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_INSERT),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
      assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
    }

    @Test
    void maintainAInsertAuth() {
      // Authenticate user
      autenticateUser(headers);
      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_INSERT),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    void maintainBUpdateAuth() {
      // Authenticate user
      autenticateUser(headers);
      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_UPDATE),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    void maintainCUpdateWithParametersAuth() {
      // Authenticate user
      autenticateUser(headers);

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

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    void maintainDeleteAuth() {
      // Authenticate user
      autenticateUser(headers);

      HttpEntity<String> entity = new HttpEntity<>(null, headers);

      ResponseEntity<AweRestResponse> response = restTemplate.exchange(
              createURLWithPort("/api/maintain/" + TEST_DELETE),
              HttpMethod.POST, entity, AweRestResponse.class);

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    // API Public maintain

    @Test
    void publicMaintain() {

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

      assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
      assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
    }

    private void autenticateUser(HttpHeaders headers) {
      String jwtToken = JWT.create()
              .withSubject("test")
              .withExpiresAt(new Date(System.currentTimeMillis() + 60000)) // 1 min
              .withIssuer("AWE ISSUER")
              .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
      headers.add("Authorization", jwtTokenService.getPrefix() + " " + jwtToken);
    }
  }
}
