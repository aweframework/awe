import {aweApplication} from "../../awe";
import {echarts, getEChartsLocale} from "awe-client-react/utilities/echartsSetup";
import {observeColorMode, resolveChartColors, sameColors} from "awe-client-react/utilities/echartsTheme";

// The colors of the chrome come from the design tokens (see echartsTheme), so the chart uses the plain theme
const PLAIN_THEME = null;

/**
 * Read the size of the element that holds the chart
 * @param {HTMLElement} element Chart element
 * @returns {{width: number, height: number}} Size
 */
function sizeOf(element) {
  return {width: element.clientWidth, height: element.clientHeight};
}

// Apache ECharts plugin: draws the `echartsModel` of the chart with the SVG renderer
aweApplication.directive('uiChart', ['AweSettings', 'AweUtilities',
  /**
   * Chart plugin
   * @param {Service} $settings
   * @param {Service} Utilities
   */
  function ($settings, Utilities) {
    return {
      restrict: 'A',
      link: function (scope, elem, attrs) {
        const element = elem[0];
        let size = {width: 0, height: 0};
        let colors = null;
        let stopObserving = null;

        /**
         * Draw the chart option (again)
         */
        const draw = function () {
          const chart = scope.component?.chartInstance;
          if (!chart) {
            return;
          }
          try {
            chart.setOption(scope.component.getOption({...size, colors}), {notMerge: true});
          } catch (error) {
            console.error(`[ERROR] The chart '${scope.component.id}' could not be drawn`, error);
          }
        };

        /**
         * Mark the chart as drawn (browser tests wait for it)
         */
        const markRendered = function () {
          if (!scope.component.rendered) {
            scope.component.rendered = true;
            scope.$applyAsync();
          }
        };

        /**
         * Resolve the colors of the chart again (the mode, the surface or the palette of the application changed) and
         * draw the option with them
         */
        const refreshColors = function () {
          const resolved = resolveChartColors(element);
          if (!sameColors(colors, resolved)) {
            colors = resolved;
            draw();
          }
        };

        /**
         * Dispose the chart instance
         */
        const dispose = function () {
          const chart = scope.component?.chartInstance;
          if (chart) {
            chart.dispose();
            scope.component.chartInstance = null;
          }
        };

        /**
         * Create the chart instance (again when the language changes: the texts of ECharts are set on creation)
         */
        const create = function () {
          const component = scope.component;
          dispose();
          colors = resolveChartColors(element);
          size = sizeOf(element);
          let chart;
          try {
            chart = echarts.init(element, PLAIN_THEME, {
              renderer: "svg",
              locale: getEChartsLocale($settings.get("language"))
            });
          } catch (error) {
            console.error(`[ERROR] The chart '${component.id}' could not be created`, error);
            return;
          }
          component.chartInstance = chart;
          // The chart events come from outside AngularJS: the template reads what they change (x, y, zoom)
          const inDigest = (handler) => (params) => {
            handler(params, chart);
            scope.$applyAsync();
          };
          chart.on("rendered", markRendered);
          chart.on("finished", markRendered);
          chart.on("click", inDigest(component.onClick));
          chart.on("dblclick", inDigest(component.onDblClick));
          chart.on("contextmenu", inDigest(component.onContextMenu));
          chart.on("datazoom", inDigest(component.onZoom));
          chart.on("legendselectchanged", inDigest(component.onLegendSelect));
          component.render = draw;
          stopObserving = stopObserving || observeColorMode(refreshColors);
          draw();
        };

        /**
         * Resize the chart when the size of its element changes
         */
        const onResize = function () {
          const chart = scope.component?.chartInstance;
          if (!chart || !Utilities.isVisible(element)) {
            return;
          }
          const current = sizeOf(element);
          if (current.width !== size.width || current.height !== size.height) {
            size = current;
            chart.resize();
            // The layout of the option depends on the size of the chart
            draw();
          }
        };

        /**********************************************************************/
        /* WATCHES                                                            */
        /**********************************************************************/

        // The chart is created when the chart service has its model; the service draws it again on each change
        const unwatch = scope.$watch(attrs.uiChart, function (model) {
          if (model) {
            unwatch();
            create();
          }
        });
        // Watch for screen initialization
        scope.$on("initialised", onResize);
        // Watch for element resize
        scope.$on("resize", onResize);
        // Watch for element resize
        scope.$on("resize-action", onResize);
        // Watch for language change
        scope.$on('languageChanged', function () {
          if (scope.component?.chartInstance) {
            create();
          }
        });
        scope.$on('$destroy', function () {
          dispose();
          if (stopObserving) {
            stopObserving();
            stopObserving = null;
          }
        });
      }
    };
  }]);
