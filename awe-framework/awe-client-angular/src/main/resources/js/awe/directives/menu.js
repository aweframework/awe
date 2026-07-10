import {aweApplication} from "../awe";
import {ClientActions} from "../data/actions";
import {searchOptions} from "../data/menuSearch";
import _ from "lodash";

function getResolutionType(width) {
  if (width <= 640) {
    return "mobile";
  } else if (width <= 768) {
    return "tablet";
  } else {
    return "desktop";
  }
}

// Menu directive
aweApplication.directive('aweMenu',
  ['ServerData', 'Component', '$document', '$window', '$filter', '$location', 'AweUtilities', 'ActionController', '$translate', 'AweSettings',
    function (serverData, Component, $document, $window, $filter, $location, Utilities, $actionController, $translate, $settings) {
      return {
        restrict: 'E',
        replace: false,
        template:
          `<ul class="awe-menu {{::controller.style}}" ng-class="{'menu-minimized': status.minimized, 'ng-hide': !isVisible()}" ng-cloak>
            <div class="awe-menu-search awe-menu-search-{{::menuType}}" ng-class="{'open': search.open}" ng-if="searchEnabled && isVisible()" ng-cloak>
              <button type="button" class="awe-menu-search-toggle" ng-click="toggleSearch()"
                      title="{{'BUTTON_SEARCH'| translateMultiple}}" aria-label="{{'BUTTON_SEARCH'| translateMultiple}}">
                <i class="fa fa-search"></i>
              </button>
              <div class="awe-menu-search-panel" ng-if="search.open">
                <input type="text" class="awe-menu-search-input form-control" ng-model="search.query" ng-change="onSearchChange()"
                       ng-keydown="onSearchKeydown($event)" placeholder="{{'BUTTON_SEARCH'| translateMultiple}}"/>
                <ul class="awe-menu-search-results" ng-if="search.results.length">
                  <li ng-repeat="entry in search.results track by $index" ng-class="{'active': $index === search.active}"
                      ng-click="selectResult(entry)" ng-mouseenter="search.active = $index">
                    <span class="awe-menu-search-label">{{entry.label}}</span>
                    <span class="awe-menu-search-breadcrumb" ng-if="entry.breadcrumb">{{entry.breadcrumb}}</span>
                  </li>
                </ul>
                <div class="awe-menu-search-empty" ng-if="search.query && !search.results.length">{{'MENU_SEARCH_EMPTY'| translateMultiple}}</div>
              </div>
            </div>
            <awe-option ng-repeat="option in options| allowedOption track by option.id" controller="option" status="status" on-option-click="onOptionClick()" menu-type="{{::menuType}}"
                        close-first-level="closeFirstLevel()" first-level="true" selected-option="selectedOption" option-title="{{::option.title}}" option-name="{{::option.name}}"
                        option-style="{{::option.style}}" option-icon="{{::option.icon}}" option-text="{{::option.label}}"></awe-option>
          </ul>`,
        scope: {
          'menuId': '@'
        },
        link: function (scope, element) {
          // Init as component
          let $body = $("body");
          let component = new Component(scope, scope.menuId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          // Initialize options
          scope.menuType = scope.controller.style && scope.controller.style.indexOf("horizontal") !== -1 ? "horizontal" : "vertical";
          let isMinimized = $body.hasClass("mmc");
          scope.visible = isMinimized ? scope.menuType !== "horizontal" : true;
          scope.status = {minimized: isMinimized, animating: false, resolution: "desktop"};
          scope.options = [];
          scope.selectedOption = {name: "", opened: {}};
          scope.search = {open: false, query: "", results: [], active: -1};
          // Menu option search is enabled unless the 'menuSearchEnabled' setting is explicitly false
          scope.searchEnabled = $settings.get("menuSearchEnabled") !== false;

          // Update children
          if (component.controller && "options" in component.controller) {
            scope.options = component.controller.options;
          }

          /******************************************************************************
           * SCOPE METHODS
           *****************************************************************************/

          /**
           * Act in case of option click
           */
          scope.onOptionClick = function () {
            switch (scope.menuType) {
              case "horizontal":
                scope.closeFirstLevel();
                break;

              case "vertical":
              default:
                switch (scope.status.resolution) {
                  case "mobile":
                    if (!scope.status.minimized) {
                      component.toggleMenu();
                    }
                    break;
                  case "tablet":
                  case "desktop":
                  default:
                    if (scope.status.minimized) {
                      scope.closeFirstLevel();
                    }
                    break;
                }
            }
          };

          /**
           * Check if option is opened
           */
          scope.isVisible = function () {
            return scope.visible && hasVisibleChildren();
          };

          /**
           * Clear first level opened options
           */
          scope.closeFirstLevel = function () {
            for (let option in scope.selectedOption.opened) {
              if (scope.selectedOption.opened[option]) {
                delete scope.selectedOption.opened[option];
              }
            }
          };

          /**
           * Toggle the option search panel
           */
          scope.toggleSearch = function () {
            if (scope.search.open) {
              scope.closeSearch();
            } else {
              scope.openSearch();
            }
          };

          /**
           * Open the option search panel and focus the input
           */
          scope.openSearch = function () {
            scope.search.open = true;
            Utilities.timeout(function () {
              let input = $(element).find(".awe-menu-search-input");
              if (input.length) {
                input[0].focus();
              }
            });
          };

          /**
           * Close the option search panel and reset its state
           */
          scope.closeSearch = function () {
            scope.search.open = false;
            scope.search.query = "";
            scope.search.results = [];
            scope.search.active = -1;
          };

          /**
           * Recompute search results from the current query
           */
          scope.onSearchChange = function () {
            let isAllowed = (option) => $filter("allowedOption")([option]).length > 0;
            scope.search.results = searchOptions(scope.options, scope.search.query, {
              translate: (key) => $translate.instant(key),
              isAllowed: isAllowed
            }).map((entry) => ({
              option: entry.option,
              label: entry.path[entry.path.length - 1],
              breadcrumb: entry.path.slice(0, -1).join(" › ")
            }));
            scope.search.active = scope.search.results.length ? 0 : -1;
          };

          /**
           * Keyboard navigation inside the search panel
           * @param {object} event Keydown event
           */
          scope.onSearchKeydown = function (event) {
            let results = scope.search.results;
            switch (event.keyCode) {
              case 40: // Arrow down
                if (results.length) {
                  scope.search.active = (scope.search.active + 1) % results.length;
                  event.preventDefault();
                }
                break;
              case 38: // Arrow up
                if (results.length) {
                  scope.search.active = (scope.search.active - 1 + results.length) % results.length;
                  event.preventDefault();
                }
                break;
              case 13: // Enter
                if (results.length) {
                  scope.selectResult(results[Math.max(scope.search.active, 0)]);
                  event.preventDefault();
                }
                break;
              case 27: // Escape
                scope.closeSearch();
                event.preventDefault();
                break;
              default:
                break;
            }
          };

          /**
           * Launch the selected option and close the search panel
           * @param {object} entry Search result entry
           */
          scope.selectResult = function (entry) {
            let option = entry && entry.option;
            if (option && option.actions) {
              $actionController.closeAllActions();
              $actionController.addActionList(option.actions, true, {});
              scope.$emit("optionClicked");
            }
            scope.closeSearch();
          };

          /******************************************************************************
           * COMPONENT METHODS
           *****************************************************************************/

          /**
           * Toggle menu to visible or not visible
           * @param {object || null} action Action received
           */
          component.toggleMenu = function (action= null) {
            // Toggle visibility depending on menu type
            // Change menu visibility
            if (scope.menuType === "horizontal") {
              scope.visible = !scope.visible;
              // Toggle mmc class to body
              $body.toggleClass("mmc", !scope.visible);
            } else {
              // Close first level first (if resolution is tablet)
              scope.status.minimized = !scope.status.minimized;
              scope.status.animating = true;
              $body.toggleClass("mmc", scope.status.minimized);
              switch (scope.status.resolution) {
                case "tablet":
                case "mobile":
                  $body.toggleClass("mme", !scope.status.minimized);
                  break;
                default:
                  $body.toggleClass("mme", false);
                  break;
              }

              // Close first level if not mobile
              if (scope.status.resolution !== "mobile") {
                scope.closeFirstLevel();
              }
            }

            // Finish toggle action
            Utilities.timeout(function () {
              component.onEndToggleMenu(action);
            }, 250);
          };

          /**
           * Finish toggle menu visibility
           * @param {object || null} action Action received
           */
          component.onEndToggleMenu = function (action) {
            // End minimizing
            scope.status.animating = false;

            // Set resizing
            scope.status.resizing = true;

            // Publish layout event
            Utilities.publish("resize-action");

            // Finish screen action
            if (action) {
              $actionController.acceptAction(action);
            }
          };

          /**
           * Toggle navigation bar to visible or not visible
           * @param {object} action Action received
           */
          component.toggleNavbar = function (action) {
            // main-navbar-collapse
            let node = angular.element($('#main-navbar-collapse')[0]);
            if (node.hasClass('collapse')) {
              node.removeClass('collapse');
            } else {
              node.addClass('collapse');
            }
            // Finish action
            $actionController.acceptAction(action);
          };

          /**
           * Change the menu
           * @param {Object} action Action parameters
           */
          component.changeMenu = function (action) {
            let parameters = action.attr("parameters");
            scope.options = parameters.options;
            // Finish action
            $actionController.acceptAction(action);
          };

          /******************************************************************************
           * SERVICE METHODS
           *****************************************************************************/

          /**
           * Check if option is opened
           */
          function hasVisibleChildren() {
            return $filter('allowedOption')(scope.options).length > 0;
          }

          /**
           * Checks if dropdown is visible
           * @returns {Boolean} Dropdown is visible or not
           */
          function isDropdownVisible() {
            let visible = false;
            for (let option in scope.selectedOption.opened) {
              if (scope.selectedOption.opened[option]) {
                visible = true;
              }
            }
            return visible;
          }

          /**
           * Finish toggle menu visibility
           */
          function onResize() {
            scope.status.resolution = getResolutionType($window.innerWidth);
            if (scope.menuType === "vertical") {
              // Tablet size
              switch (scope.status.resolution) {
                case "tablet":
                  if (!scope.status.minimized) {
                    component.toggleMenu();
                  } else {
                    scope.closeFirstLevel();
                  }
                  break;
                case "mobile":
                  if (!scope.status.minimized) {
                    component.toggleMenu();
                  }
                  break;
                case "desktop":
                default:
                  if (scope.status.minimized) {
                    scope.closeFirstLevel();
                  }
                  break;
              }
            }
          }

          // Watch click event if menu is dropdown
          $document.bind('click', (event) => {
            // Close the search panel when clicking outside of it
            if (scope.search.open) {
              let searchElement = $(element).find(".awe-menu-search");
              let isClickInsideSearch = searchElement.find(event.target).length > 0 || searchElement.is(event.target);
              if (!isClickInsideSearch) {
                Utilities.timeout(() => scope.closeSearch());
              }
            }
            if (scope.menuType === "horizontal" ||
              (scope.status.minimized && scope.status.resolution !== "mobile")) {
              let isClickedElementChildOfDropdown = $(element).find(event.target).length > 0;
              if (!isClickedElementChildOfDropdown && isDropdownVisible()) {
                Utilities.timeout(() => {
                  scope.closeFirstLevel();
                });
              }
            }
          });

          /******************************************************************************
           * EVENT LISTENERS
           *****************************************************************************/
          let listeners = {};
          // Action listener definition
          _.each(ClientActions.menu, function (actionOptions, actionId) {
            listeners[actionId] = scope.$on("/action/" + actionId, function (event, action) {
              return component[actionOptions.method](action, scope);
            });
          });

          listeners['watchLocation'] = scope.$watch(function () {
            return $location.path();
          }, function (newLocation) {
            scope.selectedOption.name = newLocation.substring(newLocation.lastIndexOf("/") + 1, newLocation.length);
          });

          listeners['optionClick'] = scope.$on('optionClicked', function () {
            scope.onOptionClick();
          });

          listeners['resize'] = scope.$on('resize', function () {
            if (scope.status.resizing) {
              scope.status.resizing = false;
            } else {
              onResize();
            }
          });

          // Remove all listeners on unload
          listeners['resize'] = scope.$on("$destroy", function () {
            // Remove body classes
            $body.removeClass("mme");
            Utilities.clearListeners(listeners);
          });

          // Call onResize on load
          onResize();
        }
      };
    }
  ]);
