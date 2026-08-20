package com.almis.awe.scheduler.feign;

import feign.Client;
import feign.Feign;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Request-body contract of {@link RemoteScheduler#executeTaskNow}.
 *
 * <p>The request template is resolved against a stub client, so no scheduler is needed. Mocking
 * {@link RemoteScheduler} would not exercise any of this: template resolution happens below the mock.
 */
class RemoteSchedulerContractTest {

  private final AtomicReference<Request> lastRequest = new AtomicReference<>();

  /**
   * Interface declaring an explicitly optional request body.
   */
  interface OptionalBodyProbe {

    @PostMapping("/probe")
    String post(@RequestBody(required = false) Map<String, String> body);
  }

  /**
   * Build a Feign proxy over the given interface, capturing the request that reaches the client.
   *
   * @param api Interface to proxy
   * @param <T> Interface type
   * @return Feign backed proxy
   */
  private <T> T client(Class<T> api) {
    Client client = (request, options) -> {
      lastRequest.set(request);
      return Response.builder()
        .status(200)
        .reason("OK")
        .request(request)
        .headers(Collections.emptyMap())
        .body(new byte[0])
        .build();
    };
    Encoder encoder = (object, bodyType, template) -> template.body(String.valueOf(object));
    Decoder decoder = (response, type) -> null;

    return Feign.builder()
      .contract(new SpringMvcContract())
      .encoder(encoder)
      .decoder(decoder)
      .client(client)
      .target(api, "http://localhost:9999/scheduler/api/v1");
  }

  /**
   * A null variables map is rejected before the request is built, so the launch never reaches the
   * remote scheduler.
   */
  @Test
  void nullVariablesMapIsRejectedBeforeTheRequestIsBuilt() {
    RemoteScheduler remoteScheduler = client(RemoteScheduler.class);

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
      () -> remoteScheduler.executeTaskNow(1, "user", null));

    assertTrue(thrown.getMessage().contains("Body parameter 2"),
      "Expected the null body rejection, got: " + thrown.getMessage());
    assertNull(lastRequest.get(), "The request must never reach the client");
  }

  /**
   * SpringMvcContract ignores {@code required = false}: an optional body is still rejected when
   * null, so callers cannot delegate the null check to the annotation.
   */
  @Test
  void optionalRequestBodyAnnotationIsNotHonored() {
    OptionalBodyProbe probe = client(OptionalBodyProbe.class);

    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
      () -> probe.post(null));

    assertTrue(thrown.getMessage().contains("Body parameter 0"),
      "Expected the null body rejection despite required = false, got: " + thrown.getMessage());
    assertNull(lastRequest.get(), "The request must never reach the client");
  }

  /**
   * An empty variables map builds the request and reaches the client.
   */
  @Test
  void emptyVariablesMapBuildsTheRequest() {
    client(RemoteScheduler.class).executeTaskNow(1, "user", Collections.emptyMap());

    Request request = lastRequest.get();
    assertNotNull(request, "The request must reach the client");
    assertEquals(Request.HttpMethod.POST, request.httpMethod());
    assertTrue(request.url().contains("/task/1/execute"), "Unexpected url: " + request.url());
    assertTrue(request.url().contains("user=user"), "Unexpected url: " + request.url());
    assertNotNull(request.body());
  }

  /**
   * Operator supplied values are carried in the request body.
   */
  @Test
  void operatorValuesAreCarriedInTheRequestBody() {
    client(RemoteScheduler.class).executeTaskNow(1, "user", Map.of("date", "2026-01-31"));

    Request request = lastRequest.get();
    assertNotNull(request);
    assertTrue(new String(request.body()).contains("date=2026-01-31"),
      "Unexpected body: " + new String(request.body()));
  }
}
