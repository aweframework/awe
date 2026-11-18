package com.almis.awe.scheduler.webhook;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Hook of a WEBHOOK task (one row of AweSchTskHok), with the active flag of its task
 */
@Data
@Accessors(chain = true)
public class WebhookHook implements Serializable {
  private Integer taskId;
  private String hookKey;
  private Integer active;
  private String secretProperty;
  private String typeFilter;
  private Integer taskActive;

  /**
   * @return True when both the hook and its task are active
   */
  public boolean isEnabled() {
    return Integer.valueOf(1).equals(active) && Integer.valueOf(1).equals(taskActive);
  }
}
