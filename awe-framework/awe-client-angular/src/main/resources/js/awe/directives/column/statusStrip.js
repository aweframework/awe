import {aweApplication} from "../../awe";
import {buildStatusStrip, statusStripTooltip} from "../../services/grid/statusStrip";

const STATUS_STRIP_TEMPLATE = "<div class=\"column-input column-status-strip text-{{::component.controller.align}} " +
  "{{component.model.values[0].style}} no-animate\" ng-show=\"component.controller.visible\" ng-cloak></div>";

// Show delay of the tooltip of a bar: half of the help tooltip delay of the engine (helpTimeout, 1000 ms) and about
// half of the native title, which felt slow over a strip (#993). Once a tooltip is visible, moving to another bar
// updates it at once.
export const STATUS_STRIP_TOOLTIP_DELAY = 500;

const BAR = ".awe-status-strip-item";

/**
 * Builds the elements of a strip (the labels are set as text attributes, never as markup)
 * @param {Object} strip Strip built by buildStatusStrip
 * @return {HTMLElement} Strip element
 */
export function stripElement(strip) {
  const holder = document.createElement("span");
  holder.className = "awe-status-strip";
  holder.setAttribute("role", "img");
  holder.setAttribute("aria-label", strip.label);
  strip.items.forEach(item => {
    const mark = document.createElement("span");
    mark.className = `awe-status-strip-item awe-status-strip-${item.tone}`;
    mark.setAttribute("data-tone", item.tone);
    mark.setAttribute("data-tooltip", statusStripTooltip(item));
    holder.appendChild(mark);
  });
  return holder;
}

/**
 * Tooltip of the bars of a strip: the tooltip of the engine for grid cells (grid-cell-tooltip, the Bootstrap tooltip
 * markup on the body). One tooltip per strip, moved to the hovered bar (the hover is delegated to the strip), instead of
 * one per bar, so a grid of long strips does not bind a listener to every bar. The text is set as text, never as markup.
 * @param {HTMLElement} holder Strip element
 * @return {Function} Hides the tooltip
 */
export function bindTooltip(holder) {
  let tooltip = null;
  let timer = null;
  const place = function (bar) {
    if (!tooltip) {
      tooltip = $("<div class=\"tooltip top in grid-cell-tooltip awe-status-strip-tooltip\" role=\"tooltip\"><div class=\"tooltip-arrow\"></div><div class=\"tooltip-inner\"></div></div>");
      tooltip.appendTo(document.body);
    }
    tooltip.find(".tooltip-inner").text(bar.getAttribute("data-tooltip"));
    const rect = bar.getBoundingClientRect();
    const box = tooltip[0].getBoundingClientRect();
    tooltip.css({top: rect.top - box.height, left: rect.left + rect.width / 2 - box.width / 2});
  };
  const hide = function () {
    clearTimeout(timer);
    if (tooltip) {
      tooltip.remove();
      tooltip = null;
    }
  };
  holder.addEventListener("mouseover", function (event) {
    const bar = event.target.closest(BAR);
    if (bar) {
      clearTimeout(timer);
      if (tooltip) {
        place(bar);
      } else {
        timer = setTimeout(() => place(bar), STATUS_STRIP_TOOLTIP_DELAY);
      }
    }
  });
  holder.addEventListener("mouseleave", hide);
  return hide;
}

// Column status strip directive: draws the ordered statuses of the cell (a JSON array of {tone, label, title?} items) as
// bars of their tone; hovering a bar shows its label and title. The strip is one image whose accessible name counts the
// statuses per label, and it is not focusable. The rules come from the module shared with the React client.
aweApplication.directive('aweColumnStatusStrip',
  ['Column', 'Component',
    function (Column, Component) {
      return {
        restrict: 'E',
        replace: true,
        template: STATUS_STRIP_TEMPLATE,
        link: function (scope, elem, attrs) {
          // Create column, criterion and component
          let column = new Column(attrs);
          let component = new Component(scope, column.id);
          component.address = column.address;

          // Initialize criterion and column
          if (column.init(component).asComponent()) {
            let drawn = null;
            let hideTooltip = () => undefined;

            /**
             * Retrieves the statuses of the cell
             * @returns {*} The value of the cell: a JSON text or an already parsed list
             */
            component.getVisibleValue = function () {
              let model = component.model || {};
              let values = model.values || [];
              let value = values.length > 0 ? values[0].value : null;
              return value === null || value === undefined ? model.selected : value;
            };

            /**
             * Draws the strip again when the cell value changed
             */
            component.updateVisibleValue = function () {
              let strip = buildStatusStrip(component.getVisibleValue());
              let key = JSON.stringify(strip);
              if (key !== drawn) {
                drawn = key;
                hideTooltip();
                elem.empty();
                if (strip) {
                  const holder = stripElement(strip);
                  hideTooltip = bindTooltip(holder);
                  elem.append(holder);
                }
              }
            };

            // Draw on generation, and never leave a tooltip behind
            component.updateVisibleValue();
            elem.on("$destroy", () => hideTooltip());
          }
        }
      };
    }
  ]);
