package com.almis.awe.test.bean;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

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
  @DateTimeFormat(pattern = "dd/MM/yyyy")
  private Date created;
  private Date edited;
  private String url;
}
