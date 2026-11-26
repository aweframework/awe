package com.almis.awe.scheduler.webhook;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * A webhook of the inbox (one row of AweSchHokMsg) that waits to be processed
 */
@Data
@Accessors(chain = true)
public class WebhookMessage implements Serializable {
  private String hookKey;
  private String messageId;
  private String type;
  private String payload;
  private Integer attempts;
}
