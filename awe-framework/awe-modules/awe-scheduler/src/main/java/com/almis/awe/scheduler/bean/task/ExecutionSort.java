package com.almis.awe.scheduler.bean.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Sort of the execution list of all tasks (Activity screen): a column and a direction, as the grids send them in their
 * {@code sort} parameter. Only the columns of the list that can be sorted in the database are accepted
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionSort {
  /**
   * Column: ExeLstTim (start), TskNam (task), Sta or ExeStaTxt (status), ExeEla, ExePrg or ExeTim (elapsed time),
   * ExeLchBy (launched by), TypLch or ExeOriTxt (origin)
   */
  private String id;
  /**
   * Direction: asc or desc, in any case
   */
  private String direction;
}
