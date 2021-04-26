package com.almis.awe.test.unit.rest;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.ErrorResponse;
import com.almis.awe.rest.dto.LoginResponse;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Objects;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataRestControllerTest extends AweSpringRestTests {

  @LocalServerPort
  private int port;

  TestRestTemplate restTemplate = new TestRestTemplate();
  HttpHeaders headers = new HttpHeaders();

  @Value("${security.master.key}")
  private String jwtSecret;

  private final String queryIdNoAuth = "SimpleEnumPub";
  private final String queryIdAuth = "SimpleEnum";
  private final String queryWithVariable = "QueryVariableInField";

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
            .queryParam("password", "xxxx");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(builder.toUriString(),
            HttpMethod.POST, entity, ErrorResponse.class);
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), Objects.requireNonNull(response.getBody()).getCode());
    Assert.assertEquals("Bad credentials", response.getBody().getMessage());
  }

  @Test
  public void authenticateUnauthorizedWithUserNotFound() {
    // Build entity and variable maps
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/api/authenticate"))
            .queryParam("username", "foo")
            .queryParam("password", "xxxx");
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<ErrorResponse> response = restTemplate.exchange(builder.toUriString(),
            HttpMethod.POST, entity, ErrorResponse.class);
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), Objects.requireNonNull(response.getBody()).getCode());
    Assert.assertEquals("User not found or not active", response.getBody().getMessage());
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
  public void queryWithAuthentication() throws Exception {
    ServiceData expected = new ObjectMapper().readValue("{\"valid\":true,\"type\":\"OK\",\"title\":\"\",\"message\":\"\",\"dataList\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]},\"clientActionList\":[],\"variableMap\":{\"MESSAGE_TITLE\":\"\",\"DATA\":{\"total\":1,\"page\":1,\"records\":2,\"rows\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]},\"MESSAGE_TYPE\":\"ok\",\"MESSAGE_DESCRIPTION\":\"\",\"ROWS\":[{\"id\":1,\"label\":\"ENUM_NO\",\"value\":\"0\"},{\"id\":2,\"label\":\"ENUM_YES\",\"value\":\"1\"}]},\"resultDetails\":[]}", ServiceData.class);

    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    ResponseEntity<ServiceData> response = restTemplate.exchange(
            createURLWithPort("/api/data/" + queryIdAuth),
            HttpMethod.POST, entity, ServiceData.class);


    Assert.assertEquals(new ObjectMapper().writeValueAsString(expected), new ObjectMapper().writeValueAsString(response.getBody()));
  }

  @Test
  public void queryWithVariable() throws Exception {
    ServiceData expected = new ObjectMapper().readValue("{\"valid\":true,\"type\":\"OK\",\"title\":\"\",\"message\":\"\",\"dataList\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"1\":1,\"IdeModPro\":62,\"id\":1},{\"1\":1,\"IdeModPro\":65,\"id\":2},{\"1\":1,\"IdeModPro\":74,\"id\":3},{\"1\":1,\"IdeModPro\":937,\"id\":4}]},\"clientActionList\":[],\"variableMap\":{\"MESSAGE_TITLE\":\"\",\"DATA\":{\"total\":1,\"page\":1,\"records\":4,\"rows\":[{\"1\":1,\"IdeModPro\":62,\"id\":1},{\"1\":1,\"IdeModPro\":65,\"id\":2},{\"1\":1,\"IdeModPro\":74,\"id\":3},{\"1\":1,\"IdeModPro\":937,\"id\":4}]},\"MESSAGE_TYPE\":\"ok\",\"MESSAGE_DESCRIPTION\":\"\",\"ROWS\":[{\"1\":1,\"IdeModPro\":62,\"id\":1},{\"1\":1,\"IdeModPro\":65,\"id\":2},{\"1\":1,\"IdeModPro\":74,\"id\":3},{\"1\":1,\"IdeModPro\":937,\"id\":4}]},\"resultDetails\":[]}", ServiceData.class);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.set("variable", JsonNodeFactory.instance.textNode("1"));

    HttpEntity<ObjectNode> entity = new HttpEntity<>(node, headers);

    ResponseEntity<ServiceData> response = restTemplate.exchange(
            createURLWithPort("/api/data/" + queryWithVariable),
            HttpMethod.POST, entity, ServiceData.class);


    Assert.assertEquals(new ObjectMapper().writeValueAsString(expected), new ObjectMapper().writeValueAsString(response.getBody()));
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
