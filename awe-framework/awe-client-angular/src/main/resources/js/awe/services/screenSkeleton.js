import {aweApplication} from "../awe";

/**
 * Screen loading skeleton state: the view that is loading between the state change start and the screen initialisation
 * @ngdoc service
 * @name ScreenSkeleton
 */
aweApplication.factory('ScreenSkeleton', [
  function () {
    const BASE_VIEW = "base";
    const REPORT_VIEW = "report";
    let loadingView = null;

    /**
     * Retrieve the parent state name
     * @param {object} state State
     * @returns {string} Parent state name
     */
    const getParentName = state => ((state && state.name) || "").split(".")[0];

    return {
      /**
       * Show the skeleton over a view
       * @param {string} view View that is loading
       */
      show: function (view) {
        loadingView = view || BASE_VIEW;
      },
      /**
       * Hide the skeleton
       */
      hide: function () {
        loadingView = null;
      },
      /**
       * Whether a screen is loading
       * @returns {boolean} Skeleton visible
       */
      isVisible: function () {
        return loadingView !== null;
      },
      /**
       * Retrieve the view that is loading
       * @returns {string|null} Loading view
       */
      getView: function () {
        return loadingView;
      },
      /**
       * Retrieve the view a state change reloads: the report view when only the sub screen changes, the base view
       * otherwise
       * @param {object} toState Target state
       * @param {object} toParams Target parameters
       * @param {object} fromState Source state
       * @param {object} fromParams Source parameters
       * @returns {string} Loading view
       */
      getLoadingView: function (toState, toParams, fromState, fromParams) {
        const reportOnly = REPORT_VIEW in ((toState && toState.views) || {}) &&
          getParentName(toState) === getParentName(fromState) &&
          (toParams || {}).screenId === (fromParams || {}).screenId;
        return reportOnly ? REPORT_VIEW : BASE_VIEW;
      }
    };
  }
]);
