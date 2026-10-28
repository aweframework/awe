import {iconMarkup} from "./icon";

// Time the actions launched by a button must still be running before the button shows its busy state (#771): the
// actions that end before do not change it. The same value as the React engine
export const BUSY_THRESHOLD = 400;

/**
 * Spinner of a busy button, before its icon (hidden while busy) or its label, as the login loading
 * @param {string} busy Expression of the busy state
 * @returns {string} Template
 */
export const busySpinnerTemplate = busy => `<i ng-if="${busy}" class="button-icon button-busy">${iconMarkup("spinner fa-spin")}</i>`;

/**
 * Busy state of the button that launched actions (#771): the property of the scope becomes true when the actions are
 * still running BUSY_THRESHOLD after start() and false when they end or the scope is destroyed
 * @param {object} args Arguments
 * @param {Scope} args.scope Scope of the button
 * @param {string} args.property Property of the scope with the busy state
 * @param {function} args.isRunning Whether the actions are running ("actions-running" of the Storage)
 * @param {function} args.timeout $timeout
 * @returns {{start: function}} start() on the click of the button
 */
export function createBusyState({scope, property, isRunning, timeout}) {
  let timer = null;
  const stop = () => {
    timeout.cancel(timer);
    timer = null;
    scope[property] = false;
  };
  const start = () => {
    timeout.cancel(timer);
    timer = timeout(() => {
      timer = null;
      scope[property] = Boolean(isRunning());
    }, BUSY_THRESHOLD);
  };

  scope[property] = false;
  scope.$watch(() => Boolean(isRunning()), running => {
    if (!running) {
      stop();
    }
  });
  scope.$on("$destroy", stop);
  return {start};
}
