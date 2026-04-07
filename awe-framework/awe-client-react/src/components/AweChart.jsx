import React, { useCallback, useEffect, useMemo, useRef } from "react";
import Highcharts from 'highcharts/highstock';
import Highcharts3D from 'highcharts/highcharts-3d';
import HighchartsDrilldown from 'highcharts/modules/drilldown';
import HighchartsMore from 'highcharts/highcharts-more';
import HighchartsBoost from "highcharts/modules/boost.src";
import HighchartsNoData from "highcharts/modules/no-data-to-display.src";
import HighchartsExporting from "highcharts/modules/exporting.src";
import HighchartsReact from 'highcharts-react-official';
import HighchartsAccesibility from 'highcharts/modules/accessibility';
import { translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";

import "./AweChart.less";
import { localeOptions } from "primereact/api";
import { produce } from "immer";
import _ from "lodash";
import { useComponentState } from "../hooks/useComponentState";
import PropTypes from "prop-types";
import { classNames } from "../utilities/components";

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
export function processChartOptions(chartOptions, model, t, settings) {
  return produce(chartOptions, draft => {
    const { title, subtitle, legend, series, drilldown } = draft;

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
      ? { ...axis.title, text: translateLabel(axis.title.text, t) }
      : axis.title,
    labels: axis.labels?.formatter
      ? { ...axis.labels, formatter: FORMATTERS[axis.labels.formatter] }
      : axis.labels,
  }));
}

/**
 * AWE Chart component (functional)
 * @category Components
 * @subcategory Chart
 */
// Synchronous one-time module initialization — runs at module load time, never inside render.
let __AWE_CHART_MODULES_INITIALIZED__ = false;
let __AWE_CHART_CURRENT_LANG__ = null;
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
// Run one-time side effects at module evaluation time, not inside any render.
ensureModulesInit();

function removeScheduledReflowHandle(scheduledReflowsRef, handleId) {
  scheduledReflowsRef.current = scheduledReflowsRef.current.filter(handle => handle.id !== handleId);
}

function triggerScheduledReflow(scheduledReflowsRef, redrawRef, handleId) {
  removeScheduledReflowHandle(scheduledReflowsRef, handleId);
  redrawRef.current();
}

function scheduleSecondAnimationFrame(scheduledReflowsRef, redrawRef) {
  const secondFrame = requestAnimationFrame(() => {
    triggerScheduledReflow(scheduledReflowsRef, redrawRef, secondFrame);
  });

  scheduledReflowsRef.current = [...scheduledReflowsRef.current, { type: "frame", id: secondFrame }];
}

function scheduleAnimationFrameReflow(scheduledReflowsRef, redrawRef) {
  const firstFrame = requestAnimationFrame(() => {
    removeScheduledReflowHandle(scheduledReflowsRef, firstFrame);
    scheduleSecondAnimationFrame(scheduledReflowsRef, redrawRef);
  });

  scheduledReflowsRef.current = [{ type: "frame", id: firstFrame }];
}

function scheduleTimeoutReflow(scheduledReflowsRef, redrawRef) {
  const timeoutId = setTimeout(() => {
    triggerScheduledReflow(scheduledReflowsRef, redrawRef, timeoutId);
  }, 0);

  scheduledReflowsRef.current = [{ type: "timeout", id: timeoutId }];
}

function AweChart(props) {
  const { id } = props;
  const { model = { values: [] }, attributes = {} } = useComponentState(id);
  const containerRef = useRef(null);
  const chartRef = useRef(null);
  const activeRef = useRef(false);
  const redrawRef = useRef(() => { });
  const resizeObserverRef = useRef(null);
  const scheduledReflowsRef = useRef([]);
  const { t, i18n } = useTranslation();

  const cancelScheduledReflows = useCallback(() => {
    scheduledReflowsRef.current.forEach(handle => {
      if (handle.type === "frame" && typeof cancelAnimationFrame === "function") {
        cancelAnimationFrame(handle.id);
      }

      if (handle.type === "timeout") {
        clearTimeout(handle.id);
      }
    });

    scheduledReflowsRef.current = [];
  }, []);

  const schedulePostLayoutReflow = useCallback(() => {
    cancelScheduledReflows();

    if (typeof requestAnimationFrame === "function") {
      scheduleAnimationFrameReflow(scheduledReflowsRef, redrawRef);
      return;
    }

    scheduleTimeoutReflow(scheduledReflowsRef, redrawRef);
  }, [cancelScheduledReflows]);

  // Apply initial language once on mount, then track runtime language changes.
  // No explicit reflow call needed here: the key={i18n.language} prop on
  // HighchartsReact causes a full remount when the language changes, and the
  // unconstrained render effect below triggers a reflow after every committed
  // render (including the one following a language change).
  useEffect(() => {
    ensureLanguage(i18n.language);
  }, [i18n.language]);

  const afterChartCreated = useCallback((chart) => {
    chartRef.current = chart;
    redrawRef.current = _.debounce(() => activeRef.current && chartRef.current?.reflow(), 50);
    activeRef.current = true;
    schedulePostLayoutReflow();
  }, [schedulePostLayoutReflow]);

  useEffect(() => {
    const container = containerRef.current;

    if (!container || typeof ResizeObserver !== "function") {
      return;
    }

    let lastWidth = null;
    let lastHeight = null;

    const observer = new ResizeObserver(entries => {
      const entry = entries?.[0];
      if (!entry) {
        return;
      }

      const width = entry.contentRect?.width ?? container.offsetWidth;
      const height = entry.contentRect?.height ?? container.offsetHeight;

      if (width === lastWidth && height === lastHeight) {
        return;
      }

      lastWidth = width;
      lastHeight = height;
      redrawRef.current();
    });

    resizeObserverRef.current = observer;
    observer.observe(container);

    return () => {
      observer.disconnect();

      if (resizeObserverRef.current === observer) {
        resizeObserverRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    return () => {
      activeRef.current = false;
      resizeObserverRef.current?.disconnect();
      resizeObserverRef.current = null;
      cancelScheduledReflows();

      // Cancel any pending debounced reflow on unmount to prevent state updates
      // on an unmounted chart instance.
      if (typeof redrawRef.current.cancel === 'function') {
        redrawRef.current.cancel();
      }
      redrawRef.current = () => { };
    };
  }, [cancelScheduledReflows]);

  // Trigger a reflow after every committed render so the chart fills its
  // container correctly when the layout changes (e.g. resizable panels).
  useEffect(() => {
    redrawRef.current();
  });

  // Memoize chart options so HighchartsReact only sees a new object reference
  // when the underlying data, attributes, language, or translation actually change.
  const chartOptions = useMemo(() => JSON.parse(JSON.stringify(
    processChartOptions(attributes.chartModel, model.values, t, { language: i18n.language })
  )), [attributes.chartModel, model.values, t, i18n.language]);

  const { style, visible } = attributes;
  const classes = classNames("awe-chart", "expand", "highcharts-dark", style, { "hidden": !visible });
  return <div ref={containerRef} className={classes} id={id}>
    <HighchartsReact
      key={i18n.language}
      highcharts={Highcharts}
      options={chartOptions}
      callback={afterChartCreated}
      allowChartUpdate={true}
      containerProps={{ style: { position: "absolute", left: 0, top: 0, bottom: 0, right: 0 } }}
    />
  </div>;
}

AweChart.propTypes = {
  id: PropTypes.string
};

export default AweChart;
