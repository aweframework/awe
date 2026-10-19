import {aweApplication} from "../../awe";
import {buildEChartsOption, findDrilldownId} from "awe-client-react/utilities/echartsOption";
import {parseChartOptions, pointOf, zoomOf} from "awe-client-react/utilities/echartsEvents";
import {linkedNames} from "awe-client-react/utilities/echartsLegend";
import {getEChartsSerie, mergeSeriesPoints} from "awe-client-react/utilities/echartsSeries";
import {renderSvg} from "awe-client-react/utilities/echartsSetup";
import {getChartTexts} from "./texts";
import "../../directives/plugins/uiChart";
import "./events";

const AXES = ["x", "y"];
const FULL_RANGE = {start: 0, end: 100};

/**
 * Size of the printed chart image for each page orientation
 * @type {object}
 */
const PRINT_SIZES = {
  LANDSCAPE: {width: 1167, height: 360},
  PORTRAIT: {width: 796, height: 540}
};

/**
 * Retrieve the AWE axis (x or y) that a data zoom component zooms. The AWE x axis is the vertical one in an inverted
 * chart
 * @param {object} dataZoom Data zoom component
 * @param {boolean} inverted The chart is inverted
 * @returns {string|undefined} Axis
 */
function axisOf(dataZoom, inverted) {
  const axis = AXES.find(name => !_.isNil(dataZoom[`${name}AxisIndex`]));
  return inverted && axis ? AXES.find(name => name !== axis) : axis;
}

/**
 * Give the zoom of the chart to the data zoom components of an option, so that drawing the chart again keeps it
 * @param {object} option ECharts option, which is changed
 * @param {object} zoom Zoom of the chart model: {x: {min, max}, y: {min, max}}
 * @param {boolean} inverted The chart is inverted
 * @returns {object} Option
 */
function applyZoom(option, zoom, inverted) {
  if (!option.dataZoom) {
    return option;
  }
  option.dataZoom = _.castArray(option.dataZoom).map(dataZoom => {
    const range = zoom[axisOf(dataZoom, inverted)];
    if (!range) {
      return dataZoom;
    }
    const {min = null, max = null} = range;
    return {
      ..._.omit(dataZoom, ["start", "end", "startValue", "endValue"]),
      ...(min === null ? {start: FULL_RANGE.start} : {startValue: min}),
      ...(max === null ? {end: FULL_RANGE.end} : {endValue: max})
    };
  });
  return option;
}

