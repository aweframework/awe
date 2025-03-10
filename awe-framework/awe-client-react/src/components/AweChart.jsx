import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import Highcharts from 'highcharts/highstock';
import Highcharts3D from 'highcharts/highcharts-3d';
import HighchartsDrilldown from 'highcharts/modules/drilldown';
import HighchartsMore from 'highcharts/highcharts-more';
import HighchartsBoost from "highcharts/modules/boost.src";
import HighchartsNoData from "highcharts/modules/no-data-to-display.src";
import HighchartsExporting from "highcharts/modules/exporting.src";
import HighchartsReact from 'highcharts-react-official';
import HighchartsAccesibility from 'highcharts/modules/accessibility';
import {bindMethods} from "../utilities";
import _ from "lodash";

import {getHighchartsLocale} from "../i18n/highcharts";

import "./AweChart.less";

/**
 * List of magnitudes
 * @type {object[]}
 * @memberOf AweChart
 */
const MAGNITUDES = [{
  exp: 6,
  symbol: "M"
}, {
  exp: 3,
  symbol: "K"
}, {
  exp: 0,
  symbol: ""
}];

/**
 * Formatter methods
 * @type {object}
 * @memberOf AweChart
 */
const FORMATTERS = {
  /**
   * Format currency magnitude
   * @returns {String} formatted value
   */
  formatCurrencyMagnitude: function () {
    let value = this.value;
    let symbol = null;

    // Search for magnitudes and pick the biggest one
    MAGNITUDES.forEach(magnitude => {
      let factor = Math.pow(10, magnitude.exp);
      if (Math.abs(value) >= factor && symbol === null) {
        symbol = magnitude.symbol;
        value = Math.round(value * 100 / factor) / 100;
      }
    });
    return value + symbol;
  }
};

/**
 * Process chart options
 * @param {object} chartOptions chart options
 * @param {object} model Chart model
 * @param {function} t Translator
 * @param {object} settings Settings
 * @returns chartOptions with labels translated
 * @memberOf AweChart
 */
function processChartOptions(chartOptions, model, t, settings, onAnimationEnd, animating) {
  const {title, subtitle, legend, series, drilldown} = chartOptions;

  let fixedOptions = {
    backgroundColor: 'rgba(0, 0, 0, 0)',
    ...chartOptions,
    lang: getHighchartsLocale(settings.language)
  };
  // Chart title
  if (title) {
    fixedOptions.title.text = t(title.text);
  }

  // Chart subtitle
  if (subtitle) {
    fixedOptions.subtitle.text = t(subtitle.text);
  }

  // Chart legend
  if (legend && "title" in legend) {
    fixedOptions.legend.title.text = t(legend.title.text);
  }

  // Chart xAxis
  translateAxis(fixedOptions.xAxis, t);

  // Chart yAxis
  translateAxis(fixedOptions.yAxis, t);

  (series || []).forEach((serie, index) => {
    // Translate serie name
    serie.name && (fixedOptions.series[index].name = t(serie.name));
    serie.data = getSerieData(serie, model);
  });

  if (drilldown && drilldown.series) {
    (drilldown.series || []).forEach((serie, index) => {
      // Translate drilldown serie name
      serie.name && (fixedOptions.drilldown.series[index].name = t(serie.name));
      serie.data = getSerieData(serie, model);
    });

    // Disabled allow point selection in Pies
    if (fixedOptions.plotOptions.pie) {
      fixedOptions.plotOptions.pie.allowPointSelect = false;
    }

    // Animation end
    const plotOptions = (fixedOptions.plotOptions || {});
    if (animating) {
      fixedOptions.plotOptions = {...plotOptions, series: {...(plotOptions.series || {}), events: { afterAnimate: onAnimationEnd}}};
    } else {
      fixedOptions.plotOptions = {...plotOptions, series: {...(plotOptions.series || {}), events: { afterAnimate: () => null}}};
    }
  }

  return fixedOptions;
}

/**
 * Retrieve serie data
 * @param {object} serie Serie
 * @param {object} model Model
 * @memberOf AweChart
 */
function getSerieData(serie, model) {
  return model.map(row => ([
    row[serie.xValue],
    row[serie.yValue],
    ...(serie.zValue ? row[serie.zValue] : []),
    ...(serie.drilldown ? [serie.drilldown] : [])
  ]));
}

/**
 * Translate axis values
 * @param axis Axis
 * @param t Translator
 * @memberOf AweChart
 */
function translateAxis(axis, t) {
  (axis || []).forEach(item => {
    item.title && (item.title.text = t(item.title.text));
    item.labels && item.labels.formatter && (item.labels.formatter = FORMATTERS[item.labels.formatter])
  });
}

/**
 * AWE Chart component
 * @extends AweComponent
 * @category Components
 * @subcategory Chart
 */
class AweChart extends AweComponent {

  /**
   * Create a chart
   * @param {object} props Chart properties
   */
  constructor(props) {
    super(props);

    this.state = { animating: props.model.values.length > 0 };
    this.redraw = () => null;

    // Activate modules
    Highcharts3D(Highcharts);
    HighchartsDrilldown(Highcharts);
    HighchartsMore(Highcharts);
    HighchartsNoData(Highcharts);
    HighchartsBoost(Highcharts);
    HighchartsExporting(Highcharts);
    HighchartsAccesibility(Highcharts);

    // Bind methods
    bindMethods(this, ["afterChartCreated", "onAnimationEnd"]);

    // Set language
    Highcharts.setOptions({lang: getHighchartsLocale(props.settings.language)});
  }

  /**
   * Chart was created
   * @param {object} chart Chart
   */
  afterChartCreated(chart) {
    this.chart = chart;
    this.redraw = _.debounce(() => this.active && this.chart.reflow(), 50);
    this.active = true;
  }

  componentWillUnmount() {
    this.active = false;
    this.redraw = () => null;
  }

  /**
   * Chart animation finished
   */
  onAnimationEnd() {
    // Hack to avoid double animation on start
    setTimeout(() => this.setState({animating: false}), 500);
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    const {language} = this.props.settings;
    if (prevProps.settings.language !== language) {
      Highcharts.setOptions({
        lang: getHighchartsLocale(language),
      });
      this.redraw = () => null;
    } else {
      this.redraw();
    }
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {t, attributes, model, id, settings} = this.props;
    const {animating} = this.state;
    return <div className={"awe-chart expand highcharts-dark"} id={id}>
      <HighchartsReact
        key={settings.language}
        highcharts={Highcharts}
        options={processChartOptions(attributes.chartModel, model.values, t, settings, this.onAnimationEnd, animating)}
        callback={this.afterChartCreated}
        allowChartUpdate={ !animating }
        containerProps={{style: {position: "absolute", left: 0, top: 0, bottom: 0, right: 0}}}
      />
    </div>;
  }
}

export default connectComponent(AweChart);
