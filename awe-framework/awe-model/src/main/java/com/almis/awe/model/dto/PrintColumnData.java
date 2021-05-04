package com.almis.awe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PrintColumnData {

  // Column attributes
  String name;
  String label;
  String type;
  String component;
  String width;
  Integer charlength;
  String align;

  // Header attributes
  boolean header = false;
  List<PrintColumnData> columnList;
}
