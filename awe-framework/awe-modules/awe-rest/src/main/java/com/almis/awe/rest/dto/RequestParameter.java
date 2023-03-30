package com.almis.awe.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class RequestParameter {

  @Schema(title = "Parameters map in Json format",
          description = "Parameter values can be a list",
          example = "{\"parName1\":\"value1\", \"parName2\":\"value2\", \"parName3\":[\"valueList1\",\"valueList2\", \"valueList3\"]}")
  private Map<String, Object> parameters;
}
