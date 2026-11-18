package com.almis.awe.scheduler.webhook;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The hook that a task form defines: what the key and the secret property must be, that a key belongs to one task,
 * that the hook row follows the launch type, and the full URL the form shows
 */
class WebhookHookServiceTest {

  private final WebhookInboxRepository repository = mock(WebhookInboxRepository.class);
  private WebhookHookService service;

  @BeforeEach
  void setUp() {
    service = new WebhookHookService(repository);
    ApplicationContext context = mock(ApplicationContext.class);
    given(context.getBean(AweElements.class)).willReturn(mock(AweElements.class));
    service.setApplicationContext(context);
  }

  @AfterEach
  void clearRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void aKeyOfLowercaseLettersDigitsAndHyphensWithASecretPropertyIsAccepted() throws Exception {
    given(repository.findHook("market-2026")).willReturn(Optional.empty());

    service.validateWebhookHook(null, 4, "market-2026", "my.secret");
  }

  @Test
  void aKeyOutsideTheAllowedAlphabetOrLengthIsRejected() {
    for (String key : new String[]{"Market", "market_2026", "market 2026", "", null, "a".repeat(65)}) {
      assertThrows(AWException.class, () -> service.validateWebhookHook(null, 4, key, "my.secret"), String.valueOf(key));
    }
  }

  @Test
  void theSecretPropertyIsRequired() {
    assertThrows(AWException.class, () -> service.validateWebhookHook(null, 4, "market", " "));
  }

  @Test
  void aKeyOfAnotherTaskIsRejectedButTheOwnKeyIsNot() throws Exception {
    given(repository.findHook("market")).willReturn(Optional.of(new WebhookHook().setTaskId(7).setHookKey("market")));

    assertThrows(AWException.class, () -> service.validateWebhookHook(null, 4, "market", "my.secret"));
    assertThrows(AWException.class, () -> service.validateWebhookHook(8, 4, "market", "my.secret"));
    service.validateWebhookHook(7, 4, "market", "my.secret");
  }

  @Test
  void otherLaunchTypesAreNotValidatedAtAll() throws Exception {
    service.validateWebhookHook(null, 1, "Not A Key", null);

    verify(repository, never()).findHook(any());
  }

  @Test
  void aWebhookTaskSavesItsHookAndAnyOtherTaskLosesIt() throws Exception {
    service.saveWebhookHook(7, 4, "market", 1, "my.secret", "loaded");
    service.saveWebhookHook(8, 2, "market", 1, "my.secret", null);

    verify(repository).saveHook(new WebhookHook().setTaskId(7).setHookKey("market").setActive(1)
      .setSecretProperty("my.secret").setTypeFilter("loaded"));
    verify(repository).deleteHook(8);
  }

  @Test
  void theUrlIsTheContextPathOfTheRequestWithTheTriggerPathAndTheKey() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    given(request.getScheme()).willReturn("https");
    given(request.getServerName()).willReturn("awe.example.com");
    given(request.getServerPort()).willReturn(443);
    given(request.getContextPath()).willReturn("/awe");
    given(request.getRequestURI()).willReturn("/awe/action/data");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    ServiceData data = service.getWebhookUrl("market");

    assertEquals("https://awe.example.com/awe/scheduler/api/v1/webhooks/triggers/market",
      data.getDataList().getRows().get(0).get("value").getStringValue());
  }

  @Test
  void withoutAKeyTheUrlIsEmpty() {
    assertEquals("", service.getWebhookUrl(" ").getDataList().getRows().get(0).get("value").getStringValue());
  }
}
