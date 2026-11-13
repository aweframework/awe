import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {TestIds} from "../data/testIds";

const template = `<li class="awe-option {{::getStaticOptionClasses()}}" ng-class="getOptionClasses()" data-testid="${TestIds.menuOption}"
    ng-attr-data-active="{{isActive()}}" ng-attr-data-open="{{isOpen()}}" ng-mouseenter="openFlyout(); describe()" ng-mouseleave="closeFlyout()" ng-cloak>
  <a ng-if="::!controller.separator" ng-attr-title="{{getTitle()}}" ng-attr-aria-describedby="{{getDescribedBy()}}" name="{{::optionName}}" class="{{::optionStyle}}"
     data-testid="${TestIds.menuLink}" ng-click="optionClick()" ng-focus="describe()">
    ${getIconTemplate("menu-icon")}
    <span ng-if="::optionText" class="mm-text" translate-multiple="{{::optionText}}"></span>
  </a>
  <ul ng-if="::hasVisibleChildren()" class="{{::getStaticSubmenuClasses()}}" ng-class="getSubmenuClasses()"
      ng-attr-data-testid="{{::firstLevel ? '${TestIds.menuDropdown}' : '${TestIds.menuSubmenu}'}}" ng-attr-data-open="{{isOpen()}}">
    <div ng-if="::optionText" class="mmc-title" translate-multiple="{{::optionText}}"></div>
    <awe-option ng-repeat="option in controller.options| allowedOption track by option.id" controller="option" status="status" on-option-click="onOptionClick()" menu-type="{{::menuType}}" close-first-level="closeFirstLevel()" first-level="false" flyout-option="firstLevel ? controller : flyoutOption" selected-option="selectedOption" option-title="{{::option.description || option.title}}" option-name="{{::option.name}}" option-style="{{::option.style}}" option-icon="{{::option.icon}}" option-text="{{::option.label}}"></awe-option>
    <div ng-if="firstLevel && areaText()" id="{{::getAreaId()}}" class="awe-menu-description">{{areaText()| translateMultiple}}</div>
  </ul>
</li>`;

// Time the pointer has to come back before the flyout of the collapsed menu closes, as the prototype
const FLYOUT_CLOSE_DELAY = 220;

