import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import Highcharts from 'highcharts/highstock';
import Highcharts3D from 'highcharts/highcharts-3d';
import HighchartsDrilldown from 'highcharts/modules/drilldown';
import HighchartsMore from 'highcharts/highcharts-more';
import HighchartsBoost from "highcharts/modules/boost.src";
import HighchartsNoData from "highcharts/modules/no-data-to-display.src";
import HighchartsExporting from "highcharts/modules/exporting.src";
import HighchartsReact from 'highcharts-react-official';
import HighchartsAccesibility from 'highcharts/modules/accessibility';
import {translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import _ from "lodash";

import "./AweChart.less";
import {localeOptions} from "primereact/api";
import {produce} from "immer";
import {useSelector} from "react-redux";
import PropTypes from "prop-types";
import {classNames} from "../utilities/components";

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
function processChartOptions(chartOptions, model, t, settings) {
  return produce(chartOptions, draft => {
    const {title, subtitle, legend, series, drilldown} = draft;

    // Set initial attributes
    draft.backgroundColor = draft.backgroundColor || 'rgba(0, 0, 0, 0)';
    draft.lang = localeOptions(settings.language);

    // Chart title
    if (title) {
      draft.title.text = translateLabel(title.text, t);
    }

    // Chart subtitle
    if (subtitle) {
      draft.subtitle.text = translateLabel(subtitle.text, t);
    }

    // Chart legend
    if (legend && "title" in legend) {
      draft.legend.title.text = translateLabel(legend.title.text, t);
    }

    // Chart x axis
    if (draft.xAxis && draft.xAxis.length > 0) {
      draft.xAxis = translateAxis(draft.xAxis, t);
    }

    // Chart y axis
    if (draft.yAxis && draft.yAxis.length > 0) {
      draft.yAxis = translateAxis(draft.yAxis, t);
    }

    draft.series = (series || []).map(serie => ({
      ...serie,
      name: serie.name ? translateLabel(serie.name, t) : serie.name,
      data: [...getSerieData(serie, model)],
    }));

    if (drilldown && drilldown.series) {
      draft.drilldown = {
        ...draft.drilldown,
        series: drilldown.series.map(serie => ({
          ...serie,
          name: serie.name ? translateLabel(serie.name, t) : serie.name,
          data: [...getSerieData(serie, model)],
        }))
      };
    }

    // Disabled allow point selection in Pies
    if (draft.plotOptions?.pie) {
      draft.plotOptions.pie.allowPointSelect = false;
      draft.plotOptions.pie.animation = false;
    }
  });
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
 * @param axisArray Axis
 * @param t Translator
 * @memberOf AweChart
 */
function translateAxis(axisArray, t) {
  return (axisArray || []).map(axis => ({
    ...axis,
    title: axis.title?.text
      ? {...axis.title, text: translateLabel(axis.title.text, t)}
      : axis.title,
    labels: axis.labels?.formatter
      ? {...axis.labels, formatter: FORMATTERS[axis.labels.formatter]}
      : axis.labels,
  }));
}

/**
 * AWE Chart component (functional)
 * @category Components
 * @subcategory Chart
 */
// Synchronous one-time module initialization (before any render/mount)
let __AWE_CHART_MODULES_INITIALIZED__ = (typeof __AWE_CHART_MODULES_INITIALIZED__ !== 'undefined') ? __AWE_CHART_MODULES_INITIALIZED__ : false;
let __AWE_CHART_CURRENT_LANG__ = (typeof __AWE_CHART_CURRENT_LANG__ !== 'undefined') ? __AWE_CHART_CURRENT_LANG__ : null;
function ensureModulesInit() {
  if (!__AWE_CHART_MODULES_INITIALIZED__) {
    Highcharts3D(Highcharts);
    HighchartsDrilldown(Highcharts);
    HighchartsMore(Highcharts);
    HighchartsNoData(Highcharts);
    HighchartsBoost(Highcharts);
    HighchartsExporting(Highcharts);
    HighchartsAccesibility(Highcharts);
    __AWE_CHART_MODULES_INITIALIZED__ = true;
  }
}
function ensureLanguage(lang) {
  if (__AWE_CHART_CURRENT_LANG__ !== lang) {
    Highcharts.setOptions({ lang: localeOptions(lang) });
    __AWE_CHART_CURRENT_LANG__ = lang;
  }
}

function AweChart(props) {
  const { id } = props;
  const { settings, model = { values: [] }, attributes = {} } = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    settings: state.settings
  }));
  const [animating, setAnimating] = useState((model?.values || []).length > 0);
  const chartRef = useRef(null);
  const activeRef = useRef(false);
  const redrawRef = useRef(() => {});
  const { t, i18n } = useTranslation();

  // Make sure Highcharts modules are ready before first render
  ensureModulesInit();
  // Ensure initial language is applied synchronously too (before mount)
  ensureLanguage(i18n.language);

  // Update language when app language changes (runtime changes)
  useEffect(() => {
    ensureLanguage(i18n.language);
    // force redraw callback to be reset
    redrawRef.current = () => {};
  }, [i18n.language]);

  const afterChartCreated = useCallback((chart) => {
    chartRef.current = chart;
    redrawRef.current = _.debounce(() => activeRef.current && chartRef.current?.reflow(), 50);
    activeRef.current = true;

    chart.series.forEach(series => {
      Highcharts.addEvent(series, "afterAnimate", () => {
        onAnimationEnd();
      });
    });
  }, []);

  useEffect(() => {
    return () => {
      activeRef.current = false;
      redrawRef.current = () => {};
    };
  }, []);

  const onAnimationEnd = useCallback(() => {
    setTimeout(() => setAnimating(false), 500);
  }, []);

  // Redraw on updates
  useEffect(() => {
    redrawRef.current();
  });

  const chartOptions = JSON.parse(JSON.stringify(
    processChartOptions(attributes.chartModel, model.values, t, {...settings, language: i18n.language})
  ));

  const { style, visible } = attributes;
  const classes = classNames("awe-chart", "expand", "highcharts-dark", style, { "hidden": !visible });
  return <div className={classes} id={id}>
    <HighchartsReact
      key={i18n.language}
      highcharts={Highcharts}
      options={chartOptions}
      callback={afterChartCreated}
      allowChartUpdate={!animating}
      containerProps={{style: {position: "absolute", left: 0, top: 0, bottom: 0, right: 0}}}
    />
  </div>;
}

AweChart.propTypes = {
  id: PropTypes.string
};

export default AweChart;
