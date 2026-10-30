import $ from "jquery";

/**
 * Force a reflow, so a transition starts from the state the element has now (reading the width makes the browser lay it out)
 * @param {HTMLElement} element Element
 * @returns {number} Width of the element
 */
export const reflow = element => element.offsetWidth;

/**
 * Check if an element animates (Bootstrap relied on the browser supporting CSS transitions, which is always true: an
 * element which has no transition in its style would only make the user wait for the timeout)
 * @param {jQuery} target Element
 * @returns {boolean} The element has a CSS transition
 */
export const animates = target => {
  const durations = window.getComputedStyle(target[0]).transitionDuration || "";
  return durations.split(",").some(duration => Number.parseFloat(duration) > 0);
};

/**
 * Call back once a transition of the target ends: when it raises "transitionend" (the transitions of its content are
 * ignored) or after the duration, whatever happens first
 * @param {jQuery} target Element which transitions
 * @param {number} duration Maximum wait in milliseconds
 * @param {Function} callback Function to call
 * @returns {Function} Cancel: the callback is never called (the owner went away or changed its mind)
 */
export const afterTransition = (target, duration, callback) => {
  let called = false;
  let timer = null;
  const $target = $(target);
  const finish = () => {
    if (!called) {
      called = true;
      clearTimeout(timer);
      $target.off("transitionend.awe.transition", onEnd);
      callback();
    }
  };
  const onEnd = event => {
    if (event.target === $target[0]) {
      finish();
    }
  };
  $target.on("transitionend.awe.transition", onEnd);
  timer = setTimeout(finish, duration);
  return () => {
    called = true;
    clearTimeout(timer);
    $target.off("transitionend.awe.transition", onEnd);
  };
};
