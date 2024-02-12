package com.almis.awe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScreenRestriction {
  private String option;
  private boolean restricted;
  private String user;
  private String profile;
  private String module;
}
