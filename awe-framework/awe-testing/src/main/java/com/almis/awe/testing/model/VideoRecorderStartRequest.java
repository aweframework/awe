package com.almis.awe.testing.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class VideoRecorderStartRequest {
  private String source;
  private String size;
  private Integer fps;
  private String pixelFormat;
  private String fileFormat;
  private List<String> extraInput;
  private List<String> extraOutput;
}
