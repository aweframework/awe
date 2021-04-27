package com.almis.awe.test.unit.rest;

import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.RequestParameter;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@FixMethodOrder (MethodSorters.NAME_ASCENDING)
public class MaintainRestControllerTest extends AweSpringRestTests {

  @LocalServerPort
  private int port;

  TestRestTemplate restTemplate = new TestRestTemplate();
  HttpHeaders headers = new HttpHeaders();

  // Constants
  private static final String TEST_INSERT = "testInsert";
  private static final String TEST_UPDATE = "testUpdate";
  private static final String TEST_UPDATE_PARAMETERS = "testUpdateParameters";
  private static final String TEST_DELETE = "testDelete";
  private static final String TEST_PUBLIC_MAINTAIN = "testPublicDelete";

  @Value("${security.master.key}")
  private String jwtSecret;

  /**
   * Creates an url with the local port
   *
   * @param uri URI
   *
   * @return URI
   */
  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }

  // Maintains test

  @Test
  public void maintainAInsertNoAuth() {
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/maintain/" + TEST_INSERT),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    Assert.assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
  }

  @Test
  public void maintainAInsertAuth() {
    // Authenticate user
    autenticateUser(headers);
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/maintain/" + TEST_INSERT),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
  }

  @Test
  public void maintainBUpdateAuth() {
    // Authenticate user
    autenticateUser(headers);
    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/maintain/" + TEST_UPDATE),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
  }

  @Test
  public void maintainCUpdateWithParametersAuth() {
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

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
  }

  @Test
  public void maintainDeleteAuth() {
    // Authenticate user
    autenticateUser(headers);

    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/maintain/" + TEST_DELETE),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
  }

  // API Public maintain

  @Test
  public void publicMaintain() {

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

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals("Operation successful", Objects.requireNonNull(response.getBody()).getTitle());
  }

  private void autenticateUser(HttpHeaders headers) {
    String jwtToken = JWT.create()
            .withSubject("test")
            .withExpiresAt(new Date(System.currentTimeMillis() + 60000)) // 1 min
            .withIssuer("AWE ISSUER")
            .sign(Algorithm.HMAC512(jwtSecret.getBytes()));
    headers.add("Authorization", jwtToken);
  }
}
