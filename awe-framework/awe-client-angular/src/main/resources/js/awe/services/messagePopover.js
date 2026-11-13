import $ from "jquery";
import {TestAttributes} from "../data/testIds";
import {DEFAULT_ALLOW_LIST, sanitizeHtml} from "./sanitizeHtml";
import {afterTransition, animates} from "./transition";

/**
 * Message popover: the box which a target message shows next to a component or a grid cell. It replaces the Bootstrap
 * 3.4.1 popover plugin (AWE no longer ships Bootstrap): same markup and classes (`.popover.fade.in.<placement>` with
 * its `.arrow`), same placement (auto flips to the other side when there is no room, the popover stays inside the
 * viewport and the arrow keeps pointing at the target) and the same fade timing (150 ms).
 *
 * The title and the content are always shown as text. The template is sanitized with the allow list of Bootstrap plus
 * the test hooks.
 */

const TRANSITION_DURATION = 150;
const AUTO_TOKEN = /\s?auto?\s?/i;
const HOOK_ATTRIBUTES = [TestAttributes.testId, TestAttributes.owner, TestAttributes.type];

// Allow list of the template: Bootstrap's plus the test hooks
const ALLOW_LIST = Object.freeze({...DEFAULT_ALLOW_LIST, "*": [...DEFAULT_ALLOW_LIST["*"], ...HOOK_ATTRIBUTES]});

let uidCounter = 0;

/**
 * Get a new id which is not used in the page
 * @returns {string} Popover id
 */
const getUid = () => {
  let id;
  do {
    uidCounter = (uidCounter + Math.floor(Math.random() * 1000000)) % 1000000;
    id = "popover" + uidCounter;
  } while (document.getElementById(id));
  return id;
};

/**
 * Get the position and size of an element (the body is the whole window)
 * @param {jQuery} element Element
 * @returns {object} Box of the element in the page
 */
const getPosition = element => {
  const el = element[0];
  const isBody = el.tagName === "BODY";
  const rect = el.getBoundingClientRect();
  const offset = isBody ? {top: 0, left: 0} : element.offset();
  const scroll = {scroll: isBody ? document.documentElement.scrollTop || document.body.scrollTop : element.scrollTop()};
  const outerDims = isBody ? {width: $(window).width(), height: $(window).height()} : null;
  return $.extend({}, rect, scroll, outerDims, offset);
};

/**
 * Get the position of the popover for a placement
 * @param {string} placement top, bottom, left or right
 * @param {object} pos Box of the target
 * @param {number} width Popover width
 * @param {number} height Popover height
 * @returns {{top: number, left: number}} Offset of the popover
 */
const getCalculatedOffset = (placement, pos, width, height) => {
  switch (placement) {
    case "bottom":
      return {top: pos.top + pos.height, left: pos.left + pos.width / 2 - width / 2};
    case "top":
      return {top: pos.top - height, left: pos.left + pos.width / 2 - width / 2};
    case "left":
      return {top: pos.top + pos.height / 2 - height / 2, left: pos.left - width};
    default:
      return {top: pos.top + pos.height / 2 - height / 2, left: pos.left + pos.width};
  }
};

/**
 * Get the side of the target the popover goes to: the wanted one, or the opposite one when there is no room for it
 * @param {string} placement Wanted side (top, bottom, left or right)
 * @param {object} pos Box of the target
 * @param {number} width Popover width
 * @param {number} height Popover height
 * @param {object} viewport Box of the viewport
 * @returns {string} Side
 */
const flipPlacement = (placement, pos, width, height, viewport) => {
  if (placement === "bottom" && pos.bottom + height > viewport.bottom) {
    return "top";
  }
  if (placement === "top" && pos.top - height < viewport.top) {
    return "bottom";
  }
  if (placement === "right" && pos.right + width > viewport.width) {
    return "left";
  }
  if (placement === "left" && pos.left - width < viewport.left) {
    return "right";
  }
  return placement;
};

