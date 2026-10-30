import $ from "jquery";
import {afterTransition, animates, reflow} from "./transition";

/**
 * Native collapse behavior (it replaces the Bootstrap 3.4.1 collapse plugin, which AWE no longer ships).
 *
 * It only animates: the owner (the accordion item) decides whether the panel is open. At rest an open panel is
 * `.collapse.in` and a closed one `.collapse`; while it changes it is `.collapsing` with an inline height which
 * grows from 0 to its content (or shrinks to 0) with the CSS transition of the class (0.35 s), ended by
 * `transitionend` or by timeout. The trigger is `.collapsed` while the panel is closed.
 */

const DATA_KEY = "awe.collapse";
const TRANSITION_DURATION = 350;

/**
 * Collapsible panel
 */
class Collapse {
  /**
   * @param {jQuery} element Panel which collapses
   * @param {jQuery} [trigger] Control which toggles it (it is marked as collapsed while the panel is closed)
   */
  constructor(element, trigger) {
    this.element = element;
    this.trigger = trigger || $();
    this.finishRunning = null;
  }

  /**
   * Check if the panel is open (or opening)
   * @returns {boolean} The panel is open
   */
  isOpen() {
    return this.element.hasClass("in");
  }

  /**
   * Open or close the panel. A running transition is finished at once first.
   * @param {boolean} open The panel has to be open
   * @param {object} [options] animate (false changes the state at once) and onDone (called when it is finished)
   */
  setOpen(open, {animate = true, onDone} = {}) {
    if (this.finishRunning) {
      this.finishRunning();
    }
    if (!this.element.length) {
      return;
    }
    if (open === this.isOpen()) {
      // Already there: only make sure the trigger says so
      this.trigger.toggleClass("collapsed", !open);
      return;
    }
    if (open) {
      this.show(animate, onDone);
    } else {
      this.hide(animate, onDone);
    }
  }

  /**
   * Run the end of a change once, whether the transition ends, the timer fires or another change interrupts it
   * @param {Function} complete Function which leaves the panel at rest
   * @param {Function} [onDone] Function to call afterwards
   * @param {boolean} animate The change is animated
   */
  run(complete, onDone, animate) {
    const finish = () => {
      this.finishRunning = null;
      complete();
      if (onDone) {
        onDone();
      }
    };
    if (animate && animates(this.element)) {
      let called = false;
      this.finishRunning = () => {
        if (!called) {
          called = true;
          finish();
        }
      };
      afterTransition(this.element, TRANSITION_DURATION, this.finishRunning);
      return true;
    }
    finish();
    return false;
  }

  /**
   * Open the panel
   * @param {boolean} animate The change is animated
   * @param {Function} [onDone] Function to call when it is open
   */
  show(animate, onDone) {
    this.element
      .removeClass("collapse")
      .addClass("collapsing")
      .height(0);
    this.trigger.removeClass("collapsed");

    const running = this.run(() => {
      this.element
        .removeClass("collapsing")
        .addClass("collapse in")
        .height("");
    }, onDone, animate);

    if (running) {
      this.element.height(this.element[0].scrollHeight);
    }
  }

  /**
   * Close the panel
   * @param {boolean} animate The change is animated
   * @param {Function} [onDone] Function to call when it is closed
   */
  hide(animate, onDone) {
    // Start from the current height, so the transition has something to shrink from
    this.element.height(this.element.height());
    reflow(this.element[0]);
    this.element
      .addClass("collapsing")
      .removeClass("collapse in");
    this.trigger.addClass("collapsed");

    const running = this.run(() => {
      this.element
        .removeClass("collapsing")
        .addClass("collapse")
        .height("");
    }, onDone, animate);

    if (running) {
      this.element.height(0);
    }
  }
}

/**
 * Get the collapse behavior of a panel (it is created the first time)
 * @param {jQuery|HTMLElement} element Panel
 * @param {jQuery|HTMLElement} [trigger] Control which toggles it
 * @returns {Collapse} Collapse
 */
export function getCollapse(element, trigger) {
  const $element = $(element);
  let collapse = $element.data(DATA_KEY);
  if (!collapse) {
    collapse = new Collapse($element, $(trigger));
    $element.data(DATA_KEY, collapse);
  }
  return collapse;
}
