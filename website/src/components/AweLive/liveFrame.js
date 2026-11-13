// Frame of a live example. Pure on purpose (no JSX), so that node:test and the Jest tests of the client can use it.
const {liveClassName} = require('./liveClassName');

// The frame is a form, as the application renders its screens inside FormContainer: the browser groups the radios by
// name within their form, so a radio group of one example never unchecks the radios of another example with the same
// group name (outside a form, every radio named "plan" of the page would be one group)
const LIVE_FRAME_ELEMENT = 'form';

/**
 * A submit of the frame (Enter in a text criterion) stays on the page, as FormContainer does in the application
 * @param {Event} event Submit event
 */
function preventSubmit(event) {
  event.preventDefault();
  event.stopPropagation();
}

/**
 * Props of the frame of an example
 * @param {{bare?: boolean, height?: string, colorScheme?: string}} props `bare` drops the frame, `height` sizes it and
 *   `colorScheme` ('light' | 'dark') forces its colour mode
 * @returns {{className: string, style?: Object, onSubmit: Function}} Props of the frame element
 */
function liveFrameProps({bare, height, colorScheme} = {}) {
  return {
    className: liveClassName({bare, height}),
    style: colorScheme || height ? {colorScheme, height} : undefined,
    onSubmit: preventSubmit,
  };
}

module.exports = {LIVE_FRAME_ELEMENT, liveFrameProps};
