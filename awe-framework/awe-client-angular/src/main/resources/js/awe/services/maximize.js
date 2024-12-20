import {aweApplication} from "../awe";

// Maximize service
aweApplication.factory('Maximize',
  ['Position', 'AweUtilities',
    function (position, Utilities) {
      return {
        /**
         * Initialize dialog
         * @param {scope} scope Dialog scope
         * @param {object} elem dialog node
         */
        initMaximize: function (scope, elem) {
          // Panel text
          let panelText = {
            MAXIMIZE: 'SCREEN_TEXT_MAXIMIZE',
            RESTORE: 'SCREEN_TEXT_RESTORE'
          };

          // Variable initialization
          let animationTime = 300;
          let useCSS3Animation = true;
          let maximizeTarget;
          let minimize = {
            targets: [],
            parents: []
          };

          scope.maximized = false;
          scope.iconMaximized = false;
          scope.togglePanelText = panelText.MAXIMIZE;
          scope.panelResizing = false;
          scope.maximizing = false;

          // Controller variables
          if (scope.controller) {
            scope.maximize = scope.controller.maximize;
          } else {
            scope.maximize = false;
          }

          // Store heading and content panel
          let $body = $('body');
          /**
           * Remove animation node
           * @param {object} $node Animation node
           */
          const removeAnimationNode = function ($node) {
            Utilities.timeout(function () {
              // Remove aanimation
              $body.removeClass("animationContainer");
              $node.remove();
              // End resizing
              scope.$root.resizing = false;
              scope.panelResizing = false;
              scope.maximizing = false;
            });
          };
          /**
           * Launches maximize/restore animations
           * @param {object} node Resize node
           * @param {object} finalSize Final resize size
           * @param {function} onEndAnimation On end animation function (optional)
           * @returns {undefined}
           */
          const launchAnimation = function (node, finalSize, onEndAnimation) {
            let $node = $(node);
            /**
             * Launch end animation methods
             */
            const endAnimation = function () {
              // Launch on end animation event (if defined)
              if (onEndAnimation) {
                onEndAnimation();
              }

              // Remove animation node
              removeAnimationNode($node);
            };
            if (useCSS3Animation) {
              Utilities.animateCSS($node, finalSize, animationTime, endAnimation);
            } else {
              Utilities.animateJavascript($node, finalSize, animationTime, endAnimation);
            }
          };
          /**
           * Generate layer cloned for animation
           */
          const generateAnimationClone = function () {
            // Get initial size
            let initialSize = position.getOuterDimensions(elem);
            // Add animate clone
            let resizing = elem.clone();
            resizing.css(initialSize);
            resizing.removeClass("expand");
            resizing.addClass("resizeAnimation");

            // Empty content layer
            resizing.find(".maximize-content").addClass("panel-awe").empty();


            $body.addClass("animationContainer");
            $body.append(resizing);
            // Return clone layer
            return resizing;
          };

          /**
           * Minimize parents and siblings
           * @param {type} maximizeTarget
           * @returns {undefined}
           */
          const updateElementsToMinimize = function (maximizeTarget) {
            minimize.targets = [].concat(elem.siblings(":visible").toArray());
            minimize.parents = [];
            _.each(elem.parentsUntil(maximizeTarget), function (parent) {
              minimize.targets = minimize.targets.concat($(parent).siblings(":visible").toArray());
              minimize.parents.push(parent);
            });
          };

          /**
           * Minimize parents and siblings
           * @returns {undefined}
           */
          const minimizeParentsAndSiblings = function () {
            $(minimize.parents).addClass("maximizeParent");
            $(minimize.targets).addClass("minimized");
            elem.removeAttr('style');
          };

          /**
           * Animate maximize
           * @param resizing
           */
          const animateMaximize = function (resizing) {
            // Launch animation
            let maximizeSizes = scope.maximizeTargetLayer();
            updateElementsToMinimize(maximizeTarget);
            $(minimize.targets).fadeOut(animationTime);
            launchAnimation(resizing, maximizeSizes.final, function () {
              scope.maximized = true;
              scope.iconMaximized = true;
              minimizeParentsAndSiblings();
              scope.$broadcast("resize");
            });
          }

          /**
           * Animate restore
           * @param resizing
           */
          const animateRestore = function (resizing) {
            // Launch animation
            let finalSize = position.getOuterDimensions(elem);
            launchAnimation(resizing, finalSize, function () {
              scope.iconMaximized = false;
              scope.$broadcast("resize");
            });
          }

          /**
           * Calculate maximized size depending on maximize target
           * @returns {object} Resizing size
           */
          scope.maximizeTargetLayer = function () {
            let finalSize = position.getInnerDimensions(maximizeTarget);
            // Get margins
            let margins = {
              width: elem.outerWidth(true) - elem.outerWidth(false),
              height: elem.outerHeight(true) - elem.outerHeight(false)
            };
            finalSize.width -= margins.width;
            finalSize.height -= margins.height;
            // Apply maximize dimensions to target layer
            let elemSize = _.cloneDeep(finalSize);
            let offset = elem.offsetParent().offset();
            elemSize.top -= offset.top;
            elemSize.left -= offset.left;
            // Calculate final size for animation
            finalSize.top += parseInt(elem.css('margin-top'));
            finalSize.left += parseInt(elem.css('margin-left'));
            return {final: finalSize, element: elemSize};
          };
          /**
           * On resize screen
           *
           */
          scope.onResize = function () {
            if (scope.maximized) {
              let maximizeSizes = scope.maximizeTargetLayer();
              elem.css(maximizeSizes.element);
            }
          };

          /**
           * Maximize the panel
           */
          scope.maximizePanel = function () {
            // Generate animation clone
            let resizing = generateAnimationClone();
            // Set resizing and maximized
            scope.$root.resizing = true;
            scope.panelResizing = true;
            scope.maximizing = true;
            // Restore all maximized parents
            scope.$emit("restore");
            // Find maximize target
            if (!maximizeTarget) {
              maximizeTarget = elem.parents(".maximize-target:first");
            }

            // Launch animation
            Utilities.timeout(() => animateMaximize(resizing), 200);
          };

          /**
           * Restore the window size
           */
          scope.restorePanel = function () {
            // Generate animation clone
            let resizing = generateAnimationClone();
            // Remove minimize class
            $(minimize.targets).removeClass("minimized").fadeIn(animationTime + 150);
            $(minimize.parents).removeClass("maximizeParent").fadeIn(animationTime + 150);
            // Remove minimized class
            elem.removeAttr('style');
            // Set resizing
            scope.$root.resizing = true;
            scope.panelResizing = true;
            scope.maximized = false;

            // Launch animation
            Utilities.timeout(() => animateRestore(resizing), 150)
          };

          /**
           * Restore the window size
           */
          scope.togglePanel = function () {
            if (scope.maximized) {
              scope.restorePanel();
              scope.togglePanelText = panelText.RESTORE;
            } else {
              scope.maximizePanel();
              scope.togglePanelText = panelText.MAXIMIZE;
            }
          };

          /**
           * Event listeners
           */
          let listeners = {};
          // Capture event for element resize
          listeners['resize'] = scope.$on("resize", function (event, initialScope) {
            if (scope !== initialScope && scope.onResize) {
              scope.onResize();
            }
          });
          // Capture event for children maximizes
          listeners['restore'] = scope.$on("restore", function () {
            if (scope.maximized) {
              scope.restorePanel();
            }
          });
          // Remove all listeners on unload
          scope.$on("$destroy", function () {
            // Clear listeners
            Utilities.clearListeners(listeners);
          });
        }
      };
    }
  ]);