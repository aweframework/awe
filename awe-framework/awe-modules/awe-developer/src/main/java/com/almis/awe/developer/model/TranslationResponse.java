package com.almis.awe.developer.model;

import lombok.Data;

@Data
public class TranslationResponse {
  private Translation responseData;
  private Integer responseStatus;
  private String responseDetails;
  private boolean quotaFinished;
}
