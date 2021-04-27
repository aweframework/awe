package com.almis.awe.test.unit.rest;

import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.LoginResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataRestControllerTest extends AweSpringRestTests {

  @LocalServerPort
  private int port;

  TestRestTemplate restTemplate = new TestRestTemplate();
  HttpHeaders headers = new HttpHeaders();

  @Value("${security.master.key}")
  private String jwtSecret;

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

  // Authenticate
  @Test
  public void authenticateSuccess() {
    // Build entity and variable maps
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
            .queryParam("username", "test")
            .queryParam("password", "test");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<LoginResponse> response = restTemplate.exchange(builder.toUriString(),
            HttpMethod.POST, entity, LoginResponse.class);
    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertNotNull(Objects.requireNonNull(response.getBody()).getToken());
  }

  @Test
  public void authenticateUnauthorizedWithBadCredentials() {
    // Build entity and variable maps
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
            .queryParam("username", "test")
            .queryParam("password", "dummy");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
            HttpMethod.POST, entity, AweRestResponse.class);
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    Assert.assertEquals("Bad credentials", Objects.requireNonNull(response.getBody()).getMessage());
  }

  @Test
  public void authenticateUnauthorizedWithUserNotFound() {
    // Build entity and variable maps
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
            .queryParam("username", "foo")
            .queryParam("password", "dummy");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(builder.toUriString(),
            HttpMethod.POST, entity, AweRestResponse.class);
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    Assert.assertEquals("User not found or not active", Objects.requireNonNull(response.getBody()).getMessage());
  }

  // Queries

  @Test
  public void protectedQueryUnauthorized() {
    // Build entity and variable maps
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/data/" + queryIdAuth),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    Assert.assertEquals("Not authorized. Token is not valid or not found", Objects.requireNonNull(response.getBody()).getMessage());
  }

  @Test
  public void protectedQueryAuthorized() {
    //Authenticate users
    autenticateUser(headers);

    // Build entity and variable maps
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/data/" + queryIdAuth),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals(2,  Objects.requireNonNull(response.getBody()).getDataList().getRecords());
  }

  @Test
  public void protectedQueryWithParametersAuthorized() {
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

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals(4, Objects.requireNonNull(response.getBody()).getDataList().getRecords());
  }

  @Test
  public void publicQuerySuccess() {
    // Build entity and variable maps
    HttpEntity<String> entity = new HttpEntity<>(headers);
    String queryIdNoAuth = "SimpleEnumPub";
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/public/data/" + queryIdNoAuth),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    Assert.assertEquals(2,  Objects.requireNonNull(response.getBody()).getDataList().getRecords());
  }

  @Test
  public void protectedQueryWithPublicApiUnauthorized() {
    // Build entity and variable maps
    HttpEntity<String> entity = new HttpEntity<>(headers);
    String queryProtected = "getApplicationParameters";
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/public/data/" + queryProtected),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCodeValue());
    Assert.assertEquals(AnswerType.ERROR,  Objects.requireNonNull(response.getBody()).getType());
  }

  @Test
  public void unknownQueryWithPublicBarRequest() {
    // Build entity and variable maps
    HttpEntity<String> entity = new HttpEntity<>(headers);
    String unknownQuery = "unknownQuery";
    ResponseEntity<AweRestResponse> response = restTemplate.exchange(
            createURLWithPort("/api/public/data/" + unknownQuery),
            HttpMethod.POST, entity, AweRestResponse.class);

    Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
    Assert.assertEquals(AnswerType.ERROR,  Objects.requireNonNull(response.getBody()).getType());
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
