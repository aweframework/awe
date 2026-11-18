package com.almis.awe.scheduler.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.webhook.WebhookHook;
import com.almis.awe.scheduler.webhook.WebhookInboxRepository;
import com.almis.awe.scheduler.webhook.WebhookSignatureVerifier;
import com.almis.awe.scheduler.webhook.WebhookStoredEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Receives the webhooks that launch tasks. The signature of the message is the authentication, so this path is not
 * behind the bearer of the scheduler api. The message is stored in the inbox and answered 202; the launch happens
 * later, outside the request.
 */
@Slf4j
@RestController
@RequestMapping("/scheduler/api/v1/webhooks")
public class WebhookController {

  private static final int MAX_MESSAGE_ID_LENGTH = 256;
  private static final int MAX_TYPE_LENGTH = 200;
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final WebhookInboxRepository repository;
  private final WebhookSignatureVerifier verifier;
  private final Environment environment;
  private final ApplicationEventPublisher eventPublisher;
  private final long maxBodyBytes;

  /**
   * Constructor
   *
   * @param repository     Inbox and hooks
   * @param verifier       Signature verifier
   * @param environment    Environment that resolves the property that holds the secret of a hook
   * @param eventPublisher Publisher of the event that tells a message was stored
   * @param maxBodyBytes   Maximum size of the body
   */
  public WebhookController(WebhookInboxRepository repository, WebhookSignatureVerifier verifier,
                           Environment environment, ApplicationEventPublisher eventPublisher, long maxBodyBytes) {
    this.repository = repository;
    this.verifier = verifier;
    this.environment = environment;
    this.eventPublisher = eventPublisher;
    this.maxBodyBytes = maxBodyBytes;
  }

  /**
   * Receive a webhook that launches the task of a hook
   *
   * @param hookKey   Key of the hook
   * @param id        Value of the webhook-id header
   * @param timestamp Value of the webhook-timestamp header
   * @param signature Value of the webhook-signature header
   * @param request   Request, whose raw body is the signed content
   * @return 202 with the message id (also for a duplicate); 404 when the hook does not exist; 413 when the body is
   * too big; 401 when the signature or the timestamp are not valid; 409 when the hook or its task are not active; 400
   * when the body is not a message with a data object
   * @throws AWException Error storing the message
   * @throws IOException Error reading the body
   */
  @PostMapping("/triggers/{hookKey}")
  ResponseEntity<Map<String, Object>> receive(@PathVariable String hookKey,
                                              @RequestHeader(name = "webhook-id", required = false) String id,
                                              @RequestHeader(name = "webhook-timestamp", required = false)
                                              String timestamp,
                                              @RequestHeader(name = "webhook-signature", required = false)
                                              String signature,
                                              HttpServletRequest request) throws AWException, IOException {
    WebhookHook hook = repository.findHook(hookKey).orElse(null);
    if (hook == null) {
      return ResponseEntity.notFound().build();
    }

    byte[] body = readBody(request);
    if (body == null) {
      return error(413, "payload-too-large", "the body must have " + maxBodyBytes + " bytes at most");
    }

    String secrets = hook.getSecretProperty() == null ? null : environment.getProperty(hook.getSecretProperty());
    if (secrets == null) {
      log.warn("[SCHEDULER][WEBHOOK {}] The secret property of the hook does not resolve", hookKey);
    }
    if (!verifier.verify(secrets, id, timestamp, signature, body)) {
      log.warn("[SCHEDULER][WEBHOOK {}] Rejected: signature or timestamp not valid", hookKey);
      return error(401, "invalid-signature", "the signature or the timestamp are not valid");
    }

    if (!hook.isEnabled()) {
      return error(409, "hook-not-active", "the hook or its task are not active");
    }

    String payload = new String(body, StandardCharsets.UTF_8);
    String type = validEnvelopeType(payload);
    if (type == null || id.length() > MAX_MESSAGE_ID_LENGTH) {
      return error(400, "invalid-request", "the body must be a JSON object with a data object, and an optional "
        + "type of " + MAX_TYPE_LENGTH + " characters at most");
    }

    store(hookKey, id, type.isEmpty() ? null : type, payload);
    return ResponseEntity.accepted().body(Map.of("messageId", id));
  }

  /**
   * Read the body without going over the limit
   *
   * @param request Request
   * @return The body, or null when it is bigger than the limit
   */
  private byte[] readBody(HttpServletRequest request) throws IOException {
    if (request.getContentLengthLong() > maxBodyBytes) {
      return null;
    }
    byte[] body = request.getInputStream().readNBytes((int) maxBodyBytes + 1);
    return body.length > maxBodyBytes ? null : body;
  }

  /**
   * Validate the message envelope
   *
   * @param payload Body
   * @return The type of the message (empty when it has none), or null when the body is not valid
   */
  private String validEnvelopeType(String payload) {
    try {
      JsonNode root = MAPPER.readTree(payload);
      JsonNode data = root.get("data");
      JsonNode type = root.get("type");
      boolean hasType = type != null && !type.isNull();
      if (!root.isObject() || data == null || !data.isObject() || hasType && !type.isString()
        || hasType && type.asString().length() > MAX_TYPE_LENGTH) {
        return null;
      }
      return hasType ? type.asString() : "";
    } catch (JacksonException exc) {
      return null;
    }
  }

  /**
   * Store the message unless it is already in the inbox (the same webhook-id sent again)
   */
  private void store(String hookKey, String id, String type, String payload) throws AWException {
    if (repository.messageExists(hookKey, id)) {
      log.debug("[SCHEDULER][WEBHOOK {}] Duplicate message ignored", hookKey);
      return;
    }
    try {
      repository.insertMessage(hookKey, id, type, payload, Instant.now());
    } catch (AWException | RuntimeException exc) {
      // Another request may have stored it in the meantime: the key is unique
      if (repository.messageExists(hookKey, id)) {
        log.debug("[SCHEDULER][WEBHOOK {}] Duplicate message ignored", hookKey);
        return;
      }
      throw exc;
    }
    log.info("[SCHEDULER][WEBHOOK {}] Message received", hookKey);
    eventPublisher.publishEvent(new WebhookStoredEvent(hookKey));
  }

  private ResponseEntity<Map<String, Object>> error(int status, String type, String detail) {
    return ResponseEntity.status(status).body(Map.of("type", type, "detail", detail));
  }
}
