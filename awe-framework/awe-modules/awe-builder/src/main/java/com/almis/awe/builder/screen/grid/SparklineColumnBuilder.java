package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.Component;
import com.almis.awe.builder.enumerates.SparklineType;
import com.almis.awe.builder.screen.base.AbstractColumnBuilder;
import com.almis.awe.model.entities.screen.component.grid.Column;

/**
 * Sparkline column: draws the series of numbers of the cell (a JSON array such as [3, 5, -1, 8]) as a small line, bar
 * or area chart. Every option is optional: the clients apply their defaults to the ones that are not set
 *
 * @author pgarcia
 */
public class SparklineColumnBuilder extends AbstractColumnBuilder<SparklineColumnBuilder, Column> {

  private SparklineType sparklineType;
  private String sparklineColor;
  private String sparklineNegativeColor;
  private Boolean sparklineLastPoint;
  private Boolean sparklineMinMax;

  /**
   * Set the drawing type
   *
   * @param sparklineType line, bar or area
   * @return This
   */
  public SparklineColumnBuilder setSparklineType(SparklineType sparklineType) {
    this.sparklineType = sparklineType;
    return this;
  }

  /**
   * Set the colour of the series
   *
   * @param sparklineColor CSS colour
   * @return This
   */
  public SparklineColumnBuilder setSparklineColor(String sparklineColor) {
    this.sparklineColor = sparklineColor;
    return this;
  }

  /**
   * Set the colour of the negative values
   *
   * @param sparklineNegativeColor CSS colour
   * @return This
   */
  public SparklineColumnBuilder setSparklineNegativeColor(String sparklineNegativeColor) {
    this.sparklineNegativeColor = sparklineNegativeColor;
    return this;
  }

  /**
   * Highlight the last point of the series
   *
   * @param sparklineLastPoint Highlight the last point
   * @return This
   */
  public SparklineColumnBuilder setSparklineLastPoint(boolean sparklineLastPoint) {
    this.sparklineLastPoint = sparklineLastPoint;
    return this;
  }

  /**
   * Mark the minimum and maximum points of the series
   *
   * @param sparklineMinMax Mark the minimum and maximum
   * @return This
   */
  public SparklineColumnBuilder setSparklineMinMax(boolean sparklineMinMax) {
    this.sparklineMinMax = sparklineMinMax;
    return this;
  }

  @Override
  public Column build() {
    Column column = (Column) super.build(new Column())
      .setComponentType(Component.SPARKLINE.toString());
    return column
      .setSparklineType(sparklineType == null ? null : sparklineType.toString())
      .setSparklineColor(sparklineColor)
      .setSparklineNegativeColor(sparklineNegativeColor)
      .setSparklineLastPoint(sparklineLastPoint)
      .setSparklineMinMax(sparklineMinMax);
  }
}
