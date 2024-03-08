package com.almis.awe.model;

import lombok.Data;

import java.util.List;

@Data
public class Planets {
  private String vacio;
  private List<String> otro;
  private List<String> nameList;
  private List<Long> populationList;
  private String solarSystem;
}
