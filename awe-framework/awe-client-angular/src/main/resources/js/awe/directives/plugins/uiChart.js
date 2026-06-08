import {aweApplication} from "../../awe";

// HIGHCHARTS
import Highcharts from "highcharts/highstock";
import "HighchartsLocale";
import "HighchartsThemes";

require("highcharts/highcharts-more.src")(Highcharts);
require("highcharts/highcharts-3d.src")(Highcharts);
require("highcharts/modules/drilldown.src")(Highcharts);
require("highcharts/modules/boost.src")(Highcharts);
require("highcharts/modules/no-data-to-display.src")(Highcharts);
require("highcharts/modules/exporting.src")(Highcharts);

function normalizeSvgReference(value) {
  if (value?.indexOf('url(') !== 0) {
    return value;
  }
  let match = value.match(/#([^)'"]+)\)?/);
  return match ? 'url(#' + match[1] + ')' : value;
}

function normalizeSvgNodeAttribute(node, attribute) {
  if (!node?.hasAttribute?.(attribute)) {
    return;
  }
  let value = node.getAttribute(attribute);
  let normalized = normalizeSvgReference(value);
  if (normalized !== value) {
    node.setAttribute(attribute, normalized);
  }
}

function cloneChartOptions(options) {
  return typeof structuredClone === 'function'
    ? structuredClone(options)
    : JSON.parse(JSON.stringify(options));
}

function normalizeSvgReferences(chartInstance) {
  let container = chartInstance?.renderer?.box || chartInstance?.container;
  if (!container) {
    return;
  }

  if (chartInstance.renderer) {
    chartInstance.renderer.url = '';
  }

  _.each(container.getElementsByTagName('*'), function (node) {
    _.each(['fill', 'stroke', 'clip-path'], function (attribute) {
      normalizeSvgNodeAttribute(node, attribute);
    });
  });
}

function destroyChartInstance(component) {
  if (component.chartInstance) {
    component.chartInstance.destroy();
    component.chartInstance = null;
  }
}

function updateChartDimensions(elem, Utilities) {
  if (!Utilities.isVisible(elem[0])) {
    return null;
  }

  return {
    width: elem[0].clientWidth,
    height: elem[0].clientHeight
  };
}

function applyChartTheme(highchartOptions, themeChart) {
  if (themeChart === undefined || themeChart === "" || !Highcharts.theme[themeChart]) {
    return;
  }

  _.merge(highchartOptions, Highcharts.theme[themeChart]);
  _.each(highchartOptions.xAxis, function (xAxis) {
    _.merge(xAxis, Highcharts.theme[themeChart].xAxis);
  });
  _.each(highchartOptions.yAxis, function (yAxis) {
    _.merge(yAxis, Highcharts.theme[themeChart].yAxis);
  });
}

function getChartEvents(scope, elem) {
  return {
    chart: {
      renderTo: elem[0],
      events: {
        selection: scope.component.onZoom,
        redraw: scope.component.onRedraw,
        load: scope.component.onRedraw
      }
    },
    plotOptions: {
      series: {
        cursor: 'pointer',
        point: {
          events: {
            click: scope.component.onClick
          }
        }
      }
    }
  };
}

function buildChartInstance(highchartOptions, isStockChart) {
  return isStockChart ? Highcharts.stockChart(highchartOptions) : Highcharts.chart(highchartOptions);
}

// Highcharts plugin
aweApplication.directive('uiChart', ['AweSettings', 'AweUtilities',
  /**
   * Chart generic methods
   * @param {Service} $settings
   * @param {Service} Utilities
   */
  function ($settings, Utilities) {
    // Get default $settings

    // Set global options
    Highcharts.setOptions({
      global: {
        useUTC: false
      },
      lang: global.HighchartsLocale[$settings.get("language")]
    });
    return {
      restrict: 'A',
      require: ['ngModel'],
      link: function (scope, elem, attrs, modelController) {
        // Flag chart all ready initialized
        let  chartDimensions = {
          width: 0,
          height: 0
        };
        // Add global options
        let  highchartOptions = {};
        let  chartOptions = {};
        /**
         * Plugin initialization
         * @param {object} newValue plugin parameters
         * @param {object} oldValue plugin parameters
         */
        let  initPlugin = function (newValue, oldValue) {
          if (!(newValue && (!scope.component.initialized || newValue !== oldValue))) {
            if (scope.component.chartInstance) {
              scope.component.chartInstance.redraw();
            }
            return;
          }

          highchartOptions = {};
          chartOptions = cloneChartOptions(newValue);
          scope.component.initializeModel(chartOptions, modelController);

          _.merge(highchartOptions, chartOptions);
          highchartOptions.exporting = {
            enabled: false
          };

          scope.component.translateLabels(highchartOptions);
          applyChartTheme(highchartOptions, chartOptions.theme);
          _.merge(highchartOptions, getChartEvents(scope, elem));

          destroyChartInstance(scope.component);
          scope.component.chartInstance = buildChartInstance(highchartOptions, newValue.stockChart);
          normalizeSvgReferences(scope.component.chartInstance);

          Utilities.timeout(function () {
            normalizeSvgReferences(scope.component.chartInstance);
          }, 0);

          let currentDimensions = updateChartDimensions(elem, Utilities);
          if (currentDimensions) {
            chartDimensions = currentDimensions;
          }

          scope.component.initialized = true;
        };
        /**
         * Update global options
         */
        let  updateGlobalOptions = function () {
          // Update element as date with options
          destroyChartInstance(scope.component);
          scope.component.initialized = false;
          // Get lenguage
          let  language = global.HighchartsLocale[$settings.get("language")];
          // Update global options
          Highcharts.setOptions({
            lang: language
          });
          initPlugin(chartOptions);
        };
        /**
         * Check if dimensions have changed
         * @returns {Boolean}
         */
        let  changedDimensions = function () {
          let  changed = false;
          let  currentDimensions = {
            width: elem[0].clientWidth,
            height: elem[0].clientHeight
          };
          changed = currentDimensions.width !== chartDimensions.width || currentDimensions.height !== chartDimensions.height;
          chartDimensions = currentDimensions;
          return changed;
        };
        /**********************************************************************/
        /* WATCHES                                                            */
        /**********************************************************************/

        // Watch for controller changes
        scope.$watch(attrs.uiChart, initPlugin);
        /**
         * Redraw on resize
         */
        let  onResize = function () {
          if (scope.component.initialized && Utilities.isVisible(elem[0])) {
            if (scope.component.chartInstance && changedDimensions()) {
              scope.component.chartInstance.reflow();
            }
          }
        };
        // Watch for screen initialization
        scope.$on("initialised", onResize);
        // Watch for element resize
        scope.$on("resize", onResize);
        // Watch for element resize
        scope.$on("resize-action", onResize);
        // Watch for language change
        scope.$on('languageChanged', updateGlobalOptions);
        scope.$on('$destroy', function () {
          if (scope.component.initialized && scope.component.chartInstance) {
            scope.component.chartInstance.destroy();
            scope.component.chartInstance = null;
          }
        });
      }
    };
  }]);
