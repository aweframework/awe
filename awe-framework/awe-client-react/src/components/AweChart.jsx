import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {localeOptions} from "primereact/api";
import _ from "lodash";
import PropTypes from "prop-types";
import {useComponentState} from "../hooks/useComponentState";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";
import {registerChart, unregisterChart} from "../utilities/chartRegistry";
import {buildEChartsOption, findDrilldownId} from "../utilities/echartsOption";
import {linkedNames} from "../utilities/echartsLegend";
import {echarts, getEChartsLocale, isDarkTheme, renderSvg} from "../utilities/echartsSetup";

import "./AweChart.less";

const RESIZE_DELAY = 50;
const PLAIN_THEME = null;
const DARK_THEME = "dark";

/**
 * Create the ECharts instance of a container and track its drawing
 * @param {HTMLElement} element Element that holds the chart
 * @param {boolean} dark Use the dark theme
 * @param {string} language Language of the application
 * @returns {object|null} ECharts instance, null when it cannot be created
 */
function createChart(element, dark, language) {
  try {
    return echarts.init(element, dark ? DARK_THEME : PLAIN_THEME, {
      renderer: "svg",
      locale: getEChartsLocale(language)
    });
  } catch (error) {
    console.error("[ERROR] The chart could not be created", error);
    return null;
  }
}

/**
 * AWE Chart component: draws the `echartsModel` that the server sends with the values of the component using
 * Apache ECharts (SVG renderer).
 * @category Components
 * @subcategory Chart
 */
function AweChart(props) {
  const {id} = props;
  const {model = {values: []}, attributes = {}} = useComponentState(id);
  const {t, i18n} = useTranslation();
  const {echartsModel, style, visible} = attributes;
  const values = model.values;

  const containerRef = useRef(null);
  const chartElementRef = useRef(null);
  const chartRef = useRef(null);
  const optionRef = useRef(null);
  const [rendered, setRendered] = useState(false);
  const [size, setSize] = useState({width: 0, height: 0});
  const [drill, setDrill] = useState(null);
  const [dark, setDark] = useState(false);

  // The latest inputs, for the callbacks of the chart and the print image
  const latest = useRef({});
  const locale = useMemo(() => localeOptions(i18n.language) || {}, [i18n.language]);
  latest.current = {echartsModel, values, t, locale, language: i18n.language};

  // The drilldown is closed when the server sends another chart definition
  useEffect(() => {
    setDrill(null);
  }, [echartsModel]);

  const option = useMemo(() => {
    const context = {t, locale, width: size.width, height: size.height, drill, dark, onBack: () => setDrill(null)};
    return buildEChartsOption(echartsModel, values, context);
  }, [echartsModel, values, t, locale, size, drill, dark]);
  optionRef.current = option;

  const applyOption = useCallback(() => {
    try {
      chartRef.current?.setOption(optionRef.current, {notMerge: true});
    } catch (error) {
      console.error(`[ERROR] The chart '${id}' could not be drawn`, error);
    }
  }, [id]);

  // Create the chart (again when the language or the theme change: the texts of ECharts are set on creation)
  useEffect(() => {
    const element = chartElementRef.current;
    const darkTheme = isDarkTheme(containerRef.current);
    setDark(darkTheme);
    const chart = createChart(element, darkTheme, i18n.language);
    if (!chart) {
      return undefined;
    }
    chartRef.current = chart;
    const markRendered = () => setRendered(true);
    chart.on("rendered", markRendered);
    chart.on("finished", markRendered);
    chart.on("click", (params) => {
      const target = findDrilldownId(latest.current.echartsModel, params.seriesId);
      if (target) {
        setDrill({from: params.seriesId, to: target});
      }
    });
    // A series that is linked to another one hides and shows with it
    chart.on("legendselectchanged", ({name, selected}) => {
      const {echartsModel: current, t: translate} = latest.current;
      const type = selected?.[name] === false ? "legendUnSelect" : "legendSelect";
      linkedNames(current?.series || [], name, translate).forEach(linked => chart.dispatchAction({type, name: linked}));
    });
    // The image to print is drawn apart, in light colors and with the size of the page
    const handle = {
      getImage: (imageSize) => {
        const {echartsModel: current, values: data, t: translate, locale: texts, language} = latest.current;
        const printOption = buildEChartsOption(current, data, {...imageSize, t: translate, locale: texts, dark: false});
        return renderSvg(printOption, imageSize, language);
      }
    };
    registerChart(id, handle);
    applyOption();

    return () => {
      unregisterChart(id, handle);
      chartRef.current = null;
      chart.dispose();
    };
  }, [id, i18n.language, applyOption]);

  useEffect(() => {
    applyOption();
  }, [option, applyOption]);

  // Resize with the container
  useEffect(() => {
    const container = containerRef.current;
    const measure = () => {
      if (!container || container.clientWidth === 0 || container.clientHeight === 0) {
        return;
      }
      chartRef.current?.resize();
      setSize(current => current.width === container.clientWidth && current.height === container.clientHeight
        ? current
        : {width: container.clientWidth, height: container.clientHeight});
    };
    const resize = _.debounce(measure, RESIZE_DELAY);
    measure();
    let observer = null;
    if (typeof ResizeObserver !== "undefined" && container) {
      observer = new ResizeObserver(resize);
      observer.observe(container);
    }
    return () => {
      resize.cancel();
      observer?.disconnect();
    };
  }, [visible]);

  const classes = classNames("awe-chart", "expand", style, {"hidden": !visible});
  return <div className={classes} id={id} ref={containerRef}
    {...testHook(TestIds.chart, {rendered, attributes: {"chart-id": id}})}>
    <div ref={chartElementRef} style={{position: "absolute", left: 0, top: 0, bottom: 0, right: 0}}/>
  </div>;
}

AweChart.propTypes = {
  id: PropTypes.string
};

export default AweChart;
