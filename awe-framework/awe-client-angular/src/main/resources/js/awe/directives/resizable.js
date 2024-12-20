import {aweApplication} from "../awe";

// Resizable directive
aweApplication.directive('aweResizable',
  ['ServerData', 'Component', 'AweUtilities',
    function (ServerData, Component, Utilities) {

      /**
       * Manage drag event
       * @param e Event
       * @param dragOptions Drag options
       */
      function drag(e, dragOptions) {
        const {element, scope, w, h, vx, vy, start, dragDir, axis} = dragOptions;
        let offset = axis === 'x' ? start - e.clientX : start - e.clientY;
        switch (dragDir) {
          case 'top':
            if (scope.flex) {
              element[0].style.flexBasis = h + (offset * vy) + 'px';
            } else {
              element[0].style.height = h + (offset * vy) + 'px';
            }
            break;
          case 'right':
            if (scope.flex) {
              element[0].style.flexBasis = w - (offset * vx) + 'px';
            } else {
              element[0].style.width = w - (offset * vx) + 'px';
            }
            break;
          case 'bottom':
            if (scope.flex) {
              element[0].style.flexBasis = h - (offset * vy) + 'px';
            } else {
              element[0].style.height = h - (offset * vy) + 'px';
            }
            break;
          case 'left':
            if (scope.flex) {
              element[0].style.flexBasis = w + (offset * vx) + 'px';
            } else {
              element[0].style.width = w + (offset * vx) + 'px';
            }
            break;
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
