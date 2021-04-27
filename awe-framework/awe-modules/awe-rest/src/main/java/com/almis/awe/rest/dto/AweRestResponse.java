package com.almis.awe.rest.dto;

import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.type.AnswerType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * AWE response Rest DTO class.
 * Map ServiceData objects {@link com.almis.awe.model.dto.ServiceData}
 */
@Data
@NoArgsConstructor
@ApiModel("AWE rest response model")
public class AweRestResponse {
  private AnswerType type;
  private String title;
  private String message;
  @JsonInclude(NON_NULL)
  private DataList dataList;
  @JsonInclude(NON_NULL)
  private List<MaintainResultDetails> resultDetails;
}
