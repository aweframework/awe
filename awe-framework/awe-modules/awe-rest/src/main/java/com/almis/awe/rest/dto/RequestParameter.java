package com.almis.awe.rest.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
public class RequestParameter {

  @ApiModelProperty(value = "Parameters map in Json format.",
          notes = "Parameter values can be a list", allowEmptyValue = true,
          example = "{\"parName1\":\"value1\", \"parName2\":\"value2\", \"parName3\":[\"valueList1\",\"valueList2\", \"valueList3\"]}")
  private Map<String, Object> parameters;
}
