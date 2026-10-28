---
id: components
title: Components
sidebar_label: Components
---

Rules per AWE element. Each component reads semantic or component tokens only, defines every state (hover, focus,
active, disabled, invalid, loading, empty) and works in both densities and both modes. The
[sample screen](pathname:///design-system/prototype/) and the
[AWE 5 components page](pathname:///design-system/prototype/components.html) show them live.

## Buttons

| Variant | Use |
|---------|-----|
| Primary | The main action of a screen, card or dialog. **One** per area. |
| Secondary | Frequent actions next to the primary (Import, Reset). |
| Outlined | Neutral actions, toolbars, button groups. |
| Text | Low-emphasis actions (History, Clear all, links inside cards). |
| Ghost icon | Toolbar and row actions. Always `aria-label` + tooltip. |
| Danger / danger outlined | Destructive actions. Filled only in the confirmation step. |

- Sizes: small (`--awe-size-control-sm`), default (`--awe-size-control`), large (`--awe-size-control-lg`, login).
- Label weight 500, icon on the left, 8px gap.
- Busy (#771): when the action a button launched is still running after about 400 ms, that button shows a spinner
  instead of its icon (before its label when it has none), stays disabled and is not faded; quick actions show
  nothing. The other buttons are disabled while actions run. `show-loading="false"` turns it off for a button (see
  [Button](../api/button.md)).
- Button groups share borders; a split button is a primary plus a chevron button.

| Do | Don't |
|----|-------|
| "Save changes", "Delete customers" | "OK", "Yes" |
| One primary per area | Two filled buttons side by side |
| Disable and explain (tooltip) | Hide an action the user expects |

## Criteria (form fields)

| Element | Rule |
|---------|------|
| Label | Above the field, `--awe-font-size-label`, weight 500. Required: red `*`. Optional hints right-aligned in muted text. |
| Field | Height `--awe-size-control`, `--awe-radius-field`, border `--awe-color-field-border`. Leading icon optional. |
| Focus | Border in the primary plus a soft halo. |
| Invalid | Red border, message under the field with an icon (`--awe-color-danger-text`), `aria-invalid` and `aria-describedby`. |
| Disabled / read-only | Disabled: muted background, no shadow. Read-only: filled background, value selectable. Locked: lock icon plus the reason (see #791). |
| Help | One line under the field in muted text. |
| Layout | Auto-fill grid of 232px minimum columns; wide fields span 2 columns. |

**Numbers.** Right-aligned, tabular figures, unit as a prefix or suffix (`€`, `days`), optional stepper.

### Select, multiselect and autocomplete

The native browser list is **not** used: it ignores the theme and the mode. Every select is a combobox with a listbox
popup in the AWE list style.

| Part | Rule |
|------|------|
| Popup | Overlay surface, border, `--awe-shadow-overlay`, 4px padding, 2px gap, top layer (never clipped by grids or dialogs). Opens below, or above when there is no room. |
| Option | 8px × 12px padding, `--awe-radius-list-option`. Hover/focus: `--awe-color-list-option-focus`. Selected: highlight background plus a check. |
| Groups | Optional group header (small, muted) and a separator. |
| Filter | A filter input at the top when there are more than 10 options. |
| Keyboard | ↑/↓, Home/End, Enter/Space, Esc, Tab closes, type-ahead also when closed. |
| ARIA | `role="combobox"` with `aria-expanded`, `aria-controls`, `aria-activedescendant`; `role="listbox"` and `role="option"` with `aria-selected`. |
| Multiselect | Selected values as removable chips in the field; the popup lists every option with a checkbox and "n of m" in the field. |
| Autocomplete | Same list style; the matched text is highlighted; options can have a second line (meta). |

### Dates and times

- Date: text field with a calendar button. Calendar: today outlined, selected filled with the primary.
- Month picker (#820): 3×4 grid of months.
- Date formats (#820): the display format is shown as a hint next to the label.
- Range (#839): one field with two inputs and an arrow; shortcuts (7 days, 1 month, 3 months, year to date,
  12 months, all) as pill buttons under the field; the end cannot be before the start.
- Time: text field with a clock icon, tabular figures.

### Segmented control (button radio)

- A track with a border; the selected segment has its own surface, border, shadow and semibold text
  (`--awe-segmented-*`). It must read in both modes.
- **Never clips.** Segments wrap; in a cell narrower than 300px they become a two-column grid at full width.
- Give it enough width in examples and forms: a four-option control spans two criteria columns.

### Checkbox, radio and switch

- Checkbox `--awe-size-checkbox`, `--awe-radius-sm`; supports indeterminate.
- Radio groups in a row, wrapping; disabled options keep their label visible.
- Switch for settings that apply at once; checkbox for values submitted with a form.

### Uploader (#841)

Drop zone (dashed border, highlight on drag-over), "browse" link, accepted types and size as help. Files listed with
icon, name, size, progress or status. An image shows a preview with Clear. A **Take photo** button opens the camera.
Read-only uploaders show the file and disable the actions.

## Grid

| Part | Rule |
|------|------|
| Card | The grid lives in a card; title and count in the card header. |
| Toolbar | Search in results, Filters (with count), spacer, Columns, Export (split button). |
| Active filters | Removable dashed chips under the toolbar, plus "Clear all". |
| Header | `--awe-grid-header-*`, sortable columns show an arrow; the sorted one in the primary. Sticky. |
| Rows | Cell padding `--awe-grid-cell-padding-*`, hover background, no zebra. |
| Selection | Checkbox column; selected rows use the highlight background; a selection bar shows the count and bulk actions. |
| Numbers | Right-aligned, tabular, negatives in the danger colour with a minus sign. |
| Status | Tags with a dot (`tag--success`, `--warning`, `--danger`, `--info`). |
| Inline edit | The edited row gets a primary outline, fields inside the cells (small controls), Save and Cancel in the actions column. |
| Row actions | Ghost icon buttons at the end of the row, or a menu button (#801). |
| Pager | "Showing 1–10 of 1,284", page size (select), pages with first/previous/next/last, "Go to page" (#826, #874). |
| Loading | Skeleton rows under the real header (#856). A refresh dims the current rows and shows a top loading bar. |
| Empty | Empty state inside the grid body (#837). |

| Do | Don't |
|----|-------|
| Keep the real header while loading | Show a spinner over an empty box |
| Put the key column first with a second line | Repeat the same data in two columns |
| Truncate with ellipsis and tooltip | Let a long text break the row height without a limit |

## Tabs and accordion

- Tabs: muted labels, active label and 2px underline in `--awe-color-primary-text`, optional count badge, ←/→ move.
  A tab with an error shows an error count (#827).
- Accordion: one header per section with an icon and a chevron; open sections animate; several can be open.

## Overlays

| Element | Rule |
|---------|------|
| Dialog | `<dialog>`, `--awe-radius-modal`, header (title, subtitle, close), scrollable body, footer with actions on the right. Mask `--awe-color-mask` with a slight blur. Sizes in #844. |
| Confirmation | Small dialog with a danger icon, a precise question ("Delete 2 customers?"), the consequence, Cancel focused by default. |
| Side panel (#843) | Slides from the right, full height, own footer of actions. For editing a record without leaving the list. |
| Popover (#843) | Anchored to its button, with an arrow; can hold criteria and a small grid; Cancel / Accept. |
| Dropdown menu | Overlay list of options with icons; destructive options last, in danger text, after a separator. |
| Toast | Top right, below the header, top layer (above dialogs). Success, info, warning, error; auto-dismiss 6s, close button. Errors use `role="alert"`. |
| Tooltip | Inverse surface (`--awe-color-tooltip`), small text, appears on hover and focus. Never the only place of essential information. |

## Feedback

- Messages: subtle background, border and icon per status; bold first words state the fact.
- Tags for states, badges for counts.
- Empty state: icon in a circle, title, one or two lines of explanation, at most two actions.
- Progress bar for known progress; loading bar for unknown progress.

## New AWE 5 elements

Grouped as in the [components page](pathname:///design-system/prototype/components.html). **Done** items are in
AWE 5 and are redesigned; **Planned** items are proposals.

### Grid features

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Multi-line cells with tooltip | #825 | Done | Up to N lines per column, then ellipsis; the full text in a tooltip. |
| Radio and button-bar columns | #838 | Done | One column holds a set of options; arrow keys change the value. Use the segmented control style. |
| Sparkline column | #855 | Done | Line, bars or area per row; negatives in danger; min, max and last marks. |
| Expandable row detail | #835 | Done | A chevron opens a detail area under the row (fields or a nested grid), marked with a primary left border. |
| Move rows | #842 | Done | Toolbar actions: top, up, down, bottom on the selected rows. |
| Data bars in cells | #831 | Planned | A light bar behind the number; negatives grow to the left in the danger tone. The number stays readable. |
| Secondary line, status and tags in cells | #832 | Done | Muted second line, status with a dot, tags collapsing into "+n". |
| Row grouping with subtotals | #833 | Planned | Collapsible group header with count and totals, a subtotal row per group, grand total in the footer. |
| Quick search and value filters | #834 | Planned | Search in every column; column filter with values and counts; footer totals say they are filtered. |
| Pager with range, total and go to page | #826, #874 | Done | See Grid. |
| Columns dialog per user | #813 | Done | Popover anchored to the Columns button by default (`columns-panel`: `popover`, `dialog` or `side-panel`), changes applied live. Show, hide, reorder (drag or arrows), Restore default, Cancel, Save. |

### Criteria and forms

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Month picker and date formats | #820 | Done | See Dates and times. |
| Date range with shortcuts | #839 | Done | See Dates and times. |
| Uploader with drop zone, preview and camera | #841 | Done | See Uploader. |
| Reveal the first invalid field across tabs | #827 | Done | On validation, open the tab with the first error, mark the tab, focus the field, show a summary with "Go to field". |
| Required to save vs. required to finish | #840 | Planned | `*` blocks saving; a second marker (diamond, warning tone, dashed border) is needed to finish; a pending list with links; Save draft stays enabled. |
| Advanced search | #790 | Planned | Conditions (field, operator, value), nested All/Any groups, saved searches per user. |
| Visible access restrictions | #791 | Planned | Locked fields and actions stay visible with a lock icon and the reason (help text or tooltip). |

### Overlays and panels

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Field context menu and row menu button | #801 | Done | Right-click or Shift+F10 on a field, button or tab; a menu button in a row opens that row's menu. |
| Maximize and restore | #799, #813 | Done | Fills the content area; Restore or Esc returns. |
| Side panel | #843 | Done | See Overlays. |
| Popover anchored to a button | #843 | Done | See Overlays. |
| Dialog sizes | #844 | Done | sm 360px, md 560px, lg 800px, xl 1140px, full screen. |

### Navigation and launchers

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Launcher pages with descriptions | #828 | Done | Cards with icon, title, one-line description and optional count; a launcher can open another one; the breadcrumb shows every level. |

### Data visualisation

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Pivot table | #821, #878 | Done | Drop zones for rows, columns and values; aggregation select; swap; totals per row and column. |
| Map | #798 | Done | Controls and panels use AWE surfaces and buttons; markers and routes in the primary. |
| Colour scale cells and grid without header | #830 | Done | 5-step scale from the primary with a legend; a headerless grid starts with its first row. |
| Card | #899 | Done | Metric tile: title, value (tabular) with unit, change as a tag with arrow and text (tone from `good-trend`), optional target progress, an optional status chip at the top right, before the icon (#836) and a body with any component; laid out with `awe-card-grid`. |
| KPI | #836 | Planned | Adds the filter mode on top of the card (#899): a selected state, and a click filters a grid through dependencies. |

### Feedback and loading

| Element | Issue | Status | Rule |
|---------|-------|--------|------|
| Skeletons: grid, screen, dim | #856, #869 | Done | Show the structure that is coming; a refresh dims instead of hiding. |
| Configurable grid empty state | #837 | Done | Own icon, title, text and action per grid. |
| Avatar without image and first-load splash | #864 | Done | Photo, initials, or a generic user icon; never an empty circle. Splash with the logo, then the screen skeleton. |
