/**
 * Keep the colour picker of a colour criterion or column inside the viewport (#949). The picker (angular-bootstrap-
 * colorpicker) is appended to <body> with its left edge on the left edge of the swatch, which is at the end of the field:
 * near the right edge of the screen (a criterion of a side panel) it went out of the viewport and widened the page with a
 * horizontal scrollbar. Once shown, it is shifted to the left as much as it overflows, never past the left edge.
 * @param {object} scope Scope of the colour criterion or column (the picker emits its events on it)
 */
export function keepColorPickerInViewport(scope) {
  scope.$on("colorpicker-shown", () => {
    const picker = document.body.querySelector(":scope > .colorpicker.colorpicker-visible");
    if (!picker) {
      return;
    }
    const {left, width} = picker.getBoundingClientRect();
    const overflow = left + width - document.documentElement.clientWidth;
    if (overflow > 0) {
      picker.style.left = `${Math.max(0, left - overflow) + window.pageXOffset}px`;
    }
  });
}
