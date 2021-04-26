package com.almis.awe.rest.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AWE rest error response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "AWE Rest error response model")
public class ErrorResponse {
  @ApiModelProperty(value = "Error code")
  private int code;
  @ApiModelProperty(value = "Error message")
  private String message;
}
