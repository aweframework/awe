package com.almis.awe.builder.client;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.util.data.DataListUtil;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Fill action builder
 *
 * @author pgarcia
 */
public class FillActionBuilder extends ClientActionBuilder<FillActionBuilder> {

  private static final String TYPE = "fill";

  /**
   * Empty constructor
   */
  public FillActionBuilder() {
    setType(TYPE);
  }

  /**
   * Constructor with target and datalist
   *
   * @param target   Target
   * @param dataList Datalist
   */
  public FillActionBuilder(String target, DataList dataList) {
    setType(TYPE)
      .setTarget(target)
      .addParameter("datalist", fillDataListId(dataList));
  }

  /**
   * Constructor with address and datalist
   *
   * @param address  Target
   * @param dataList Datalist
   */
  public FillActionBuilder(ComponentAddress address, DataList dataList) {
    setType(TYPE)
      .setAddress(address)
      .addParameter("datalist", fillDataListId(dataList));
  }

  /**
   * Fill datalist ID if it's not defined
   * @param dataList DataList to fill id
   * @return Datalist with id filled in
   */
  private DataList fillDataListId(DataList dataList) {
    if (!DataListUtil.hasColumn(dataList, AweConstants.DATALIST_IDENTIFIER)) {
      DataListUtil.addColumn(dataList, AweConstants.DATALIST_IDENTIFIER, IntStream.range(1, dataList.getRows().size() + 1).boxed().collect(Collectors.toList()));
    }
    return dataList;
  }
}
