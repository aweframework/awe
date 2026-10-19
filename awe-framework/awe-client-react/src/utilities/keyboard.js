/**
 * Wildcard for a modifier of a shortcut: the shortcut matches whether the modifier is pressed or not
 * @type {string}
 */
export const ANY = "any";

/**
 * Codes of the digit keys of the main keyboard row (they do not change with Option on macOS, unlike event.key)
 * @type {string[]}
 */
export const DIGIT_CODES = Object.freeze(["Digit0", "Digit1", "Digit2", "Digit3", "Digit4", "Digit5", "Digit6", "Digit7", "Digit8", "Digit9"]);

const MODIFIERS = [["alt", "altKey"], ["shift", "shiftKey"], ["ctrl", "ctrlKey"], ["meta", "metaKey"]];

/**
 * Check a value of an event against one expected value or a list of them
 * @param {string} actual Value of the event
 * @param {string|string[]} expected Expected value or values
 * @return {boolean} The value is one of the expected
 */
const matchesValue = (actual, expected) => Array.isArray(expected) ? expected.includes(actual) : actual === expected;

/**
 * Check if a keyboard event is a shortcut. The modifiers are exact: one that the shortcut does not set must not be
 * pressed, unless the shortcut sets it to ANY.
 * @param {KeyboardEvent} event Keyboard event
 * @param {object} shortcut Shortcut definition
 * @param {string|string[]} [shortcut.key] Value(s) of event.key. Language dependent, use it for named keys
 * @param {string|string[]} [shortcut.code] Value(s) of event.code. Physical key, use it for digits and letters
 * @param {boolean|string} [shortcut.alt] Alt must be pressed (true), not pressed (false or unset) or either (ANY)
 * @param {boolean|string} [shortcut.shift] Shift must be pressed (true), not pressed (false or unset) or either (ANY)
 * @param {boolean|string} [shortcut.ctrl] Ctrl must be pressed (true), not pressed (false or unset) or either (ANY)
 * @param {boolean|string} [shortcut.meta] Meta must be pressed (true), not pressed (false or unset) or either (ANY)
 * @return {boolean} The event is the shortcut
 */
export function matchesShortcut(event, shortcut) {
  if (!event || !shortcut || (shortcut.key === undefined && shortcut.code === undefined)) {
    return false;
  }
  if (shortcut.key !== undefined && !matchesValue(event.key, shortcut.key)) {
    return false;
  }
  if (shortcut.code !== undefined && !matchesValue(event.code, shortcut.code)) {
    return false;
  }
  return MODIFIERS.every(([name, property]) => shortcut[name] === ANY || Boolean(event[property]) === Boolean(shortcut[name]));
}

/**
 * Create a key handler from a list of bindings. It runs the handler of the first binding that matches the event.
 * @param {object[]} [bindings] Bindings
 * @param {object} [bindings[].shortcut] Shortcut of the binding (see matchesShortcut)
 * @param {object[]} [bindings[].shortcuts] Several shortcuts for the same handler
 * @param {function(KeyboardEvent, object)} bindings[].handler Called with the event and the shortcut that matched
 * @return {function(KeyboardEvent): boolean} Handler for a key event. It returns true when a binding matched
 */
export function createKeyHandler(bindings = []) {
  return (event) => {
    for (const binding of bindings) {
      const shortcuts = binding.shortcuts ?? [binding.shortcut];
      const matched = shortcuts.find((shortcut) => matchesShortcut(event, shortcut));
      if (matched) {
        binding.handler(event, matched);
        return true;
      }
    }
    return false;
  };
}

const entry = (id, keys, scope, description) => Object.freeze({id, keys, scope, description});

/**
 * Catalogue of the keyboard shortcuts of the React engine. It is the source of the keyboard shortcuts guide of the
 * documentation: add here every new shortcut.
 * @type {ReadonlyArray<{id: string, keys: string, scope: string, description: string}>}
 */
export const SHORTCUTS = Object.freeze([
  entry("actions-stack", "Alt+Shift+0..9", "global", "Show the stack of client and server actions (debug), 0 hides it"),
  entry("grid-move-row-up", "Alt+ArrowUp", "grid", "Move the selected row up (editable grid)"),
  entry("grid-move-row-down", "Alt+ArrowDown", "grid", "Move the selected row down (editable grid)"),
  entry("grid-move-row-first", "Alt+Home", "grid", "Move the selected row to the first position (editable grid)"),
  entry("grid-move-row-last", "Alt+End", "grid", "Move the selected row to the last position (editable grid)"),
  entry("grid-cancel-row", "Escape", "grid", "Cancel the edition of the row"),
  entry("grid-save-row", "Enter", "grid", "Save the edition of the row"),
  entry("grid-cell-tooltip-hide", "Escape", "grid cell", "Hide the tooltip with the full text of a cut cell"),
  entry("grid-text-column-commit", "Enter", "grid text column", "Store the value typed in the cell"),
  entry("grid-radio-next", "ArrowRight / ArrowDown", "radio column", "Select the next option, from the last to the first"),
  entry("grid-radio-previous", "ArrowLeft / ArrowUp", "radio column", "Select the previous option, from the first to the last"),
  entry("criterion-submit", "Enter", "criterion", "Submit the screen from a text, password or color criterion"),
  entry("criterion-numeric-commit", "Enter", "criterion", "Commit the value typed in a numeric criterion"),
  entry("suggest-open", "Space", "suggest", "Open the options of an empty suggest"),
  entry("menu-search-next", "ArrowDown", "menu search", "Go to the next result"),
  entry("menu-search-previous", "ArrowUp", "menu search", "Go to the previous result"),
  entry("menu-search-select", "Enter", "menu search", "Open the active result"),
  entry("menu-search-close", "Escape", "menu search", "Close the search"),
  entry("dialog-close", "Escape", "dialog", "Close the dialog (message and confirm dialogs do not close with it)")
]);
