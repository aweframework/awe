package com.almis.awe.testing.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VideoRecorderStopRequest {
  private String id;
}
