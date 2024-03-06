package com.almis.awe.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Planet {

  private String name;
  private String rotationPeriod;
  private String orbitalPeriod;
  private String diameter;
  private String climate;
  private String gravity;
  private String terrain;
  private String surfaceWater;
  private Long population;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", timezone = "CET")
  private Date created;
  private Date edited;
  private String url;
}
