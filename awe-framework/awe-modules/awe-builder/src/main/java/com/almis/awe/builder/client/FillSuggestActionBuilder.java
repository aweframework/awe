package com.almis.awe.builder.client;

import com.almis.awe.builder.model.SuggestValue;
import com.almis.awe.model.entities.actions.ComponentAddress;

import java.util.List;

/**
 * Fill suggest action builder
 *
 * @author pgarcia
 * @deprecated the {@code fill-suggest} action duplicates {@code fill} and {@code select}. Fill the suggest with the
 * values (with their labels) using {@link FillActionBuilder} and select them with {@link SelectActionBuilder}:
 * <pre>{@code
 * DataList dataList = new DataList();
 * DataListUtil.addColumn(dataList, "value", List.of("aaa", "bbb"));
 * DataListUtil.addColumn(dataList, "label", List.of("Label aaa", "Label bbb"));
 * serviceData
 *   .addClientAction(new FillActionBuilder("my-suggest", dataList).build())
 *   .addClientAction(new SelectActionBuilder("my-suggest", "aaa", "bbb").build());
 * }</pre>
 * Use only {@link SelectActionBuilder} when the suggest can resolve the labels through its {@code check-target} or
 * {@code target-action}.
 */
@Deprecated(since = "5.0.0")
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
