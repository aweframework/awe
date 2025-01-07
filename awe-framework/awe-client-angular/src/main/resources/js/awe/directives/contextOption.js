import {aweApplication} from "../awe";
import "../services/contextMenu";
import {getIconTemplate} from "../services/component";

const template =
`<li ng-show="controller.visible || controller.separator" class="context-option {{controller.opened ? 'open' : ''}}" ng-class="::{'divider': controller.separator, 'dropdown-submenu': controller.hasChildren}" ui-dependency="dependencies" ng-cloak>
  <a ng-if="::!controller.separator" ng-disabled="isDisabled()" title="{{controller.label| translateMultiple}}" name="{{::controller.id}}" class="{{::controller.style}}"
     ng-click="onClick()">
     ${getIconTemplate("nav-icon")}
    <span ng-if="::controller.label" class="context-option-text" translate-multiple="{{::controller.label}}"></span>
  </a>
  <ul ng-if="::!controller.separator && controller.hasChildren" class="context-submenu dropdown-menu" ng-show="controller.opened">
    <awe-context-option ng-repeat="option in controller.contextMenu track by option.id" option-id="{{::option.id}}" option="option"></awe-context-option>
  </ul>
</li>`;

/**
 * Show the submenu (if it exists)
 */
let toggleSubmenu = function (params, Utilities, submenu, show, delay) {
  const {component} = params;
  if (submenu.length > 0) {
    Utilities.timeout.cancel(params.timer);
    params.timer = Utilities.timeout(() => {
      component.controller.opened = show;
    }, delay);
  }
};

/**
 * Initialize layers
 */
let initLayers = function (params, Utilities) {
  const {elem} = params;

  // Look for layers
  let link = elem.children("a");
  let submenu = elem.children("ul");

  // Add mouseenter and mouseleave events
  link.on("mouseenter", () => toggleSubmenu(params, Utilities, submenu, true, undefined));
  link.on("mouseleave", () => toggleSubmenu(params, Utilities, submenu, false, 200));
  if (submenu.length > 0) {
    submenu.on("mouseenter", () => toggleSubmenu(params, Utilities, submenu, true, undefined));
    submenu.on("mouseleave", () => toggleSubmenu(params, Utilities, submenu, false, 200));
  }
};

// Context option directive
aweApplication.directive('aweContextOption',
  ['ActionController', '$compile', 'Component', 'AweUtilities', 'Storage',
    function (ActionController, $compile, Component, Utilities, Storage) {
      return {
        restrict: 'E',
        replace: true,
        template: template,
        scope: {
          'optionId': '@',
          'option': '='
        },
        compile: function (tElem) {
          let  contents = tElem.contents().remove();
          let  compiledContents;

          return {
            pre: function (scope, elem) {
              if (!compiledContents) {
                compiledContents = $compile(contents);
              }
              compiledContents(scope, function (clone) {
                elem.append(clone);
              });
            },
            post: function (scope, elem) {
              // Set opened as false
              scope.opened = false;
              scope.active = false;

              // Init as component
              let  component = new Component(scope, scope.optionId);
              if (!component.asComponent()) {
                // If component initialization is wrong, cancel initialization
                return false;
              }

              // Check if option has children
              if (component.controller) {
                component.controller.hasChildren = component.controller.contextMenu.length > 0;
                component.controller.separator = scope.option?.separator;
              } else {
                component.controller = {
                  hasChildren: false,
                  separator: scope.option?.separator
                };
              }

              /**
               * Basic getSpecificFields function (To be overwritten on complex directives)
               * @returns {Object} Specific fields from component
               */
              component.getSpecificFields = function () {
                return {buttonValue: component.model.selected, buttonAddress: component.address};
              };

              const params = {timer: null, elem, component};

              /**
               * Click option function
               */
              scope.onClick = function () {
                if (!scope.controller.disabled) {
                  ActionController.addActionList(component.controller.actions, true, {address: component.address, context: component.context});
                  component.storeEvent('click');
                  scope.$emit("hideContextMenu");
                }
              };

              /**
               * Check if option is disabled
               * @returns {boolean} option is disabled
               */
              scope.isDisabled = function () {
                return Storage.get("actions-running") ||
                    scope.$root.status.loading ||
                    component.controller?.disabled;
              };

              // Disable option context menu
              elem.on('contextmenu mouseup', function (event) {
                // Cancel event propagation
                Utilities.stopPropagation(event, true);
              });

              // Initialize link and submenu layers
              Utilities.timeout(() => initLayers(params, Utilities));
            }
          };
        }
      };
    }]);
