package com.almis.awe.rest.dto;

import com.almis.awe.model.dto.DataList;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * AWE response Rest DTO class.
 * Map ServiceData objects {@link com.almis.awe.model.dto.ServiceData}
 */
@Data
@ApiModel("AWE rest response model")
public class AweRestResponse {
  private String type;
  private String title;
  private String message;
  @JsonInclude(NON_NULL)
  private DataList dataList;
}
