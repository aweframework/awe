package com.almis.awe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EmailServer {
  private String name;
  private String host;

  private boolean authenticated;
  private String user;
  private String pass;
}
