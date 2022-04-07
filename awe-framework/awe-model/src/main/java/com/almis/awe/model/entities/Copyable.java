package com.almis.awe.model.entities;

import java.io.Serializable;

public interface Copyable extends Serializable {

  /**
   *  Get a copy of this element
   * @param <T> element type
   * @return element copy
   */
  <T> T copy();
}
