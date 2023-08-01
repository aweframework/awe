package com.almis.awe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Favourite {
  private String option;
}
