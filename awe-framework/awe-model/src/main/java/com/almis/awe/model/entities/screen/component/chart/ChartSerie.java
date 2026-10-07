package com.almis.awe.model.entities.screen.component.chart;

import com.almis.awe.model.entities.screen.component.chart.echarts.EChartsModelBuilder;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChartSerie Class
 * <p>
 * Used to parse a chart Serie tag with XStream
 * Generates a Chart widget
 * </p>
 * @author Pablo VIDAL - 21/OCT/2014
 */
@Slf4j
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("chart-serie")
public class ChartSerie extends AbstractChart {

  private static final long serialVersionUID = -3249310197282122907L;

  // Color of serie
  @XStreamAlias("color")
  @XStreamAsAttribute
  private String color;

  // Index xAxis of serie
  @XStreamAlias("x-axis")
  @XStreamAsAttribute
  @JsonProperty("xAxis")
  private String xAxis;

  // Index yAxis of serie
  @XStreamAlias("y-axis")
  @XStreamAsAttribute
  @JsonProperty("yAxis")
  private String yAxis;

  // Point value of serie X
  @XStreamAlias("x-value")
  @XStreamAsAttribute
  @JsonProperty("xValue")
  private String xValue;

  // Point value of serie Y
  @XStreamAlias("y-value")
  @XStreamAsAttribute
  @JsonProperty("yValue")
  private String yValue;

  // Point value of serie Z
  @XStreamAlias("z-value")
  @XStreamAsAttribute
  @JsonProperty("zValue")
  private String zValue;

  // Id serie for drilldown
  @XStreamAlias("drilldown-serie")
  @XStreamAsAttribute
  @JsonProperty("drillDownSerie")
  private String drillDownSerie;

  // Flag if serie is type drilldown
  @XStreamAlias("drilldown")
  @XStreamAsAttribute
  @JsonProperty("drillDown")
  private Boolean drillDown;

  // Chart serie data
  @XStreamImplicit
  private transient List<ChartSeriePoint> data;

  /**
   * Retrieve the Apache ECharts translation of the series, without data.
   * <p>
   * It travels with the series of the chart actions (add, replace and remove series), beside the fields that the
   * AngularJS client reads. If the translation fails, a warning is logged and the key is omitted.
   * </p>
   *
   * @return ECharts series, empty when it could not be built
   */
  @JsonGetter("echarts")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public Map<String, Object> getEcharts() {
    try {
      return new EChartsModelBuilder().buildSeries(this);
    } catch (RuntimeException exc) {
      log.warn("The ECharts series '{}' could not be built and is omitted: {}", getId(), exc.toString());
      return Collections.emptyMap();
    }
  }

  @Override
  public ChartSerie copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .data(ListUtil.copyList(getData()))
      .build();
  }

  /**
   * Retrieve Json model node
   *
   * @return Model node
   */
  public Map<String, Object> getModel() {

    // Variable definition
    Map<String, Object> model = new HashMap<>();

    // Add id serie
    if (StringUtils.isNotBlank(getId())) {
      model.put(ChartConstants.ID, getId());
    }

    // Add name of serie
    if (StringUtils.isNotBlank(getLabel())) {
      model.put(ChartConstants.NAME, getLabel());
    }

    // Add type of serie
    if (StringUtils.isNotBlank(getType())) {
      model.put(ChartConstants.TYPE, getType());
    }

    // Add color of serie
    if (StringUtils.isNotBlank(getColor())) {
      model.put(ChartConstants.COLOR, getColor());
    }

    // Add index xAxix
    if (StringUtils.isNotBlank(getXAxis())) {
      model.put(ChartConstants.X_AXIS, Integer.valueOf(getXAxis()));
    }

    // Add index yAxix
    if (StringUtils.isNotBlank(getYAxis())) {
      model.put(ChartConstants.Y_AXIS, Integer.valueOf(getYAxis()));
    }

    // Add xValue field name
    if (StringUtils.isNotBlank(getXValue())) {
      model.put(ChartConstants.X_VALUE, getXValue());
    }
    // Add yValue field name
    if (StringUtils.isNotBlank(getYValue())) {
      model.put(ChartConstants.Y_VALUE, getYValue());
    }
    // Add zValue field name
    if (StringUtils.isNotBlank(getZValue())) {
      model.put(ChartConstants.Z_VALUE, getZValue());
    }

    // Add drilldown serie id
    if (StringUtils.isNotBlank(getDrillDownSerie())) {
      model.put(ChartConstants.DRILL_DOWN, getDrillDownSerie());
    }

    // Add data to serie id
    if (getData() != null) {
      model.put(ChartConstants.DATA, getData());
    }

    // Add extra parameters
    addParameters(model);

    return model;
  }
}
