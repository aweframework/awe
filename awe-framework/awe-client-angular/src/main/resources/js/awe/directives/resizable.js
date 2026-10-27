import {aweApplication} from "../awe";

// Resizable directive: the handle of a side resizes the element and, when the parent lays out along that axis,
// the sibling on the handle side follows (it gives or takes the size) so the splitter stays under the pointer
aweApplication.directive('aweResizable',
  ['ServerData', 'Component', 'AweUtilities',
    function (ServerData, Component, Utilities) {

      // Minimum size of the resizable (as in the stylesheet) and of the neighbour when it has no min-width/min-height
      const MIN_SIZE = 160;

      /**
       * Get the sibling on the handle side (next one for right/bottom, previous one for left/top) when the parent
       * lays out along the axis of the handle and the sibling takes space. This sibling follows the splitter.
       * @param {HTMLElement} container Resizable element
       * @param {string} direction Handle direction
       * @param {string} axis Handle axis (x/y)
       * @returns {{element: HTMLElement, size: number, min: number}|null} Neighbour data
       */
      function findNeighbour(container, direction, axis) {
        const parent = container.parentElement;
        const parentStyle = parent ? window.getComputedStyle(parent) : null;
        // Only a flex parent lays out along an axis: a block parent computes flex-direction: row too
        const parentDirection = parentStyle && (parentStyle.display || "").includes("flex") ? parentStyle.flexDirection || "" : "";
        if (!parentDirection.startsWith(axis === 'x' ? 'row' : 'column')) {
          return null;
        }
        const element = direction === 'right' || direction === 'bottom'
          ? container.nextElementSibling : container.previousElementSibling;
        const size = element ? element.getBoundingClientRect()[axis === 'x' ? 'width' : 'height'] : 0;
        if (!element || !(size > 0)) {
          return null;
        }
        const cssMin = parseFloat(window.getComputedStyle(element)[axis === 'x' ? 'minWidth' : 'minHeight']);
        return {element, size, min: Math.max(Number.isFinite(cssMin) ? cssMin : 0, Math.min(MIN_SIZE, size))};
      }

      /**
       * Manage drag event
       * @param e Event
       * @param dragOptions Drag options
       */
      function drag(e, dragOptions) {
        const {element, scope, w, h, vx, vy, start, dragDir, axis, neighbour} = dragOptions;
        const offset = axis === 'x' ? start - e.clientX : start - e.clientY;
        const initial = axis === 'x' ? w : h;
        let size;
        switch (dragDir) {
          case 'top':
            size = h + (offset * vy);
            break;
          case 'right':
            size = w - (offset * vx);
            break;
          case 'bottom':
            size = h - (offset * vy);
            break;
          case 'left':
            size = w + (offset * vx);
            break;
        }

        let neighbourSize = 0;
        if (neighbour) {
          // The neighbour takes what the container gains; when it hits its minimum the container stops growing, and the
          // container does not get under its own minimum either
          size = Math.max(MIN_SIZE, size);
          neighbourSize = Math.max(neighbour.min, neighbour.size - (size - initial));
          size = initial + neighbour.size - neighbourSize;
          neighbour.element.style.flex = `0 0 ${neighbourSize}px`;
        }

        const dimension = dragDir === 'left' || dragDir === 'right' ? 'width' : 'height';
        const applySize = () => {
          if (scope.flex) {
            element[0].style.flexBasis = size + 'px';
          } else {
            element[0].style[dimension] = size + 'px';
          }
        };
        applySize();

        if (neighbour) {
          // A flex item does not get smaller than its content (min-height: auto, a grid keeps its minimum rows area): when
          // one of the two kept more than it was given, the other one gives that back, so the outer edges do not move
          const total = initial + neighbour.size;
          const keptByNeighbour = neighbour.element.getBoundingClientRect()[dimension];
          const keptByContainer = element[0].getBoundingClientRect()[dimension];
          if (keptByNeighbour > neighbourSize) {
            size = total - keptByNeighbour;
            neighbour.element.style.flex = `0 0 ${keptByNeighbour}px`;
            applySize();
          } else if (keptByContainer > size) {
            size = keptByContainer;
            neighbour.element.style.flex = `0 0 ${total - keptByContainer}px`;
            applySize();
          }
        }
      }

      /**
       * Manage onDragStart event
       * @param {*} e
       * @param {string} direction
       * @param {object} dragOptions
       */
      function onDragStart(e, direction, dragOptions) {
        const {element, scope, style} = dragOptions;
        dragOptions.dragDir = direction;
        dragOptions.axis = dragOptions.dragDir === 'left' || dragOptions.dragDir === 'right' ? 'x' : 'y';
        dragOptions.start = dragOptions.axis === 'x' ? e.clientX : e.clientY;
        dragOptions.w = parseInt(style.getPropertyValue("width"));
        dragOptions.h = parseInt(style.getPropertyValue("height"));

        // The neighbour gets a fixed size from the first move on, so a press without a move leaves it fluid
        dragOptions.neighbour = findNeighbour(element[0], direction, dragOptions.axis);

        //prevent transition while dragging
        element.addClass('no-transition');
        const dragFcn = (evt) => drag(evt, dragOptions);

        document.addEventListener('mouseup', () => {
          document.removeEventListener('mousemove', dragFcn, false);
          element.removeClass('no-transition');
          if (scope.onDragEnd) {
            scope.onDragEnd();
          }
        });
        document.addEventListener('mousemove', dragFcn, false);

        // Cancel event propagation
        Utilities.stopPropagation(e, true);
      }

      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template: `<div class="resizable resizable-awe expandible-vertical {{::resizableStyle}}" ng-cloak>
                     <div ng-transclude class="expand expandible-vertical" ng-cloak></div>
                     <div ng-repeat="direction in directions track by direction" class="{{'rg-' + direction}}"
                          ng-mousedown="dragStart($event, direction)"><span class="fa"></span></div>
                   </div>`,
        scope: {
          resizableId: '@'
        },
        link: function (scope, element) {
          // Init as component
          let component = new Component(scope, scope.resizableId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          // Generate style
          scope.resizableStyle = component.controller.style + " " + component.controller.directions;

          // Generate directions
          scope.directions = component.controller.directions ? component.controller.directions.split(" ") : [];

          // Set variables
          scope.flex = true;

          // On drag end
          scope.onDragEnd = function () {
            Utilities.publish("resize-action");
          };

          const dragOptions = {
            scope,
            element,
            style: window.getComputedStyle(element[0], null),
            w: null,
            h: null,
            vx: scope.rCenteredX ? 2 : 1, // if centered double velocity
            vy: scope.rCenteredY ? 2 : 1, // if centered double velocity
            start: null,
            dragDir: null,
            axis: null
          };

          scope.dragStart = (e, direction) => onDragStart(e, direction, dragOptions);
        }
      };
    }
  ]);
