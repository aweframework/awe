---
id: keyboard-shortcuts
title: Keyboard shortcuts
sidebar_label: Keyboard shortcuts
---

This page lists the keyboard shortcuts of the React client. The AngularJS client has the same shortcuts for the
actions stack, the editable grid rows, the criteria that submit the screen with Enter, the menu search and the radio
columns.

## Available shortcuts

### Global

| Keys | Action |
|------|--------|
| `Alt` + `Shift` + `0`..`9` | Show the stack of client and server actions (debug). `0` hides it. |

The digit is read from the physical key, so it also works on macOS where `Option` changes the typed character.

### Grid

| Keys | Action |
|------|--------|
| `Alt` + `ArrowUp` / `ArrowDown` | Move the selected row up or down (editable grid, or a grid with [`move-rows`](../api/grids.md#drag-and-drop)). |
| `Alt` + `Home` / `End` | Move the selected row to the first or last position (editable grid, or a grid with [`move-rows`](../api/grids.md#drag-and-drop)). |
| `Enter` | Save the edition of the row. |
| `Escape` | Cancel the edition of the row. |
| `Escape` (on a cut cell) | Hide the tooltip with the full text of the cell. |
| `Enter` (in a text column) | Store the value typed in the cell. |
| `ArrowRight` / `ArrowDown` | Radio column: select the next option, from the last to the first. |
| `ArrowLeft` / `ArrowUp` | Radio column: select the previous option, from the first to the last. |

### Criteria

| Keys | Action |
|------|--------|
| `Enter` (text, password, color, date, filtered date, time and date range) | Submit the screen. |
| `Enter` (numeric) | Commit the value typed in the criterion. |
| `Enter` (on a day, month or year of a calendar) | Choose it, without submitting the screen or saving the row of a grid. |
| `ArrowDown` (date input) | Go into the calendar. Opening the calendar keeps the focus on the input, so the date can be typed. |
| `Space` (suggest, empty) | Open the options of the suggest. |

### Menu search

| Keys | Action |
|------|--------|
| `ArrowDown` / `ArrowUp` | Go to the next or previous result. |
| `Enter` | Open the active result. |
| `Escape` | Close the search. |

### Dialogs

| Keys | Action |
|------|--------|
| `Escape` | Close the dialog. A confirm dialog closes with it and cancels its action, as `Cancel` does. Message dialogs do not close with it. |

## Register a shortcut

The React client registers its shortcuts with the `useKeyboardShortcut` hook and the helpers of
`src/utilities/keyboard.js`. The hook adds a listener while the component is mounted and removes it on unmount, so it
never replaces the handlers of other components (do not assign `window.onkeydown`).

```jsx
import useKeyboardShortcut from "../hooks/useKeyboardShortcut";

function MyComponent({onSave}) {
  // Listens to the window; pass {target: ref} to listen to one element
  useKeyboardShortcut([
    {shortcut: {key: "s", ctrl: true}, handler: (event) => { event.preventDefault(); onSave(); }}
  ]);
  return null;
}
```

* A shortcut has `key` (language dependent, for named keys such as `Enter`) or `code` (physical key, for digits and
  letters), both as a string or a list of strings.
* The modifiers `alt`, `shift`, `ctrl` and `meta` are exact: one that is not set must not be pressed. Set it to `ANY`
  (exported by the utility) to accept it either way, or spread `ANY_MODIFIERS` to accept every combination.
* For handlers written as `onKeyDown` props, `createKeyHandler(bindings)` returns a function that runs the first binding
  that matches and returns whether one did. A binding can have a `when(event)` predicate for a condition that is not a
  key (for example an empty input): the binding is skipped when it returns false.
* With `target: ref` the element is read when the effect runs, so it must be mounted together with the component. For an
  element that is rendered conditionally use an `onKeyDown` with `createKeyHandler` instead.
* The hook keeps the latest bindings, so they can change on every render without subscribing again. The options are
  `target`, `enabled`, `eventName` and `capture`.

Add every new shortcut to the `SHORTCUTS` catalogue in `src/utilities/keyboard.js` and to the tables above.
