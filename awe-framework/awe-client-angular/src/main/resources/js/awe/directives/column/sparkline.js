import {aweApplication} from "../../awe";
import {buildSparkline, sparklineMarkup} from "../../services/chart/sparklineRenderer";

const SPARKLINE_TEMPLATE = "<div class=\"column-input column-sparkline text-{{::component.controller.align}} " +
  "{{component.model.values[0].style}} no-animate\" ng-show=\"component.controller.visible\" ng-cloak></div>";

// Column sparkline directive: draws the series of numbers of the cell (a JSON array) as a small SVG chart. The drawing
// comes from the renderer shared with the React client. The SVG is not focusable, so the keyboard navigation of the
// grid is not affected, and every point has a transparent hit area with its value as the tooltip.
aweApplication.directive('aweColumnSparkline',
  ['Column', 'Component',
    function (Column, Component) {
      return {
        restrict: 'E',
        replace: true,
        template: SPARKLINE_TEMPLATE,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let column = new Column(attrs);
          let component = new Component(scope, column.id);
          component.address = column.address;

          // Initialize criterion and column
          if (column.init(component).asComponent()) {
            let drawn = null;

            /**
             * Retrieves the series of the cell
             * @returns {*} The value of the cell: a JSON text or an already parsed list
             */
            component.getVisibleValue = function () {
              let model = component.model || {};
              let values = model.values || [];
              let value = values.length > 0 ? values[0].value : null;
              return value === null || value === undefined ? model.selected : value;
            };

            /**
             * Draws the series again when the cell value or the options of the column changed
             */
            component.updateVisibleValue = function () {
              let controller = component.controller || {};
              let value = component.getVisibleValue();
              let markup = sparklineMarkup(buildSparkline(value, {
                type: controller.sparklineType,
                color: controller.sparklineColor,
                negativeColor: controller.sparklineNegativeColor,
                lastPoint: controller.sparklineLastPoint,
                minMax: controller.sparklineMinMax
              }));
              if (markup !== drawn) {
                drawn = markup;
                elem.html(markup);
              }
            };

            // Draw on generation
            component.updateVisibleValue();
          }
        }
      };
    }
  ]);
