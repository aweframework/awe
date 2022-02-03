package com.almis.awe.developer.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TranslationResponse implements ITranslationResult {
  private Translation responseData;
  private Integer responseStatus;
  private String responseDetails;
  private boolean quotaFinished;
  private String remaining;

  public String getTranslation() {
    return responseData.getTranslatedText();
  }

  public String getRemaining() {
    return remaining;
  }
}