/**
 * Get how much the popover has to move to stay inside the viewport
 * @param {string} placement Placement
 * @param {object} pos Offset of the popover
 * @param {number} width Popover width
 * @param {number} height Popover height
 * @param {jQuery} viewport Viewport element
 * @returns {{top: number, left: number}} Movement
 */
const getViewportAdjustedDelta = (placement, pos, width, height, viewport) => {
  const delta = {top: 0, left: 0};
  const dimensions = getPosition(viewport);

  if (/right|left/.test(placement)) {
    const topEdgeOffset = pos.top - dimensions.scroll;
    const bottomEdgeOffset = pos.top - dimensions.scroll + height;
    if (topEdgeOffset < dimensions.top) {
      delta.top = dimensions.top - topEdgeOffset;
    } else if (bottomEdgeOffset > dimensions.top + dimensions.height) {
      delta.top = dimensions.top + dimensions.height - bottomEdgeOffset;
    }
  } else {
    const leftEdgeOffset = pos.left;
    const rightEdgeOffset = pos.left + width;
    if (leftEdgeOffset < dimensions.left) {
      delta.left = dimensions.left - leftEdgeOffset;
    } else if (rightEdgeOffset > dimensions.right) {
      delta.left = dimensions.left + dimensions.width - rightEdgeOffset;
    }
  }

  return delta;
};

/**
 * Resolve an option which may be a function of the target
 * @param {*} value Value or function
 * @param {HTMLElement} target Target
 * @returns {*} Value
 */
const resolve = (value, target) => typeof value === "function" ? value.call(target) : value;

/**
 * Popover of one target
 */
class MessagePopover {
  /**
   * @param {jQuery} target Element the popover points at
   * @param {object} options template, title, content, placement ("auto bottom"), container (selector, default body),
   * onShown and onHidden (functions)
   */
  constructor(target, options) {
    this.target = $(target);
    this.options = {placement: "auto bottom", container: "body", ...options};
    this.tipElement = null;
    this.destroyed = false;
  }

  /**
   * Get the element of the popover (it is created from the sanitized template the first time)
   * @returns {jQuery} Popover element
   */
  tip() {
    if (!this.tipElement) {
      this.tipElement = $(sanitizeHtml(this.options.template, ALLOW_LIST));
      if (this.tipElement.length !== 1) {
        throw new Error("The popover template must consist of exactly 1 top-level element");
      }
    }
    return this.tipElement;
  }

  /**
   * Check if the popover can be shown: it needs a target in the page, a title or a content, and not to be destroyed
   * @param {*} title Title
   * @param {*} content Content
   * @returns {boolean} It can be shown
   */
  canShow(title, content) {
    if (this.destroyed || !(title || content)) {
      return false;
    }
    const target = this.target[0];
    return $.contains(target.ownerDocument.documentElement, target);
  }

  /**
   * Write the title and the content in the popover and link the target to it
   * @param {*} title Title
   * @param {*} content Content
   */
  fill(title, content) {
    const tip = this.tip();
    const id = getUid();

    tip.find(".popover-title").text(title || "");
    tip.find(".popover-content").children().detach().end().text(content || "");
    tip.removeClass("fade top bottom left right in");
    // A popover without title does not show its header
    if (!tip.find(".popover-title").html()) {
      tip.find(".popover-title").hide();
    }
    tip.attr("id", id);
    this.target.attr("aria-describedby", id);
    tip.addClass("fade");
  }