// Option directive
aweApplication.directive('aweOption',
  ['ServerData', 'ActionController', '$compile', '$filter', 'AweUtilities', 'AweSettings',
    function (serverData, ActionController, $compile, $filter, $util, $settings) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          'optionName': '@',
          'flyoutOption': '<',
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
                return !!scope.selectedOption && !!scope.selectedOption.opened && scope.optionName in scope.selectedOption.opened;
              };

              /**
               * Test hooks: state of the option as boolean values. They run on every digest, so they never fail when
               * the selected option is not set yet, and an option without name (separator) is never active
               */
              scope.isOpen = () => isOpened();
              scope.isActive = () => !!scope.optionName && !!scope.selectedOption && scope.selectedOption.name === scope.optionName;

              /**
               * The options are described by an area of the flyout of the collapsed menu (panel mode), which replaces
               * the hint of their link. The option of the rail itself is outside its flyout: it keeps its hint
               */
              const inFlyout = () => $settings.get("menuDescription") === "panel" && !!scope.status && scope.status.minimized &&
                scope.status.resolution !== 'mobile';
              const findOption = (options, name) => {
                for (const option of $filter('allowedOption')(options || [])) {
                  const found = option.name === name ? option : findOption(option.options, name);
                  if (found) {
                    return found;
                  }
                }
                return null;
              };
              const getDescribed = () => scope.selectedOption?.described;

              /**
               * Hint of the option link: its description, unless the flyout describes it
               */
              scope.getTitle = () => (scope.firstLevel || !inFlyout() ? $filter('translateMultiple')(scope.optionTitle) : "") || undefined;

              /**
               * Option of the flyout that holds this option: the first level one
               */
              const flyoutOption = () => (scope.firstLevel ? scope.controller : scope.flyoutOption);

              /**
               * Description shown by the area of the flyout while an option is described: the option under the pointer
               * or with the focus, or else the option of the flyout
               */
              const areaDescription = (described) => {
                const flyout = flyoutOption();
                const hovered = described && flyout && described.flyout === flyout.name ?
                  findOption(flyout.options, described.name) : null;
                return hovered?.description || flyout?.description || "";
              };

              /**
               * Id of the description area of the flyout of the option
               */
              scope.getAreaId = () => `awe-menu-description-${flyoutOption().name}`;

              /**
               * Area that describes the option link under the pointer or with the focus, while its flyout is open
               */
              scope.getDescribedBy = () => {
                const described = getDescribed();
                return inFlyout() && !scope.firstLevel && described && described.name === scope.optionName &&
                  areaDescription(described) ? scope.getAreaId() : undefined;
              };

              /**
               * Describe this option in the area of its flyout
               */
              scope.clearDescribed = () => {
                const described = getDescribed();
                if (described && described.flyout === scope.optionName) {
                  delete scope.selectedOption.described;
                }
              };
              scope.describe = function () {
                if (inFlyout() && !scope.firstLevel && scope.selectedOption) {
                  scope.selectedOption.described = {name: scope.optionName, flyout: scope.flyoutOption.name};
                }
              };

              /**
               * Description shown by the area of the flyout of a first level option. Empty when the flyout is closed or
               * when neither the option nor its flyout have a description
               */
              scope.areaText = () => inFlyout() && isOpened() ? areaDescription(getDescribed()) : "";

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
                    if ($settings.get("menuFlyoutPlacement") === "full-height") {
                      classes.push('mmc-dropdown-full-height');
                    }
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
            post: function (scope, elem) {
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
                  ActionController.addActionList(scope.controller.actions.map(a => ({...a, origin: "menu"})), true, {address: scope.address, context: scope.context});

                  // Emit option clicked
                  scope.$emit("optionClicked");
                }
              };

              /**
               * Closes the option and marks option as closed after animation
               */
              scope.closeOption = function () {
                delete scope.selectedOption.opened[scope.optionName];
                scope.clearDescribed();
                scope.opened = false;
              };

              // Collapsed menu, as the prototype: the options of a first level option open as a flyout while the pointer
              // is on it (or on the flyout, which is inside the option), and close a moment after it leaves
              let flyoutTimer = null;
              const isFlyout = () => scope.firstLevel && scope.status.resolution !== 'mobile' && scope.status.minimized &&
                scope.hasVisibleChildren();
              const isFlyoutOpen = () => !!scope.selectedOption?.opened && scope.optionName in scope.selectedOption.opened;

              /**
               * Open the flyout of the option under the pointer, instead of the open one
               */
              scope.openFlyout = function () {
                if (!isFlyout()) {
                  return;
                }
                $util.timeout.cancel(flyoutTimer);
                if (!isFlyoutOpen()) {
                  scope.closeFirstLevel();
                  scope.opened = true;
                  scope.selectedOption.opened[scope.optionName] = scope.firstLevel;
                  delete scope.selectedOption.described;
                  placeFlyout();
                }
              };

              /**
               * With the full-height placement, the flyout starts at the top of the menu (the style gives it the height
               * down to the bottom of the window)
               */
              const placeFlyout = () => {
                const menu = elem[0].closest("ul.awe-menu");
                const flyout = elem[0].querySelector(":scope > ul");
                if (menu && flyout && $settings.get("menuFlyoutPlacement") === "full-height") {
                  flyout.style.setProperty("--_flyout-top", `${menu.getBoundingClientRect().top}px`);
                }
              };

              /**
               * Close the flyout a moment after the pointer leaves the option
               */
              scope.closeFlyout = function () {
                if (isFlyout() && isFlyoutOpen()) {
                  $util.timeout.cancel(flyoutTimer);
                  flyoutTimer = $util.timeout(() => scope.closeOption(), FLYOUT_CLOSE_DELAY);
                }
              };

              scope.$on("$destroy", () => $util.timeout.cancel(flyoutTimer));
            }
          };
        }
      };
    }]);
