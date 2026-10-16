import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";

// Loader directive
aweApplication.directive('aweLoader',
  ['ServerData', '$compile', '$templateCache', 'AweUtilities',
    /**
     * Help directive
     * @param {object} ServerData Server call service
     * @param {function} $compile compilation service
     * @param {object} $templateCache Template cache
     * @param {object} Utilities Awe utilities
     */
    function (ServerData, $compile, $templateCache, Utilities) {
      // Action Controller methods
      let TEMPLATE_LOADING = "--LOADING--";
      // The skeleton is drawn by the client: a block placeholder over the component that is loading
      const SKELETON = "skeleton";
      const SKELETON_TEMPLATE = `<div class="skeleton-block"><span class="skeleton-bar"></span></div>`;
      // Skeleton kinds: the loader draws the shape of the component (a field, a box with its label, a button, a chart)
      const RELOAD = "reload";
      const BAR_HEIGHTS = [45, 70, 55, 85, 60, 40, 75];
      const TREND_TYPES = ["line", "area", "spline", "areaspline", "scatter"];
      const bar = style => `<span class="skeleton-bar"${style ? ` style="${style}"` : ""}></span>`;
      const CHECK_TEMPLATE = radio => `<div class="skeleton-block skeleton-box"><span class="skeleton-bar skeleton-check${radio ? " skeleton-radio" : ""}"></span>${bar("flex: 1 1 0")}</div>`;
      const CHART_BARS = BAR_HEIGHTS.map(height => bar(`height: ${height}%`)).join("");

      /**
       * Check if the loading icon is the default one: the component draws its skeleton instead of a spinner
       * @param {String} icon Loading icon
       * @return {boolean} Default loading icon
       */
      const isDefaultIcon = icon => !icon || icon === "spinner" || icon === SKELETON;

      /**
       * Get the type of the chart the loader belongs to
       * @param {Object} scope Loader scope
       * @return {String} Chart type
       */
      function getChartType(scope) {
        const model = (scope.controller && scope.controller.chartModel) || {};
        return (model.chart && model.chart.type) || (model.series && model.series[0] && model.series[0].type) || "";
      }

      /**
       * Get the markup of a skeleton
       * @param {String} kind Skeleton kind (field, checkbox, radio, button, chart)
       * @param {Object} scope Loader scope
       * @return {String} Markup
       */
      function getSkeletonTemplate(kind, scope) {
        switch (kind) {
          case "checkbox":
          case "radio":
            return CHECK_TEMPLATE(kind === "radio");
          case "chart": {
            const type = getChartType(scope);
            const shape = type === "pie" ? "pie" : (TREND_TYPES.includes(type) ? "trend" : "bars");
            return `<div class="skeleton-block"><div class="skeleton-chart skeleton-chart-${shape}">${shape === "bars" ? CHART_BARS : bar()}</div></div>`;
          }
          default:
            return SKELETON_TEMPLATE;
        }
      }
      return {
        restrict: 'E',
        link: function (scope, elem, attrs) {
          // Test hook: every loader is a "loader" unless its owner gave it a more specific hook
          if (!elem.attr(TestAttributes.testId)) {
            elem.attr(TestAttributes.testId, TestIds.loader);
          }

          /**
           * Compile the template
           * @param {type} template
           * @returns {undefined}
           */
          function compileTemplate(template) {
            // Compile the received data
            let newElement = $compile(template)(scope);
            // Which we can then append to our DOM element.
            elem.append(newElement);
          }

          /**
           * Mark an element while the skeleton is drawn, and restore it when the last loader that marked it goes away
           * (several loaders can share a host, as the two fields of a date range)
           * @param {Element} host Element to mark
           * @param {String} className Class to add
           */
          function markHost(host, className) {
            if (host) {
              const hostElement = angular.element(host);
              const countKey = `skeletonMarks-${className}`;
              hostElement.data(countKey, (hostElement.data(countKey) || 0) + 1);
              hostElement.addClass(className);
              let restored = false;
              const restore = () => {
                if (restored) {
                  return;
                }
                restored = true;
                const marks = (hostElement.data(countKey) || 1) - 1;
                hostElement.data(countKey, marks);
                if (marks === 0) {
                  hostElement.removeClass(className);
                }
              };
              elem.on("$destroy", restore);
              scope.$on("$destroy", restore);
            }
          }

          /**
           * Draw the skeleton of the component: the loader is the placeholder, and the element it belongs to (the criterion
           * with its label, the button) is masked. The loader is the test hook from the start: only the visuals are delayed
           * @param {String} kind Skeleton kind
           */
          function showSkeleton(kind) {
            const criterion = elem[0].closest(".criterion");
            elem.addClass("skeleton-loader");
            if (kind === "button") {
              elem.addClass("skeleton-hook");
              markHost(elem[0].parentElement, "skeleton-pending");
              return;
            }
            markHost(criterion, "skeleton-pending");
            compileTemplate(getSkeletonTemplate(kind, scope));
          }

          /**
           * Dim the rows of a grid that reloads: the loader keeps the hook and the grid is busy until the new rows arrive
           */
          function showReload() {
            elem.addClass("grid-reload");
            markHost(elem[0].parentElement, "grid-reloading");
          }

          /**
           * Compile the template
           * @param {type} template
           * @returns {undefined}
           */
          let endWatchTemplateLoaded;

          function onTemplateLoaded(event, template) {
            endWatchTemplateLoaded();
            compileTemplate(template);
          }

          // Observe select2 attributes
          let initWatch = attrs.$observe('iconLoader', initLoader);

          /**
           * Loader initialization
           * @param {String} iconLoader
           */
          function initLoader(iconLoader) {
            const kind = attrs.skeleton;
            if (kind === RELOAD && iconLoader === SKELETON) {
              showReload();
              initWatch();
              return;
            }
            if (kind && kind !== RELOAD && isDefaultIcon(iconLoader)) {
              showSkeleton(kind);
              initWatch();
              return;
            }
            if (iconLoader === SKELETON) {
              elem.addClass("skeleton-loader");
              compileTemplate(SKELETON_TEMPLATE);
              initWatch();
              return;
            }

            // Add action
            let templateName = "loader/" + (iconLoader || "spinner");

            // Check parameters
            let template = $templateCache.get(templateName);
            if (!template) {
              $templateCache.put(templateName, TEMPLATE_LOADING);
              ServerData.preloadAngularTemplate({path: templateName}, function (data) {
                Utilities.publishDelayed('template-' + templateName, data);
                compileTemplate(data);
              });
            } else if (template === TEMPLATE_LOADING) {
              endWatchTemplateLoaded = scope.$on('template-' + templateName, onTemplateLoaded);
            } else {
              compileTemplate(template);
            }
            // Remove watch
            initWatch();
          }
        }
      };
    }
  ]);
