import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";

const template = `<li class="awe-option {{::getStaticOptionClasses()}}" ng-class="getOptionClasses()" ng-cloak>
  <a ng-if="::!controller.separator" title="{{::optionTitle| translateMultiple}}" name="{{::optionName}}" class="{{::optionStyle}}"
     ng-click="optionClick()">
    ${getIconTemplate("menu-icon")}
    <span ng-if="::optionText" class="mm-text" translate-multiple="{{::optionText}}"></span>
  </a>
  <ul ng-if="::hasVisibleChildren()" class="{{::getStaticSubmenuClasses()}}" ng-class="getSubmenuClasses()">
    <div ng-if="::optionText" class="mmc-title" translate-multiple="{{::optionText}}"></div>
    <awe-option ng-repeat="option in controller.options| allowedOption track by option.id" controller="option" status="status" on-option-click="onOptionClick()" menu-type="{{::menuType}}" close-first-level="closeFirstLevel()" first-level="false" selected-option="selectedOption" option-title="{{::option.title}}" option-name="{{::option.name}}" option-style="{{::option.style}}" option-icon="{{::option.icon}}" option-text="{{::option.label}}"></awe-option>
  </ul>
</li>`;

// Option directive
aweApplication.directive('aweOption',
  ['ServerData', 'ActionController', '$compile', '$filter', 'AweUtilities',
    function (serverData, ActionController, $compile, $filter, $util) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          'optionName': '@',
          'optionTitle': '@',
          'optionStyle': '@',
          'optionIcon': '@',
          'optionText': '@',
          'controller': '=',
          'selectedOption': '=',
          'firstLevel': '=',
          'closeFirstLevel': '&',
          'menuType': '@',
          'onOptionClick': '&',
          'status': '='
        },
        compile: function (tElem) {
          let  contents = tElem.contents().remove();
          let  compiledContents;

          return {
            pre: function (scope, elem) {
              // Set opened as false
              scope.opened = false;
              scope.active = false;

              // Check if the option should be expanded initially (only for vertical menus)
              if (scope.controller && scope.controller.expanded && scope.menuType === "vertical") {
                scope.opened = true;
                if (!scope.selectedOption.opened) {
                  scope.selectedOption.opened = {};
                }
                scope.selectedOption.opened[scope.optionName] = scope.firstLevel;
              }

              /**
               * Check if option is opened
               */
              scope.hasVisibleChildren = function () {
                return $filter('allowedOption')(scope.controller.options).length > 0 &&
                  !scope.controller.separator &&
                  !scope.controller.menuScreen;
              };

              /**
               * Get option icon
               */
              scope.getIcon = function () {
                return $util.extractIcon(scope.optionIcon);
              };

              /**
               * Check if option is opened
               */
              let  isOpened = function () {
                return scope.optionName in scope.selectedOption.opened;
              };

              /**
               * Check if option is opened
               */
              let  isFloating = function () {
                return scope.firstLevel && scope.status.resolution !== 'mobile';
              };

              /**
               * Static option classes
               * @returns {Array}
               */
              scope.getStaticOptionClasses = function () {
                let  classes = [];

                // Visible children
                if (scope.hasVisibleChildren()) {
                  classes.push('mm-dropdown');
                }

                // First level
                if (scope.firstLevel) {
                  classes.push('mm-dropdown-root');
                }

                // Separator
                if (scope.controller.separator) {
                  classes.push('divider');
                }

                return classes.join(" ");
              };

              /**
               * Dynamic option classes
               * @returns {Array}
               */
              scope.getOptionClasses = function () {
                let  classes = [];
                // Opened status
                if (isOpened()) {
                  if (isFloating() && scope.status.minimized) {
                    classes.push('mmc-dropdown-open');
                  } else {
                    classes.push('open');
                  }
                }

                // Active status
                if (scope.selectedOption.name === scope.optionName) {
                  classes.push('active');
                }

                return classes.join(" ");
              };

              /**
               * Static submenu classes
               * @returns {Array}
               */
              scope.getStaticSubmenuClasses = function () {
                let  classes = [];

                // First level classes
                if (scope.firstLevel) {
                  classes.push('mm-dropdown-first');
                  classes.push('menu-shadow');
                } else {
                  classes.push('mm-dropdown-target');
                }

                return classes.join(" ");
              };

              /**
               * Dynamic submenu classes
               * @returns {Array}
               */
              scope.getSubmenuClasses = function () {
                let  classes = [];

                // Opened status
                if (isOpened()) {
                  if (isFloating() && scope.status.minimized) {
                    classes.push('mmc-dropdown-open-ul');
                  } else {
                    classes.push('opened');
                  }
                }

                // Hide if not is opened or is floating
                if (!(isOpened() || isFloating())) {
                  classes.push("ng-hide");
                }

                return classes.join(" ");
              };

              if (!compiledContents) {
                compiledContents = $compile(contents);
              }
              compiledContents(scope, function (clone) {
                elem.append(clone);
              });
            },
            post: function (scope) {
              /**
               * Click button function
               */
              scope.optionClick = function () {
                if (scope.hasVisibleChildren()) {
                  // Node is a branch
                  if (scope.optionName in scope.selectedOption.opened) {
                    // Node is an opened branch
                    scope.closeOption();
                  } else {
                    // Node is a closed branch
                    if (scope.firstLevel) {
                      scope.closeFirstLevel();
                    }
                    scope.opened = true;
                    scope.selectedOption.opened[scope.optionName] = scope.firstLevel;
                  }
                } else if ("actions" in scope.controller) {
                  // Node is a leaf
                  // Close all previous actions
                  ActionController.closeAllActions();

                  // All action list to stack
                  ActionController.addActionList(scope.controller.actions, true, {address: scope.address, context: scope.context});

                  // Emit option clicked
                  scope.$emit("optionClicked");
                }
              };

              /**
               * Closes the option and marks option as closed after animation
               */
              scope.closeOption = function () {
                delete scope.selectedOption.opened[scope.optionName];
                scope.opened = false;
              };
            }
          };
        }
      };
    }]);