  /**
   * Show the popover, unless it has no content or its target is not in the page
   */
  show() {
    // Nothing to point at (no element matches): nothing to show
    if (!this.target.length) {
      return;
    }
    const title = resolve(this.options.title, this.target[0]);
    const content = resolve(this.options.content, this.target[0]);
    if (!this.canShow(title, content)) {
      return;
    }

    this.fill(title, content);
    const tip = this.tip();

    let placement = this.options.placement;
    const autoPlace = AUTO_TOKEN.test(placement);
    if (autoPlace) {
      placement = placement.replace(AUTO_TOKEN, "") || "top";
    }

    tip
      .detach()
      .css({top: 0, left: 0, display: "block"})
      .addClass(placement)
      .appendTo($(document).find(this.options.container));

    const pos = getPosition(this.target);
    const actualWidth = tip[0].offsetWidth;
    const actualHeight = tip[0].offsetHeight;

    if (autoPlace) {
      const flipped = flipPlacement(placement, pos, actualWidth, actualHeight, getPosition($("body")));
      tip.removeClass(placement).addClass(flipped);
      placement = flipped;
    }

    this.applyPlacement(getCalculatedOffset(placement, pos, actualWidth, actualHeight), placement);

    const complete = () => this.options.onShown?.();
    if (tip.hasClass("fade") && animates(tip)) {
      afterTransition(tip, TRANSITION_DURATION, complete);
    } else {
      complete();
    }
  }

  /**
   * Put the popover in its position and point its arrow at the target
   * @param {{top: number, left: number}} offset Position of the popover
   * @param {string} placement Placement
   */
  applyPlacement(offset, placement) {
    const tip = this.tip();
    const width = tip[0].offsetWidth;
    const height = tip[0].offsetHeight;

    // The margins are read by hand because the bounding box includes them
    const marginTop = Number.parseInt(tip.css("margin-top"), 10) || 0;
    const marginLeft = Number.parseInt(tip.css("margin-left"), 10) || 0;
    offset.top += marginTop;
    offset.left += marginLeft;

    // jQuery does not round the pixel values
    $.offset.setOffset(tip[0], $.extend({
      using: props => tip.css({top: Math.round(props.top), left: Math.round(props.left)})
    }, offset), 0);

    tip.addClass("in");

    // The popover may resize itself in its new position
    const actualWidth = tip[0].offsetWidth;
    const actualHeight = tip[0].offsetHeight;

    if (placement === "top" && actualHeight !== height) {
      offset.top = offset.top + height - actualHeight;
    }

    const delta = getViewportAdjustedDelta(placement, offset, actualWidth, actualHeight, $("body"));

    if (delta.left) {
      offset.left += delta.left;
    } else {
      offset.top += delta.top;
    }

    const isVertical = /top|bottom/.test(placement);
    const arrowDelta = isVertical ? delta.left * 2 - width + actualWidth : delta.top * 2 - height + actualHeight;
    const dimension = tip[0][isVertical ? "offsetWidth" : "offsetHeight"];

    tip.offset(offset);
    tip.find(".arrow")
      .css(isVertical ? "left" : "top", 50 * (1 - arrowDelta / dimension) + "%")
      .css(isVertical ? "top" : "left", "");
  }

  /**
   * Hide the popover: it fades out, is detached and raises onHidden
   * @param {Function} [callback] Function to call once it is hidden
   */
  hide(callback) {
    const tip = this.tipElement;
    if (!tip?.parent().length) {
      callback?.();
      return;
    }

    const complete = () => {
      tip.detach();
      this.target.removeAttr("aria-describedby");
      if (this.options.onHidden) {
        this.options.onHidden();
      }
      if (callback) {
        callback();
      }
    };

    tip.removeClass("in");
    if (tip.hasClass("fade") && animates(tip)) {
      afterTransition(tip, TRANSITION_DURATION, complete);
    } else {
      complete();
    }
  }

  /**
   * Hide the popover and release it: it cannot be shown again
   */
  destroy() {
    this.hide(() => {
      this.destroyed = true;
      if (this.tipElement) {
        this.tipElement.detach();
      }
      this.tipElement = null;
    });
  }
}

/**
 * Create the popover of a target (it is not shown until show() is called)
 * @param {jQuery|HTMLElement} target Element the popover points at
 * @param {object} options Options: template (required), title, content (text or functions), placement, container,
 * onShown and onHidden
 * @returns {{show: Function, hide: Function, destroy: Function}} Popover
 */
export function createMessagePopover(target, options) {
  const popover = new MessagePopover(target, options);
  return {
    show: () => popover.show(),
    hide: () => popover.hide(),
    destroy: () => popover.destroy()
  };
}
