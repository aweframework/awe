package com.almis.awe.model.component;

import lombok.Data;
import tools.jackson.databind.node.ObjectNode;

@Data
public class RequestDataHolder {
  private ObjectNode requestData;
}