// Chart service
aweApplication.factory('Chart',
  ['Component', '$translate', 'ChartEvents', 'Control', 'AweUtilities', 'AweSettings',
    /**
     * Chart generic methods
     * @param {function} Component
     * @param {Object} $translate
     * @param {Object} ChartEvents
     * @param {Object} Control
     * @param {Object} Utilities
     * @param {Object} $settings Awe $settings
     */
    function (Component, $translate, ChartEvents, Control, Utilities, $settings) {

      /**
       * Translate a locale key
       * @param {string} key Key
       * @returns {string} Translated text
       */
      const translate = (key) => $translate.instant(key);

      /**
       * Chart constructor
       * @param {Scope} scope Chart scope
       * @param {String} id Chart id
       * @param {String} element Chart element
       */
      function Chart(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Component(this.scope, this.id);
        this.component.chart = this;

        // Define attribute methods
        this.attributeMethods = {
          /**
           * Retrieve x value
           * @param {object} component Component scope
           * @returns {mixed} Current value
           */
          "x": function (component) {
            return component.model.selected && "x" in component.model.selected ? component.model.selected.x : null;
          },
          /**
           * Retrieve y value
           * @param {object} component Component scope
           * @returns {mixed} Current value
           */
          "y": function (component) {
            return component.model.selected && "y" in component.model.selected ? component.model.selected.y : null;
          },
          /**
           * Retrieve min x value on zoom
           * @param {object} component Component scope
           * @returns {mixed} Current text value
           */
          "xMin": function (component) {
            return component.model.zoom && "x" in component.model.zoom ? component.model.zoom.x.min : null;
          },
          /**
           * Retrieve max x value on zoom
           * @param {object} component Component scope
           * @returns {mixed} Current text value
           */
          "xMax": function (component) {
            return component.model.zoom && "x" in component.model.zoom ? component.model.zoom.x.max : null;
          },
          /**
           * Retrieve min y value on zoom
           * @param {object} component Component scope
           * @returns {mixed} Current text value
           */
          "yMin": function (component) {
            return component.model.zoom && "y" in component.model.zoom ? component.model.zoom.y.min : null;
          },
          /**
           * Retrieve max y value on zoom
           * @param {object} component Component scope
           * @returns {mixed} Current text value
           */
          "yMax": function (component) {
            return component.model.zoom && "y" in component.model.zoom ? component.model.zoom.y.max : null;
          }
        };

        // Add attribute methods
        _.merge(this.component.attributeMethods, this.attributeMethods);
        this.component.asChart = function () {
          return this.chart.init();
        };
        return this.component;
      }

      Chart.prototype = {
        init: function () {
          // Init as component
          let  component = this.component;
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          /**
           * Initialize the chart model: the ECharts model sent by the server, no selection, no zoom and no drilldown
           */
          component.initializeModel = function () {
            component.echartsModel = component.controller.echartsModel || {};
            component.drill = null;
            component.rendered = false;
            component.model.selected = {};
            component.model.zoom = {};
          };

          /**
           * Draw the chart again (the chart plugin replaces it when it creates the chart)
           */
          component.render = component.render || _.noop;

          /**
           * Check if the values have more points than the `limitPointsSerie` setting allows
           * @param {object[]} values Chart values
           * @returns {boolean} There are too many points
           */
          const isTooMuchData = function (values) {
            const limit = _.get($settings.get("chartOptions"), "limitPointsSerie");
            return Number.isFinite(limit) && values.length > limit;
          };

          /**
           * Build the ECharts option of the chart
           * @param {{width: number, height: number, dark: boolean}} environment Size of the chart and dark mode
           * @returns {object} ECharts option
           */
          component.getOption = function ({width, height, dark}) {
            const values = component.model.values || [];
            const texts = getChartTexts($settings.get("language"));
            const tooMuchData = isTooMuchData(values);
            const option = buildEChartsOption(component.echartsModel, tooMuchData ? [] : values, {
              t: translate,
              locale: tooMuchData ? {...texts, noData: texts.tooMuchData} : texts,
              width, height, dark,
              drill: component.drill,
              onBack: component.drillUp
            });
            return applyZoom(option, component.model.zoom || {}, isInverted());
          };

          /**
           * Check if the chart is zoomed (shows a part of its range)
           * @returns {boolean} The chart is zoomed
           */
          component.isZoomed = function () {
            const zoom = component.model.zoom || {};
            return AXES.some(axis => zoom[axis] && (!_.isNil(zoom[axis].min) || !_.isNil(zoom[axis].max)));
          };

          /**
           * Update the series of the ECharts model, which closes the drilldown
           * @param {function(object[]): object[]} update Retrieves the new series from the current ones
           */
          const updateSeries = function (update) {
            const model = component.echartsModel || {};
            component.echartsModel = {...model, series: update(model.series || [])};
            component.drill = null;
          };

          /**
           * Check if the chart is inverted (its axes are swapped)
           * @returns {boolean} The chart is inverted
           */
          const isInverted = () => Boolean(_.get(component.echartsModel, "awe.inverted"));

          /**
           * Store the model values and draw the chart again
           * @param {object[]} values chart values
           */
          component.updateModel = function (values) {
            component.model.values = values;
            component.render();
          };

          /**
           * On add points event: add the points to the values. When the values reach the maximum of the chart, the
           * oldest points are removed
           * @param {object[]} points chart points
           */
          component.addPoints = function (points) {
            if (!points) {
              return;
            }
            const values = component.model.values;
            const max = component.getMax();
            _.each(points, function (point) {
              values.push(point);
              if (max > 0 && values.length > max) {
                values.shift();
              }
            });
            component.render();
          };

          /**
           * On add series event: add the series (and their points) to the chart
           * @param {object[]} series series to add
           */
          component.addSeries = function (series) {
            const ids = new Set(_.map(series, "id"));
            const inverted = isInverted();
            updateSeries(current => [
              ...current.filter(serie => !ids.has(serie.id)),
              ...series.map(serie => getEChartsSerie(serie, inverted))
            ]);
            component.model.values = mergeSeriesPoints(component.model.values || [], series);
            component.render();
          };

          /**
           * On replace series event: the series (and their points) replace the ones of the chart
           * @param {object[]} series series of the chart
           */
          component.replaceSeries = function (series) {
            const inverted = isInverted();
            updateSeries(() => series.map(serie => getEChartsSerie(serie, inverted)));
            component.model.values = mergeSeriesPoints([], series);
            component.render();
          };

          /**
           * On remove series event: remove the series of the chart with the same identifier
           * @param {object[]} series series to remove
           */
          component.removeSeries = function (series) {
            const ids = new Set(_.map(series, "id"));
            updateSeries(current => current.filter(serie => !ids.has(serie.id)));
            component.render();
          };

          /**
           * On reset action event: remove the points of the chart
           */
          component.resetChart = function () {
            component.model.values = [];
            component.drill = null;
            component.render();
          };
          /**
           * On restore action event
           * Restore chart with initial model
           */
          component.restoreChart = function () {
            Control.restoreInitialModelAttribute(component.address, "values");
          };
          /**
           * Basic getAttribute function (To be overwritten on complex directives)
           * @param {type} attribute Attribute to check
           * @returns {undefined}
           */
          component.getAttribute = function (attribute) {
            return component.attributeMethods[attribute](component);
          };
          /**
           * Check what parameters have changed and apply the changes to the chart
           * @param {object} parameters
           */
          component.checkParametersChanged = function (parameters) {
            let  options = parseChartOptions(parameters.chartOptions);
            if ("zoom" in options) {
              component.changeZoom(options.zoom);
            }
          };
          /**
           * Change the chart zoom (the zoom of a linked chart)
           * @param {object} zoom Zoom of each axis: {x: {min, max}, y: {min, max}}
           */
          component.changeZoom = function (zoom) {
            let changed = false;
            _.each(AXES, function (axis) {
              if (!zoom || !(axis in zoom)) {
                return;
              }
              const min = _.get(zoom, [axis, "min"], null);
              const max = _.get(zoom, [axis, "max"], null);
              const current = component.model.zoom[axis] || {};
              if ((current.min ?? null) !== min || (current.max ?? null) !== max) {
                component.model.zoom[axis] = {min, max};
                changed = true;
              }
            });
            if (changed) {
              component.render();
            }
          };
          /**
           * Close the drilldown series and show the series that opened it
           */
          component.drillUp = function () {
            component.drill = null;
            component.render();
          };
          /**********************************************************************/
          /* EVENT HANDLERS                                                     */
          /**********************************************************************/

          /**
           * Store point clicked on chart model
           * @param {object} params ECharts event parameters
           */
          component.storeClickData = function (params) {
            component.model.selected = pointOf(params, isInverted());
          };

          /**
           * Click event: opens the drilldown series of the point, if any
           * @param {object} params ECharts event parameters
           */
          component.onClick = function (params) {
            const target = findDrilldownId(component.echartsModel, params.seriesId);
            if (target) {
              component.drill = {from: params.seriesId, to: target};
              component.render();
            }
            component.storeClickData(params);
            component.storeEvent("click");
          };

          /**
           * Double Click event
           * @param {object} params ECharts event parameters
           */
          component.onDblClick = function (params) {
            component.storeClickData(params);
            component.storeEvent("double-click");
          };

          /**
           * Context menu event (the menu is shown by the mousedown listener of the chart element)
           * @param {object} params ECharts event parameters
           */
          component.onContextMenu = function (params) {
            _.invoke(params, "event.event.preventDefault");
            component.storeClickData(params);
            component.storeEvent("context-menu");
          };

          /**
           * Zoom event
           * @param {object} params ECharts event parameters
           * @param {object} chart ECharts instance
           */
          component.onZoom = function (params, chart) {
            const {xMin, xMax, yMin, yMax} = zoomOf(chart, isInverted());
            component.model.zoom.x = {min: xMin, max: xMax};
            component.model.zoom.y = {min: yMin, max: yMax};
            component.storeEvent("zoom");
          };

          /**
           * Legend event: a series that is linked to another one hides and shows with it
           * @param {object} params ECharts event parameters
           * @param {object} chart ECharts instance
           */
          component.onLegendSelect = function ({name, selected}, chart) {
            const type = _.get(selected, name) === false ? "legendUnSelect" : "legendSelect";
            const series = _.get(component.echartsModel, "series", []);
            linkedNames(series, name, translate).forEach(linked => chart.dispatchAction({type, name: linked}));
          };

          /**
           * Update the model on model changed
           * @param {Object} changes
           */
          component.onModelChanged = function (changes) {
            component.updateModel(changes.values);
          };

          /**********************************************************************/
          /* API                                                                */
          /**********************************************************************/

          /**
           * Get chart return data function;
           * @returns Object data of chart
           */
          let  api = Control.getAddressApi(component.address);
          /**
           * Retrieve the data of the component that should be sent to the server
           * @return {Object} Data to send to the server
           */
          api.getData = function () {
            let  data = {};
            let  model = Control.getAddressModel(component.address);
            if (model.selected) {
              if ("x" in model.selected) {
                data[component.address.component + ".x"] = model.selected.x;
              }
              if ("y" in model.selected) {
                data[component.address.component + ".y"] = model.selected.y;
              }
            }
            return data;
          };
          /**
           * Retrieve the data of the component that should be sent to the server for printing actions: the chart
           * image (SVG) drawn apart, in light colors and with the size of the page
           * @param {String} orientation Page orientation (LANDSCAPE or PORTRAIT)
           * @return {Object} Data to send to the server
           */
          api.getPrintData = function (orientation) {
            const size = PRINT_SIZES[orientation] || PRINT_SIZES.PORTRAIT;
            const language = $settings.get("language");
            const option = buildEChartsOption(component.echartsModel, component.model.values || [],
              {...size, t: translate, locale: getChartTexts(language), dark: false});
            return {[component.address.component]: {image: renderSvg(option, size, language)}};
          };
          /**********************************************************************/
          /* EVENTS                                                             */
          /**********************************************************************/

          // Initialize the chart model
          component.initializeModel();

          // Map grid actions
          ChartEvents.mapActions(component);
          /**
           * Event listeners
           */
          component.listeners = component.listeners || {};

          // Action listener definition
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, check: ["values"], service: component, method: "onModelChanged"});

          // On model change launch dependency
          component.listeners["controllerChange"] = component.scope.$on("controllerChange", function (event, parameters) {
            if (_.isEqual(parameters.address, component.address)) {
              component.checkParametersChanged(parameters.controller);
            }
          });

          // Return initialization well done
          return true;
        }
      };
      return Chart;
    }
  ]);
