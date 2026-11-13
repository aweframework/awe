package com.almis.awe.builder.screen.criteria;

import com.almis.awe.builder.enumerates.Component;
import com.almis.awe.builder.screen.base.AbstractCriteriaBuilder;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;

/**
 * Builder of a date range criterion: a start and an end date with optional quick presets
 *
 * @author pgarcia
 */
public class DateRangeCriteriaBuilder extends AbstractCriteriaBuilder<DateRangeCriteriaBuilder, AbstractCriteria> {

  private String presets;

  @Override
  public AbstractCriteria build() {
    AbstractCriteria criterion = new Criteria().setPresets(presets);

    return (AbstractCriteria) super.build(criterion)
      .setComponentType(Component.DATE_RANGE.toString());
  }

  /**
   * Set the quick presets of the range (a list of codes separated by commas: TODAY, YTD, ALL, nD, nW, nM, nY)
   *
   * @param presets quick presets
   * @return This
   */
  public DateRangeCriteriaBuilder setPresets(String presets) {
    this.presets = presets;
    return this;
  }
}
