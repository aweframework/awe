package com.almis.awe.developer.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Literal {
  private String value;
  private String key;
  private String code;
}
