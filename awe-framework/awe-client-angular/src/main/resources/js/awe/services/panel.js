import {aweApplication} from "../awe";

// Panel service
aweApplication.factory('Panel',
  ['Component',
    /**
     * Numeric generic methods
     * @param {Service} Component
     */
    function (Component) {
      // Events
      const VALIDITY_EVENT = "criterion-validity";
      const SELECT_EVENT = "select-current-tab";

      const Panel = {
        /**
         * Initialize numeric criteria
         * @param {Object} controller Panel controller
         * @param {Scope} scope Panel scope
         * @param {string} identifier Panel id
         */
        init: function (controller, scope, identifier) {
          // Init as component
          let  component = new Component(scope, identifier);
          if (!component.init()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          /**
           * Click button function
           */
          scope.isActive = function () {
            return controller.isActive(identifier);
          };

          // A deferred page loads its data the first time it becomes the active one
          scope.$watch(function () {
            return controller.isActive(identifier);
          }, function (active) {
            if (active) {
              component.loadDeferred();
            }
          });

          /**
           * Event listeners
           */
          component.listeners = component.listeners || {};

          // Select current tab
          component.listeners['selectCurrentTab'] = scope.$on(SELECT_EVENT, function () {
            controller.selectTab(identifier);
          });

          // Mark the panel while it holds invalid fields
          if (controller.setInvalid) {
            component.listeners['trackInvalid'] = Panel.trackInvalid(scope, function (invalid) {
              controller.setInvalid(identifier, invalid);
            });
          }
        },
        /**
         * Follow the validity of the fields inside a panel
         * @param {Scope} scope Panel scope
         * @param {function} onChange Called with true when the panel starts to hold invalid fields and with false when
         * it holds none anymore
         * @return {function} Function to release the listener
         */
        trackInvalid: function (scope, onChange) {
          const invalidIds = new Set();
          return scope.$on(VALIDITY_EVENT, function (event, data) {
            const wasInvalid = invalidIds.size > 0;
            if (data.invalid) {
              invalidIds.add(data.id);
            } else {
              invalidIds.delete(data.id);
            }
            if (wasInvalid !== invalidIds.size > 0) {
              onChange(invalidIds.size > 0);
            }
          });
        },
        /**
         * Open a collapsible item when a field inside it has to be revealed
         * @param {Scope} scope Item scope
         * @param {function} isOpen Returns true when the item is open
         * @param {function} open Opens the item
         * @return {function} Function to release the listener
         */
        revealOn: function (scope, isOpen, open) {
          return scope.$on(SELECT_EVENT, function () {
            if (!isOpen()) {
              open();
            }
          });
        }
      };
      return Panel;
    }
  ]);