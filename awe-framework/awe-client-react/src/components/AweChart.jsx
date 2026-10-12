import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {localeOptions} from "primereact/api";
import _ from "lodash";
import PropTypes from "prop-types";
import {useComponentState} from "../hooks/useComponentState";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";
import {updateAttributesWithDependencies} from "../redux/thunks/components";
import {registerChart, unregisterChart} from "../utilities/chartRegistry";
import {buildEChartsOption, findDrilldownId} from "../utilities/echartsOption";
import {linkedNames} from "../utilities/echartsLegend";
import {parseChartOptions, pointOf, zoomOf, zoomRequests} from "../utilities/echartsEvents";
import {echarts, getEChartsLocale, isDarkTheme, renderSvg} from "../utilities/echartsSetup";

import "./AweChart.less";

const RESIZE_DELAY = 50;
const PLAIN_THEME = null;
const DARK_THEME = "dark";
const AXES = ["x", "y"];

/**
 * Keep the previous value while the new one is equal: the state of a component is copied when an attribute changes
 * (the events of the chart write attributes), and the chart must not be drawn again (it would lose its zoom) for it
 * @param {*} value Value
 * @returns {*} The previous value when it equals the new one
 */
function useStable(value) {
  const ref = useRef(value);
  if (!_.isEqual(ref.current, value)) {
    ref.current = value;
  }
  return ref.current;
}

/**
 * Select the axes of a zoom whose range differs from the range the chart attributes tell
 * @param {object} zoom Zoom of the chart options: {x: {min, max}, y: {min, max}}
 * @param {object} attributes Chart attributes (xMin, xMax, yMin, yMax)
 * @returns {object} Zoom with the axes that change
 */
function changedZoom(zoom, attributes) {
  const changed = {};
  AXES.filter(axis => zoom[axis]).forEach(axis => {
    const {min = null, max = null} = zoom[axis];
    if (min !== (attributes[`${axis}Min`] ?? null) || max !== (attributes[`${axis}Max`] ?? null)) {
      changed[axis] = {min, max};
    }
  });
  return changed;
}

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
  const {model = {values: []}, attributes = {}, address} = useComponentState(id);
  const dispatch = useDispatch();
  const {t, i18n} = useTranslation();
  const {style, visible, chartOptions, xMin, xMax, yMin, yMax} = attributes;
  const echartsModel = useStable(attributes.echartsModel);
  const values = useStable(model.values);

  const containerRef = useRef(null);
  const chartElementRef = useRef(null);
  const chartRef = useRef(null);
  const optionRef = useRef(null);
  const [rendered, setRendered] = useState(false);
  const [size, setSize] = useState({width: 0, height: 0});
  const [drill, setDrill] = useState(null);
  const [dark, setDark] = useState(false);
  // True while the chart is zoomed by the chart options: its own zoom event must not be sent as a user zoom
  const zooming = useRef(false);

  // The latest inputs, for the callbacks of the chart and the print image
  const latest = useRef({});
  const locale = useMemo(() => localeOptions(i18n.language) || {}, [i18n.language]);
  latest.current = {echartsModel, values, t, locale, language: i18n.language, address, attributes};

  // Write the attributes of the chart (x, y, xMin...) and raise the event, which launches the dependencies
  const emit = useCallback((data) => dispatch(updateAttributesWithDependencies(latest.current.address, data)), [dispatch]);

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
      emit({...pointOf(params), event: "click"});
    });
    chart.on("dblclick", (params) => emit({...pointOf(params), event: "double-click"}));
    chart.on("contextmenu", (params) => emit({...pointOf(params), event: "context-menu"}));
    chart.on("datazoom", () => {
      if (!zooming.current) {
        emit({...zoomOf(chart), event: "zoom"});
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
  }, [id, i18n.language, applyOption, emit]);

  useEffect(() => {
    applyOption();
  }, [option, applyOption]);

  // The chart options sent by a dependency: the zoom is applied to the chart (linked charts), the rest is ignored
  useEffect(() => {
    const chart = chartRef.current;
    const {zoom} = parseChartOptions(chartOptions);
    const changed = chart && zoom ? changedZoom(zoom, latest.current.attributes) : {};
    const requests = chart ? zoomRequests(chart, changed) : [];
    if (requests.length === 0) {
      return;
    }
    zooming.current = true;
    try {
      requests.forEach(request => chart.dispatchAction(request));
    } finally {
      zooming.current = false;
    }
    emit(Object.fromEntries(Object.entries(changed).flatMap(([axis, {min, max}]) => [[`${axis}Min`, min], [`${axis}Max`, max]])));
  }, [chartOptions, emit]);

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

  const zoomed = [xMin, xMax, yMin, yMax].some(limit => !_.isNil(limit));
  const classes = classNames("awe-chart", "expand", style, {"hidden": !visible});
  return <div className={classes} id={id} ref={containerRef}
    {...testHook(TestIds.chart, {rendered, zoomed, attributes: {"chart-id": id}})}>
    <div ref={chartElementRef} style={{position: "absolute", left: 0, top: 0, bottom: 0, right: 0}}/>
  </div>;
}

AweChart.propTypes = {
  id: PropTypes.string
};

export default AweChart;
