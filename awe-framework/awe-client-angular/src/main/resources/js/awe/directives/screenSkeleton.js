import {aweApplication} from "../awe";
import "../services/screenSkeleton";

const SCREEN_SKELETON_TEMPLATE = `<div class="screen-skeleton" ng-if="isVisible()" ng-style="position" aria-busy="true">
  <div class="screen-skeleton-title"><span class="skeleton-bar"></span></div>
  <div class="screen-skeleton-criteria">
    <span class="skeleton-bar" ng-repeat="criterion in ::criteria track by $index"></span>
  </div>
  <div class="screen-skeleton-grid">
    <div class="screen-skeleton-grid-header"><span class="skeleton-bar"></span></div>
    <div class="screen-skeleton-grid-row" ng-repeat="row in ::rows track by $index"><span class="skeleton-bar"></span></div>
  </div>
</div>`;

/**
 * Screen loading skeleton: a generic screen (title bar, a row of criteria and a grid) over the view that is loading
 * @ngdoc directive
 * @name aweScreenSkeleton
 */
aweApplication.directive('aweScreenSkeleton',
  ['ScreenSkeleton', '$window',
    /**
     * @param {object} ScreenSkeleton Screen skeleton state
     * @param {object} $window Window
     */
    function (ScreenSkeleton, $window) {
      const BUSY = "aria-busy";
      return {
        restrict: 'E',
        scope: {},
        template: SCREEN_SKELETON_TEMPLATE,
        link: function (scope) {
          let target = null;
          scope.criteria = Array.from({length: 4});
          scope.rows = Array.from({length: 6});
          scope.isVisible = () => ScreenSkeleton.isVisible();

          /**
           * Retrieve the element of the view that is loading (the base view when the view is not rendered)
           * @param {string} view View name
           * @returns {Element|null} View element
           */
          const getViewElement = view => document.querySelector(`[ui-view='${view}']`) || document.querySelector("[ui-view='base']");

          /**
           * Place the skeleton over the view that is loading
           */
          const place = () => {
            const rect = target ? target.getBoundingClientRect() : null;
            scope.position = rect && rect.width > 0 && rect.height > 0 ?
              {top: `${rect.top}px`, left: `${rect.left}px`, width: `${rect.width}px`, height: `${rect.height}px`} : {};
          };

          /**
           * Clear the busy state of the view
           */
          const release = () => {
            if (target) {
              target.removeAttribute(BUSY);
              target = null;
            }
          };

          const onResize = () => scope.$evalAsync(place);

          scope.$watch(() => ScreenSkeleton.getView(), view => {
            release();
            if (view) {
              target = getViewElement(view);
              if (target) {
                target.setAttribute(BUSY, "true");
              }
              place();
            }
          });

          angular.element($window).on("resize", onResize);
          scope.$on("$destroy", () => {
            release();
            angular.element($window).off("resize", onResize);
          });
        }
      };
    }
  ]);
