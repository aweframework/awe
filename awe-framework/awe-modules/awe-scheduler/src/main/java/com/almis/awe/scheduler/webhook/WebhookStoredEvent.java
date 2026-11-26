package com.almis.awe.scheduler.webhook;

import org.springframework.context.ApplicationEvent;

/**
 * A webhook was stored in the inbox and waits to be processed
 */
public class WebhookStoredEvent extends ApplicationEvent {

  /**
   * Constructor
   *
   * @param hookKey Key of the hook that received the webhook
   */
  public WebhookStoredEvent(String hookKey) {
    super(hookKey);
  }
}
