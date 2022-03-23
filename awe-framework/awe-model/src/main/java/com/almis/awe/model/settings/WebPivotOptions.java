package com.almis.awe.model.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Web pivot options options
 * 
 * @author pgarcia
 */
@Getter
@Setter
@Accessors(chain = true)
public class WebPivotOptions {

  /**
   * Number of group element limit of pivot table component
   */
  private Integer numGroup;
}
