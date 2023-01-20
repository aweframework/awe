package com.almis.awe.builder.client;

import com.almis.awe.builder.model.SuggestValue;
import com.almis.awe.model.entities.actions.ComponentAddress;

import java.util.List;

/**
 * Fill suggest action builder
 *
 * @author pgarcia
 */
public class FillSuggestActionBuilder extends ClientActionBuilder<FillSuggestActionBuilder> {

  private static final String TYPE = "fill-suggest";
  private static final String VALUES = "values";

  /**
   * Empty constructor
   */
  public FillSuggestActionBuilder() {
    setType(TYPE);
  }

  /**
   * Constructor with target and value list
   *
   * @param target Target
   * @param values Value list
   */
  public FillSuggestActionBuilder(String target, List<SuggestValue> values) {
    setType(TYPE)
      .setTarget(target)
      .addParameter(VALUES, values);
  }

  /**
   * Constructor with target and value list
   *
   * @param target Target
   * @param values Value list
   */
  public FillSuggestActionBuilder(String target, SuggestValue... values) {
    setType(TYPE)
      .setTarget(target)
      .addParameter(VALUES, values);
  }

  /**
   * Constructor with address and value list
   *
   * @param address Target
   * @param values  Value list
   */
  public FillSuggestActionBuilder(ComponentAddress address, List<SuggestValue> values) {
    setType(TYPE)
      .setAddress(address)
      .addParameter(VALUES, values);
  }

  /**
   * Constructor with address and list values
   *
   * @param address Target
   * @param values  Value list
   */
  public FillSuggestActionBuilder(ComponentAddress address, SuggestValue... values) {
    setType(TYPE)
      .setAddress(address)
      .addParameter(VALUES, values);
  }
}
