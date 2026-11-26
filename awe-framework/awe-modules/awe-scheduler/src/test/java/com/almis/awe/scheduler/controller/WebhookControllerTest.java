package com.almis.awe.scheduler.controller;

import com.almis.awe.scheduler.webhook.WebhookHook;
import com.almis.awe.scheduler.webhook.WebhookInboxRepository;
import com.almis.awe.scheduler.webhook.WebhookSignatureVerifier;
import com.almis.awe.scheduler.webhook.WebhookStoredEvent;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Contract of the webhook endpoint that launches a task: the answers 202, 400, 401, 404, 409 and 413
 */
class WebhookControllerTest {

  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
  private static final String ID = "0193f2a4-6c1e-7b3a-9d2e-5f8a1c4e7b20";
  private static final String TIMESTAMP = "1763029530";
  private static final String BODY = "{\"type\":\"job.finished\",\"timestamp\":\"2025-11-13T10:25:30Z\","
    + "\"data\":{\"jobId\":\"0193f2a0-1111-7aaa-8bbb-000000000001\",\"status\":\"COMPLETED\"}}";

  private final WebhookInboxRepository repository = mock(WebhookInboxRepository.class);
  private final Environment environment = mock(Environment.class);
  private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
  private WebhookController controller;

  @BeforeEach
  void setUp() throws Exception {
    WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(
      Clock.fixed(Instant.ofEpochSecond(1763029530L), ZoneOffset.UTC), Duration.ofMinutes(5));
    controller = new WebhookController(repository, verifier, environment, publisher, 1024);
    given(repository.findHook("market")).willReturn(Optional.of(hook(1, 1)));
    given(environment.getProperty("hooks.market.secret")).willReturn(SECRET);
  }

  private static WebhookHook hook(int active, int taskActive) {
    return new WebhookHook().setTaskId(7).setHookKey("market").setActive(active).setTaskActive(taskActive)
      .setSecretProperty("hooks.market.secret");
  }

  private static String sign(String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(Base64.getDecoder().decode(SECRET.substring(6)), "HmacSHA256"));
    return "v1," + Base64.getEncoder().encodeToString(
      mac.doFinal((ID + "." + TIMESTAMP + "." + body).getBytes(StandardCharsets.UTF_8)));
  }

  private ResponseEntity<Map<String, Object>> post(String hookKey, String body, String signature) throws Exception {
    return post(hookKey, TIMESTAMP, body, signature);
  }

  private ResponseEntity<Map<String, Object>> post(String hookKey, String timestamp, String body, String signature)
    throws Exception {
    byte[] content = body.getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream bytes = new ByteArrayInputStream(content);
    HttpServletRequest request = mock(HttpServletRequest.class);
    given(request.getContentLengthLong()).willReturn((long) content.length);
    given(request.getInputStream()).willReturn(new ServletInputStream() {
      @Override
      public int read() {
        return bytes.read();
      }

      @Override
      public boolean isFinished() {
        return bytes.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
      }
    });
    return controller.receive(hookKey, ID, timestamp, signature, request);
  }

  private ResponseEntity<Map<String, Object>> postSigned(String body) throws Exception {
    return post("market", body, sign(body));
  }

  private void verifyNothingStored() throws Exception {
    verify(repository, never()).insertMessage(anyString(), anyString(), any(), anyString(), any());
  }

  @Test
  void aSignedWebhookIsStoredAndAnswered202() throws Exception {
    ResponseEntity<Map<String, Object>> response = postSigned(BODY);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    assertThat(response.getBody()).containsEntry("messageId", ID);
    verify(repository).insertMessage(eq("market"), eq(ID), eq("job.finished"), eq(BODY), any());
    verify(publisher).publishEvent(any(WebhookStoredEvent.class));
  }

  @Test
  void aDuplicateIsAnswered202AndNotStoredAgain() throws Exception {
    given(repository.messageExists("market", ID)).willReturn(true);

    ResponseEntity<Map<String, Object>> response = postSigned(BODY);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    assertThat(response.getBody()).containsEntry("messageId", ID);
    verifyNothingStored();
    verifyNoInteractions(publisher);
  }

  @Test
  void aDuplicateThatRacesTheInsertIsAnswered202() throws Exception {
    given(repository.messageExists("market", ID)).willReturn(false, true);
    doThrow(new IllegalStateException("duplicate key")).when(repository)
      .insertMessage(anyString(), anyString(), any(), anyString(), any());

    assertThat(postSigned(BODY).getStatusCode().value()).isEqualTo(202);
    verifyNoInteractions(publisher);
  }

  @Test
  void anInsertThatFailsForAnotherReasonIsAnError() throws Exception {
    doThrow(new IllegalStateException("database down")).when(repository)
      .insertMessage(anyString(), anyString(), any(), anyString(), any());

    assertThrows(IllegalStateException.class, () -> postSigned(BODY));
  }

  @Test
  void anUnknownHookAnswers404() throws Exception {
    assertThat(post("unknown", BODY, sign(BODY)).getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void anInvalidOrMissingSignatureAnswers401() throws Exception {
    assertThat(post("market", BODY, "v1,AAAA").getStatusCode().value()).isEqualTo(401);
    assertThat(post("market", BODY, null).getStatusCode().value()).isEqualTo(401);
    assertThat(post("market", BODY + " ", sign(BODY)).getStatusCode().value()).isEqualTo(401);
    verifyNothingStored();
  }

  @Test
  void aSecretThatDoesNotResolveAnswers401() throws Exception {
    given(environment.getProperty("hooks.market.secret")).willReturn(null);

    assertThat(postSigned(BODY).getStatusCode().value()).isEqualTo(401);
  }

  @Test
  void aTimestampOutOfToleranceAnswers401() throws Exception {
    assertThat(post("market", "1763030999", BODY, sign(BODY)).getStatusCode().value()).isEqualTo(401);
  }

  @Test
  void aHookOrTaskNotActiveAnswers409() throws Exception {
    given(repository.findHook("market")).willReturn(Optional.of(hook(0, 1)));
    assertThat(postSigned(BODY).getStatusCode().value()).isEqualTo(409);

    given(repository.findHook("market")).willReturn(Optional.of(hook(1, 0)));
    assertThat(postSigned(BODY).getStatusCode().value()).isEqualTo(409);
    verifyNothingStored();
  }

  @Test
  void aSignedBodyThatIsNotAnEnvelopeAnswers400() throws Exception {
    String[] bodies = {"not json", "[]", "{\"type\":\"x\"}", "{\"data\":[]}", "{\"data\":{},\"type\":5}"};
    for (String body : bodies) {
      ResponseEntity<Map<String, Object>> response = postSigned(body);

      assertThat(response.getStatusCode().value()).as(body).isEqualTo(400);
      assertThat(response.getBody()).as(body).containsEntry("type", "invalid-request");
    }
    verifyNothingStored();
  }

  @Test
  void aBodyWithoutTypeIsAccepted() throws Exception {
    assertThat(postSigned("{\"data\":{}}").getStatusCode().value()).isEqualTo(202);
    verify(repository).insertMessage(eq("market"), eq(ID), eq(null), eq("{\"data\":{}}"), any());
  }

  @Test
  void aBodyOverTheLimitAnswers413BeforeAnyCheck() throws Exception {
    String body = "{\"data\":{\"x\":\"" + "a".repeat(1100) + "\"}}";

    assertThat(postSigned(body).getStatusCode().value()).isEqualTo(413);
    verifyNothingStored();
  }
}
