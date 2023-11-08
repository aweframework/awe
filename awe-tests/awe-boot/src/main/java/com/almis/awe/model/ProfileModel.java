package com.almis.awe.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProfileModel {
  private String name;
  private Integer value;
}
