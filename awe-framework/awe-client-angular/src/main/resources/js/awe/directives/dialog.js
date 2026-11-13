import {aweApplication} from "../awe";
import "./plugins/uiModal";
import {getIconTemplate} from "../services/component";
import {TestIds} from "../data/testIds";

const template = `<div ng-attr-id="{{::controller.id}}" ng-cloak>
  <div class="modal fade" ui-modal on-close="component.closeDialog()" data-testid="${TestIds.dialog}" ng-attr-data-testid-owner="{{::controller.id}}" ng-cloak>
    <div class="modal-dialog {{::controller.style}}"  ng-class="::{'fullHeight expandible-vertical': isExpandible}">
      <div class="modal-content" ng-class="::{'expand expandible-vertical': isExpandible}">
        <div ng-if="::controller.label" class="modal-header">
          <button type="button" class="close" data-dismiss="modal" data-testid="${TestIds.dialogClose}">&times;</button>
          <h4 class="modal-title">
            ${getIconTemplate("panel-title-icon")}
            <span translate-multiple="{{::controller.label}}"></span>
          </h4>
        </div>
        <div ng-class="::{'expand expandible-vertical': isExpandible}" ng-transclude></div>
      </div>
    </div>
  </div>
</div>`;

/**
 * Initialize a dialog component (a dialog or a side panel): it opens and closes when its controller "opened" attribute
 * changes, with its own action stack, and finishes the action which opened it when it is closed.
 * @param {object} scope Directive scope
 * @param {string} id Component identifier
 * @param {object} services Component, ActionController and AweUtilities services
 * @returns {object|false} Initialized component (false when the component initialization failed)
 */
export function initDialogComponent(scope, id, {Component, $actionController, Utilities}) {
  // Init as component
  let component = new Component(scope, id);
  component.isOpened = false;
  if (!component.asComponent()) {
    // If component initialization is wrong, cancel initialization
    return false;
  }

  // End of a close not run yet: the close ends some time after the modal is hidden, and the focus is back on the
  // control which opened it before that, so it can be opened again in between (#976)
  let pendingClose = null;

  /**
   * Open modal screen
   */
  component.openDialog = function () {
    if (pendingClose) {
      // Opened again before the close ended: end it now (its stack and its opening action), then open
      Utilities.timeout.cancel(pendingClose.timer);
      pendingClose.end();
    }
    if (!component.isOpened) {
      component.isOpened = true;

      // Broadcast model change
      Utilities.publishDelayedFromScope("modalChange", true, scope);

      // Add new modal action stack
      $actionController.addStack();

      // Load the data of a deferred dialog the first time it is opened
      component.loadDeferred();

      // Opened by an action in view mode, its criteria are read-only for this opening
      const openAction = component.controller.openAction;
      component.setViewMode(typeof openAction?.attr === "function" && openAction.attr("viewMode") === true);

      // Load dialog model (if not loaded yet)
      let actions = [];
      // Launch a resize
      actions.push({type: 'resize', parameters: {delay: 200}});

      // Launch action list
      $actionController.addActionList(actions, true, {address: component.address, context: component.context});

      // Store event
      component.storeEvent('open');
    }
  };
  /**
   * Close modal screen
   */
  component.closeDialog = function () {
    if (component.isOpened && !pendingClose) {
      // Store event
      component.storeEvent('close');

      // The view mode belongs to the opening
      component.setViewMode(false);

      // Broadcast model change
      Utilities.publishDelayedFromScope("modalChange", false, scope);

      // The action which opened this opening: a new opening replaces the one of the controller
      const {openAction, accept} = component.controller;
      const close = {
        end() {
          pendingClose = null;
          // Remove modal action stack
          $actionController.removeStack();
          // Close dialog action
          $actionController.finishAction(openAction, accept);

          // Set default action on accept
          component.controller.accept = component.controller.acceptOnClose;
          component.isOpened = false;
        }
      };
      pendingClose = close;

      // Launch close dialog delayed
      close.timer = Utilities.timeout(() => pendingClose === close && close.end(), 200);
    }
  };

  /**
   * Event listeners
   */
  component.listeners = component.listeners || {};
  // On model change launch dependency
  component.listeners["controllerChange"] = scope.$on("controllerChange", function (event, parameters) {
    if (_.isEqual(parameters.address, component.address) && "opened" in parameters.controller) {
      if (parameters.controller.opened) {
        component.openDialog();
      } else {
        component.closeDialog();
      }
    }
  });
  return component;
}

// Dialog directive
aweApplication.directive('aweDialog',
  ['ServerData', 'Component', 'ActionController', 'AweUtilities',
    function (serverData, Component, $actionController, Utilities) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          modalId: '@dialogId'
        },
        link: function (scope) {
          const component = initDialogComponent(scope, scope.modalId, {Component, $actionController, Utilities});
          if (!component) {
            return false;
          }

          if (component.controller) {
            scope.isExpandible = component.controller.style ? component.controller.style.indexOf("expand") !== -1 : false;
          }
        }
      };
    }
  ]);
