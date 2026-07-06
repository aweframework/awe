package com.almis.awe.scheduler.bean.task;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Operator supplied value for a variable (source = "1") task parameter on manual launch.
 *
 * <p>The field names ({@code name}, {@code value}) MUST match the modal grid column names.
 * The AWE JavaConnector binds a {@code bean-class} + {@code list="true"} service-parameter by
 * assembling the send-all grid's per-column arrays into a {@code List} of this bean, matching
 * request keys against these field names (see AbstractServiceConnector#extractParameter and
 * DataListUtil#getParameterBeanListValue).
 */
@Data
@Accessors(chain = true)
public class TaskVariable implements Serializable {
  private String name;
  private String value;
}
