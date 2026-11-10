import {aweApplication} from "../awe";
import {getBreadcrumbLevels, originOf} from "awe-client-react/utilities/breadcrumbs";
import {findOptionPath} from "awe-client-react/utilities/menuLaunch";

/**
 * Find an option of the menu by its name
 * @param {object[]} options Options
 * @param {string} name Option name
 * @returns {object|null} Option
 */
function findOption(options, name) {
  for (const option of options || []) {
    const found = option.name === name ? option : findOption(option.options, name);
    if (found) {
      return found;
    }
  }
  return null;
}

// Parameters of a state that do not tell the screen: the same screen with them different is a reload
const NOT_SCREEN = ["freshScreen", "r"];
const screenOf = params => Object.fromEntries(Object.entries(params || {}).filter(([name]) => !NOT_SCREEN.includes(name)));

// Breadcrumb service: what the levels of the window breadcrumb do. The menu attaches itself, because it holds the
// options (and their actions) and knows how to launch one and, the vertical one, how to open at a group.
// It also keeps the screens the user came from (the origin): a screen opened from another screen, not from the menu,
// shows them in its breadcrumb; the menu and the home level start again without them
aweApplication.factory("Breadcrumb",
  ['$state', '$location', '$rootScope', 'AweSettings', 'AweUtilities',
    /**
     * Breadcrumb service
     * @param {object} $state State service
     * @param {object} $location Location service
     * @param {object} $rootScope Root scope service
     * @param {object} $settings Settings service
     * @param {object} $utilities Utilities service
     * @returns {object} Service
     */
    function ($state, $location, $rootScope, $settings, $utilities) {
      let menu = null;
      // Levels of the screens the screen was opened from (not from the menu), outermost first
      let origin = [];
      // Levels and address of the screen that is open: what the next screen keeps of it
      let current = null;

      const $breadcrumb = {
        /**
         * Attach the menu of the application
         * @param {{options: function(): object[], launch: function(object), expand?: function(string[])}} newMenu Menu
         * @returns {function} Function that detaches it
         */
        attachMenu: function (newMenu) {
          menu = newMenu;
          return () => {
            menu = menu === newMenu ? null : menu;
          };
        },
        /**
         * Levels of the screen that is open: the screens it was opened from, the levels of its menu path and the screen
         * @param {object[]} levels Levels of the menu above the screen
         * @param {string} title Title of the screen
         * @returns {object[]} Levels, outermost first
         */
        getLevels: function (levels, title) {
          return getBreadcrumbLevels(origin, {items: [...levels, {label: title}]}, undefined, $location.url());
        },
        /**
         * Publish the levels of the screen that is open, so the screen that follows keeps them
         * @param {object[]} levels Levels shown
         * @param {string} [option] Name of the option of the screen (empty when it is not in the menu)
         */
        show: function (levels, option) {
          current = {levels, option, location: $location.url()};
        },
        /**
         * Check if a level can be followed: a screen the user came from, an option the menu launches, or a group the
         * menu opens at
         * @param {{name?: string, group?: boolean, path?: string}} level Level
         * @returns {boolean} The level is a link
         */
        isLink: function (level) {
          if (level.path) {
            return true;
          }
          if (!menu || !level.name) {
            return false;
          }
          if (level.group) {
            return !!menu.expand && !!findOptionPath(menu.options(), level.name);
          }
          return !!findOption(menu.options(), level.name)?.actions?.length;
        },
        /**
         * Follow a level: go back to its screen, launch its option like the menu does, or open the menu at its group
         * @param {{name?: string, group?: boolean, path?: string}} level Level
         */
        follow: function (level) {
          if (!$breadcrumb.isLink(level)) {
            return;
          }
          if (level.path) {
            $location.url(level.path);
          } else if (level.group) {
            menu.expand(findOptionPath(menu.options(), level.name));
          } else {
            origin = [];
            menu.launch(findOption(menu.options(), level.name));
          }
        },
        /**
         * Go to the start screen, as a menu option does
         */
        goHome: function () {
          origin = [];
          const state = $utilities.getState($settings.get("initialURL") || "");
          $state.go(state.to, {...state.parameters, freshScreen: true}, {reload: false, inherit: true, notify: true, location: true});
        }
      };

      // The screen that is left is read and forgotten when the transition starts, before the view of the next screen exists:
      // the next screen publishes its levels whenever it is rendered, and nothing later can erase them
      $rootScope.$on("$stateChangeStart", (event, toState, toParams, fromState, fromParams) => {
        if (toParams?.freshScreen === true) {
          // A menu launch (or the home level) starts again
          origin = [];
        } else if (current && !(toState === fromState && angular.equals(screenOf(toParams), screenOf(fromParams)))) {
          // The screen is left for another one that the menu did not launch: it becomes the last origin level. A screen
          // that shows no breadcrumb (current is null) and a reload of the same screen leave the origin as it is
          origin = originOf(current.levels, current.location, current.option || undefined);
        }
        current = null;
      });

      return $breadcrumb;
    }]);
