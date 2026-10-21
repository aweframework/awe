import {aweApplication} from "../awe";
import "./plugins/uiModal";
import {getIconTemplate} from "../services/component";
import {createFocusTrap} from "../services/focusTrap";
import {TestIds} from "../data/testIds";
import {initDialogComponent} from "./dialog";

const DEFAULT_MAX_WIDTH = "40rem";

const template = `<div ng-attr-id="{{::controller.id}}" ng-cloak>
  <div class="modal fade side-panel side-panel-{{::position}}" ui-modal on-close="component.closeDialog()" data-testid="${TestIds.sidePanel}" ng-attr-data-testid-owner="{{::controller.id}}" role="dialog" aria-modal="true" ng-cloak>
    <div class="modal-dialog {{::controller.style}}" ng-style="::{'max-width': maxWidth}">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" data-testid="${TestIds.sidePanelClose}">&times;</button>
          <h4 class="modal-title">
            ${getIconTemplate("panel-title-icon")}
            <span translate-multiple="{{::controller.label}}"></span>
          </h4>
        </div>
        <div class="side-panel-body" ng-transclude></div>
      </div>
    </div>
  </div>
</div>`;

// Side panel directive: a dialog anchored to the left or right edge of the screen (#843)
aweApplication.directive('aweSidePanel',
  ['Component', 'ActionController', 'AweUtilities',
    function (Component, $actionController, Utilities) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          sidePanelId: '@'
        },
        link: function (scope, element) {
          const component = initDialogComponent(scope, scope.sidePanelId, {Component, $actionController, Utilities});
          if (!component) {
            return false;
          }

          const controller = component.controller || {};
          scope.position = controller.position === "left" ? "left" : "right";
          scope.maxWidth = controller.maxWidth || DEFAULT_MAX_WIDTH;

          // Keep the focus inside while the panel is shown, and give it back to the control which opened it
          const modal = element.find(".modal");
          const trap = createFocusTrap(() => modal[0]);
          modal.on("show.bs.modal", () => trap.activate());
          modal.on("shown.bs.modal", () => trap.focusFirst());
          modal.on("hidden.bs.modal", () => trap.deactivate());
          scope.$on("$destroy", () => {
            trap.deactivate();
            modal.off("show.bs.modal shown.bs.modal hidden.bs.modal");
          });
        }
      };
    }
  ]);
