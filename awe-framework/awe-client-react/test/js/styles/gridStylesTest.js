const fs = require('fs');
const path = require('path');

// The AWE 5 grid of the React engine (#885): header, rows, cells, toolbar, paginator, loading, empty state, columns
// dialog, row detail and tree grid styled with the --awe-* tokens over the PrimeReact markup (the screen XML and the test
// hooks do not change)
const SRC = path.join(__dirname, '../../../src');
const GRID_STYLES = ['components/AweGrid.less', 'components/AweTreeGrid.less', 'components/AweGridSkeleton.less',
  'components/AweGridEmptyState.less', 'components/AweGridColumnsDialog.less', 'columns/Columns.less',
  'columns/ColumnTextView.less'];

const read = file => fs.readFileSync(path.join(SRC, file), 'utf8');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule whose selector list holds the selector
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[},])\\s*(?:[^{}]*,\\s*)?${escape(selector)}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('AWE 5 grid styles (#885)', () => {
  const grid = read('components/AweGrid.less');

  it.each(GRID_STYLES)('styles %s with tokens only: no colour literals, sizes or mode selectors', file => {
    const css = withoutComments(read(file));
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|color-mix)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Only hairlines are written in px: every other size comes from a token
    expect(css.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em|ms|s)\b/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--(awe-|_|cell-max-lines|grid-row-height)/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
  });

  it('draws the header in the grid header style, aligned like the cells of its column', () => {
    const header = rule(grid, '.p-datatable .p-datatable-thead > tr > th');
    expect(header).toMatch(/font-size: var\(--awe-grid-header-font-size\);/);
    expect(header).toMatch(/font-weight: var\(--awe-grid-header-font-weight\);/);
    expect(rule(grid, '.p-treetable .p-treetable-thead > tr > th')).toBe(header);
    expect(rule(grid, '.p-datatable .p-datatable-thead > tr > th.p-align-right > .p-column-header-content'))
      .toMatch(/justify-content: flex-end;/);
    expect(rule(grid, '.p-datatable .p-datatable-thead > tr > th.p-align-left > .p-column-header-content'))
      .toMatch(/justify-content: flex-start;/);
  });

  it('fades the arrow of a column that is not sorted and shows the sorted one in the primary', () => {
    expect(rule(grid, '.p-datatable .p-sortable-column:not(.p-highlight) .p-sortable-column-icon')).toMatch(/opacity: /);
    expect(rule(grid, '.p-datatable .p-sortable-column .p-sortable-column-icon')).toMatch(/width: var\(--awe-size-icon-sm\);/);
  });

  it('shows the figures of the cells in tabular numbers', () => {
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td')).toMatch(/font-variant-numeric: var\(--awe-font-variant-numeric\);/);
    expect(rule(grid, '.p-treetable .p-treetable-tfoot > tr > td')).toMatch(/font-variant-numeric: var\(--awe-font-variant-numeric\);/);
  });

  it('keeps the cells on one line whatever the grid, as the prototype, and the editors of an edited row in their cell', () => {
    // Not only on the resizable grids (PrimeReact): a grid with a grouped header is not resizable
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td')).toMatch(/white-space: nowrap;/);
    expect(rule(grid, '.p-datatable .p-cell-text.p-cell-wrap')).toMatch(/white-space: normal;/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.editing > td')).toMatch(/overflow: hidden;/);
  });

  it('draws the choose button of an uploader in an edited row as an addon of its field, at the small control height', () => {
    const choose = rule(grid, '.p-datatable .p-datatable-tbody > tr.editing .p-inputgroup .p-fileupload .p-button');
    // The delete button of an uploaded file too
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.editing .p-inputgroup > .p-button')).toBe(choose);
    expect(choose).toMatch(/height: var\(--awe-size-control-sm\);/);
    expect(choose).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    expect(choose).toMatch(/color: var\(--awe-color-text\);/);
    expect(choose).toMatch(/background: var\(--awe-color-field\);/);
    expect(choose).toMatch(/border: 1px solid var\(--awe-color-field-border\);/);
  });

  it('paints the frozen cells opaque, with the colour of their row on top, so the cells scrolling under them do not show', () => {
    const layered = token => `background: linear-gradient(var(${token}), var(${token})), var(--awe-grid-row-background);`;
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td.p-frozen-column')).toMatch(/background: var\(--awe-grid-row-background\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.p-highlight > td.p-frozen-column'))
      .toContain(layered('--awe-grid-row-selected-background'));
    expect(rule(grid, '.p-datatable.p-datatable-hoverable-rows .p-datatable-tbody > tr:not(.p-highlight):hover > td.p-frozen-column'))
      .toContain(layered('--awe-grid-row-hover-background'));
    expect(rule(grid, '.p-datatable.p-datatable-striped .p-datatable-tbody > tr.p-row-odd:not(.p-highlight) > td.p-frozen-column'))
      .toContain(layered('--awe-color-surface-subtle'));
  });

  it('keeps the background of the edited row in its frozen cells, also when the row is selected or hovered', () => {
    const edited = rule(grid, '.p-datatable .p-datatable-tbody > tr.editing > td.p-frozen-column');
    expect(edited).toMatch(/background: var\(--awe-grid-row-background\);/);
    expect(rule(grid, '.p-datatable.p-datatable-hoverable-rows .p-datatable-tbody > tr.editing:hover > td.p-frozen-column')).toBe(edited);
  });

  it('keeps the row edit column narrow: small padding and a small gap between the save and cancel buttons', () => {
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td.grid-row-editor-column')).toMatch(/padding-inline: var\(--awe-space-2\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td.grid-row-editor-column .p-row-editor-save'))
      .toMatch(/margin-right: var\(--awe-space-1\);/);
  });

  it('draws the addon of a field in an edited row at the small control height of the row', () => {
    const addon = rule(grid, '.p-datatable .p-datatable-tbody > tr.editing .p-inputgroup-addon');
    expect(addon).toMatch(/height: var\(--awe-size-control-sm\);/);
    expect(addon).toMatch(/min-width: var\(--awe-size-control-sm\);/);
  });

  it('separates the row edit column, frozen at the right, from the columns that scroll under it', () => {
    expect(rule(grid, '.p-datatable .grid-row-editor-column')).toMatch(/box-shadow: inset 1px 0 0 var\(--awe-grid-border\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.editing > td.grid-row-editor-column'))
      .toMatch(/box-shadow: inset 0 1px 0 var\(--awe-color-primary\), inset 1px 0 0 var\(--awe-grid-border\);/);
  });

  it('marks the edited row and the row detail with a primary line on their left', () => {
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.editing > td')).toMatch(/border-bottom-color: var\(--awe-color-primary\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.editing > td:first-child'))
      .toMatch(/inset var\(--awe-grid-row-marker-width\) 0 0 var\(--awe-color-primary\)/);
    const detail = rule(grid, '.p-datatable .p-datatable-tbody > tr.p-datatable-row-expansion > td .grid-row-detail');
    expect(detail).toMatch(/border-left: var\(--awe-grid-row-marker-width\) solid var\(--awe-color-primary\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.p-datatable-row-expansion > td')).toMatch(/background: var\(--awe-color-surface-subtle\);/);
  });

  it('draws the row grip and the drop line of a grid with move-rows with the grid tokens (#888)', () => {
    expect(rule(grid, '.p-datatable .grid-row-grip')).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(rule(grid, '.p-datatable .grid-row-grip:hover')).toMatch(/color: var\(--awe-color-primary\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.grid-row-drop-before > td'))
      .toMatch(/box-shadow: inset 0 var\(--awe-grid-drop-indicator-width\) 0 0 var\(--awe-color-primary\);/);
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.grid-row-drop-after > td'))
      .toMatch(/box-shadow: inset 0 calc\(-1 \* var\(--awe-grid-drop-indicator-width\)\) 0 0 var\(--awe-color-primary\);/);
  });

  it('keeps the sticky header of a grid over the nested grid of a row detail that scrolls under it', () => {
    // Both sticky headers take the same z-index from PrimeReact: the detail isolates the nested one
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr.p-datatable-row-expansion > td')).toMatch(/isolation: isolate;/);
  });

  it('stops what scrolls into view in a grid under its sticky header, not behind it', () => {
    // The height of a header row: its padding, the line of its text and its bottom border
    expect(rule(grid, '.p-datatable-scrollable > .p-datatable-wrapper')).toMatch(new RegExp(escape(
      'scroll-padding-top: calc(2 * var(--awe-grid-cell-padding-y) + var(--awe-grid-header-font-size) * var(--awe-line-height-snug) + 1px);')));
  });

  it('stops what scrolls into view in an editable grid before its row edit column, frozen at the right, not behind it', () => {
    // The width of the row edit column of an edited row: its save and cancel buttons, their gap and its padding
    expect(rule(grid, '.p-datatable-scrollable > .p-datatable-wrapper:has(.grid-row-editor-column)')).toMatch(new RegExp(escape(
      'scroll-padding-inline-end: calc(2 * var(--awe-size-control-sm) + var(--awe-space-1) + 2 * var(--awe-space-2));')));
  });

  it('draws the selection bar on the highlight, hidden until there are selected rows', () => {
    const bar = rule(grid, '.grid-selection-bar');
    expect(bar).toMatch(/display: none;/);
    expect(bar).toMatch(/padding: var\(--awe-space-2\) var\(--awe-grid-cell-padding-x\);/);
    expect(bar).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    expect(bar).toMatch(/color: var\(--awe-color-on-highlight\);/);
    expect(bar).toMatch(/background: var\(--awe-color-highlight\);/);
    expect(bar).toMatch(/border-bottom: 1px solid var\(--awe-grid-border\);/);
    const visible = rule(grid, '.grid-selection-bar.is-visible');
    expect(visible).toMatch(/display: flex;/);
    expect(visible).toMatch(/animation: awe-grid-selection-bar-in var\(--awe-motion-duration-fast\) var\(--awe-motion-easing-enter\);/);
    expect(withoutComments(grid)).toMatch(/@media \(prefers-reduced-motion: reduce\) \{\s*\.grid-selection-bar\.is-visible \{\s*animation: none;/);
    expect(rule(grid, '.grid-selection-bar .grid-selection-bar-spacer')).toMatch(/flex: 1;/);
  });

  it('draws the buttons of the selection bar small and ghost, and the danger one filled with the danger colour', () => {
    const small = rule(grid, '.grid-selection-bar .p-button.p-button-sm');
    expect(small).toMatch(/height: var\(--awe-size-control-sm\);/);
    expect(small).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    const ghost = rule(grid, '.grid-selection-bar .p-button.p-button-text');
    expect(ghost).toMatch(/color: inherit;/);
    expect(ghost).toMatch(/background: transparent;/);
    expect(rule(grid, '.grid-selection-bar .p-button.p-button-text:enabled:hover')).toMatch(/background: var\(--awe-color-surface-hover\);/);
    const danger = rule(grid, '.grid-selection-bar .p-button.p-button-danger');
    expect(danger).toMatch(/color: var\(--awe-color-on-danger\);/);
    expect(danger).toMatch(/background: var\(--awe-color-danger\);/);
    expect(rule(grid, '.grid-selection-bar .p-button.p-button-danger:enabled:hover')).toMatch(/background: var\(--awe-color-danger-hover\);/);
    expect(rule(grid, '.grid-selection-bar .p-button:focus-visible')).toMatch(/var\(--awe-color-focus-ring\)/);
  });

  it('draws the search as a small field with the field tokens at the start of the toolbar', () => {
    const search = rule(grid, '.grid-search');
    expect(search).toMatch(/height: var\(--awe-size-control-sm\);/);
    expect(search).toMatch(/margin-inline-end: auto;/);
    expect(search).toMatch(/color: var\(--awe-color-field-text\);/);
    expect(search).toMatch(/background: var\(--awe-color-field\);/);
    expect(search).toMatch(/border: 1px solid var\(--awe-color-field-border\);/);
    expect(search).toMatch(/border-radius: var\(--awe-radius-field\);/);
    expect(rule(grid, '.grid-search:has(+ .grid-quick-filter)')).toMatch(/margin-inline-end: 0;/);
    expect(rule(grid, '.grid-search:hover')).toMatch(/border-color: var\(--awe-color-field-border-hover\);/);
    const focused = rule(grid, '.grid-search:focus-within');
    expect(focused).toMatch(/border-color: var\(--awe-color-field-border-focus\);/);
    expect(focused).toMatch(/box-shadow: var\(--awe-shadow-field-focus\);/);
    expect(rule(grid, '.grid-search .grid-search-icon')).toMatch(/color: var\(--awe-color-field-icon\);/);
    expect(rule(grid, '.grid-search input::placeholder')).toMatch(/color: var\(--awe-color-field-placeholder\);/);
    expect(rule(grid, '.grid-search .grid-search-clear:hover')).toMatch(/background: var\(--awe-color-surface-hover\);/);
    expect(rule(grid, '.grid-search .grid-search-clear:focus-visible')).toMatch(/outline: var\(--awe-focus-ring\);/);
    expect(withoutComments(grid)).toMatch(/@media \(prefers-reduced-motion: reduce\) \{\s*\.grid-search \{\s*transition: none;/);
  });

  it('draws the icon of a cell as a small icon before the text, and a status with icon as a plain tag without dot (#793)', () => {
    const icon = rule(grid, '.awe-cell-icon');
    expect(icon).toMatch(/display: inline-flex;/);
    expect(icon).toMatch(/margin-inline-end: var\(--awe-space-1-5\);/);
    expect(icon).toMatch(/vertical-align: middle;/);
    const svg = rule(grid, '.awe-cell-icon > .awe-icon');
    expect(svg).toMatch(/width: var\(--awe-size-icon-sm\);/);
    expect(svg).toMatch(/height: var\(--awe-size-icon-sm\);/);
    expect(rule(grid, '.awe-cell-status-plain::before')).toMatch(/display: none;/);
    expect(rule(grid, '.awe-cell-icon-text')).toMatch(/white-space: nowrap;[^]*text-overflow: ellipsis;/);
  });

  it('centres the icon of the selection bar with its text: its wrapper takes the height of the icon (#793)', () => {
    expect(rule(grid, '.grid-selection-bar .grid-selection-bar-icon > i')).toMatch(/display: inline-flex;/);
  });

  it('keeps the icon-only buttons of adjoining cells close, as the row actions of the prototype (#793)', () => {
    expect(rule(grid, '.p-datatable .p-datatable-tbody > tr > td:has(> .p-button.no-label)'))
      .toMatch(/padding-inline: var\(--awe-space-0-5\);/);
  });

  it('draws status tags and chips as rectangles with slightly rounded corners, with a round dot (#793)', () => {
    expect(rule(grid, '.awe-cell-status')).toMatch(/border-radius: var\(--awe-radius-sm\);/);
    expect(rule(grid, '.awe-cell-chip')).toMatch(/border-radius: var\(--awe-radius-sm\);/);
    // The dot is nested in the status rule (&::before) and stays round
    expect(read('components/AweGrid.less'))
      .toMatch(/\.awe-cell-status \{[^}]*&::before \{[^}]*border-radius: var\(--awe-radius-full\);/);
  });

  it('draws the avatar of a cell as a small circle of initials before the text, as the avatar of the prototype (#793)', () => {
    expect(rule(grid, '.awe-cell-entity')).toMatch(/display: flex;[^]*align-items: center;[^]*gap: var\(--awe-space-3\);/);
    const avatar = rule(grid, '.awe-cell-avatar');
    expect(avatar).toMatch(/width: var\(--awe-size-control-sm\);/);
    expect(avatar).toMatch(/height: var\(--awe-size-control-sm\);/);
    expect(avatar).toMatch(/flex: none;/);
    expect(avatar).toMatch(/border-radius: var\(--awe-radius-full\);/);
    expect(avatar).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(avatar).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(avatar).toMatch(/color: var\(--awe-color-on-secondary\);/);
    expect(avatar).toMatch(/background-color: var\(--awe-color-secondary\);/);
    const spacer = rule(grid, '.awe-cell-avatar-spacer');
    expect(spacer).toMatch(/width: var\(--awe-size-control-sm\);/);
    expect(spacer).not.toMatch(/background/);
    expect(rule(grid, '.awe-cell-avatar-primary')).toMatch(/color: var\(--awe-color-on-primary-subtle\);[^]*background-color: var\(--awe-color-primary-subtle\);/);
    ['info', 'success', 'warning', 'danger'].forEach(tone => expect(rule(grid, `.awe-cell-avatar-${tone}`))
      .toMatch(new RegExp(`color: var\\(--awe-color-${tone}-text\\);[^]*background-color: var\\(--awe-color-${tone}-subtle\\);`)));
  });

  it('draws the quick filters as a small segmented control at the start of the toolbar', () => {
    const group = rule(grid, '.grid-quick-filter');
    expect(group).toMatch(/margin-inline-end: auto;/);
    expect(group).toMatch(/background: var\(--awe-segmented-track\);/);
    expect(group).toMatch(/border: 1px solid var\(--awe-segmented-border\);/);
    const segment = rule(grid, '.grid-quick-filter .grid-quick-filter-segment');
    expect(segment).toMatch(/height: calc\(var\(--awe-size-control-sm\) - 2 \* \(var\(--awe-space-0-5\) \+ 1px\)\);/);
    expect(segment).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(segment).toMatch(/color: var\(--awe-segmented-text\);/);
    const chosen = rule(grid, '.grid-quick-filter input:checked + .grid-quick-filter-segment');
    expect(chosen).toMatch(/color: var\(--awe-segmented-selected-text\);/);
    expect(chosen).toMatch(/background: var\(--awe-segmented-selected\);/);
    expect(chosen).toMatch(/border-color: var\(--awe-segmented-selected-border\);/);
    expect(rule(grid, '.grid-quick-filter .grid-quick-filter-option:hover > .grid-quick-filter-segment'))
      .toMatch(/color: var\(--awe-segmented-hover-text\);/);
    expect(rule(grid, '.grid-quick-filter input:focus-visible + .grid-quick-filter-segment')).toMatch(/outline: var\(--awe-focus-ring\);/);
    expect(rule(grid, '.grid-quick-filter input:disabled + .grid-quick-filter-segment')).toMatch(/opacity: var\(--awe-opacity-disabled\);/);
    expect(rule(grid, '.grid-quick-filter .grid-quick-filter-count')).toMatch(/color: var\(--awe-color-text-muted\);/);
    ['danger', 'warning', 'success', 'info'].forEach(tone => {
      expect(rule(grid, `.grid-quick-filter .grid-quick-filter-count[data-tone="${tone}"]`)).toMatch(new RegExp(`color: var\\(--awe-color-${tone}-text\\);`));
    });
    expect(withoutComments(grid)).toMatch(/@media \(prefers-reduced-motion: reduce\) \{\s*\.grid-quick-filter \.grid-quick-filter-segment \{\s*transition: none;/);
  });

  it('places the toolbar and the paginator inside the grid borders, with the page sizes of the density', () => {
    const toolbar = rule(grid, '.grid-toolbar');
    expect(toolbar).toMatch(/gap: var\(--awe-space-2\);/);
    // The inset of the footer, so the last button (Maximize) is off the edge of the grid
    expect(toolbar).toMatch(/padding: var\(--awe-space-2\) var\(--awe-grid-cell-padding-x\);/);
    expect(toolbar).toMatch(/border-bottom: 1px solid var\(--awe-grid-border\);/);
    const paginator = rule(grid, '.grid-container .p-paginator');
    expect(paginator).toMatch(/border-top: 1px solid var\(--awe-grid-border\);/);
    expect(paginator).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    expect(rule(grid, '.grid-container .p-paginator .pagination-goto-page .pagination-goto')).toMatch(/height: var\(--awe-size-control-sm\);/);
  });

  it('shows a loading bar on top of the dimmed rows of a refresh, stopped with reduced motion', () => {
    const bar = rule(grid, '.grid-container.grid-reloading::before');
    expect(bar).toMatch(/var\(--awe-color-loading-bar\)/);
    expect(bar).toMatch(/background-color: var\(--awe-color-primary-subtle\);/);
    expect(withoutComments(grid)).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.grid-container\.grid-reloading::before\s*\{\s*animation: none;/);
  });

  it('draws the empty state as in the prototype: icon in a circle, title and text', () => {
    const empty = read('components/AweGridEmptyState.less');
    expect(rule(empty, '.grid-empty-icon')).toMatch(/background: var\(--awe-color-secondary\);/);
    expect(rule(empty, '.grid-empty-title')).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    expect(rule(empty, '.grid-empty-text')).toMatch(/color: var\(--awe-color-text-muted\);/);
  });

  it('pads the skeleton rows like the cells of the grid', () => {
    const skeleton = read('components/AweGridSkeleton.less');
    expect(rule(skeleton, '.grid-skeleton-row')).toMatch(/border-bottom: 1px solid var\(--awe-grid-border\);/);
    expect(rule(skeleton, '.grid-skeleton-cell')).toMatch(/padding: var\(--awe-grid-cell-padding-y\) var\(--awe-grid-cell-padding-x\);/);
  });

  it('indents the tree grid and draws its expander at the tree toggle size', () => {
    const tree = read('components/AweTreeGrid.less');
    expect(rule(tree, '.p-treetable-expander-column')).toMatch(/gap: var\(--awe-space-1-5\);/);
  });

  it('draws the button radio column as a segmented control and the text view with a focus ring', () => {
    const columns = read('columns/Columns.less');
    expect(rule(columns, '.column-button-radio')).toMatch(/background: var\(--awe-segmented-track\);/);
    expect(rule(columns, '.column-button-radio .p-button.p-highlight')).toMatch(/background: var\(--awe-segmented-selected\);/);
    expect(rule(read('columns/ColumnTextView.less'), '.text-view:focus-visible')).toMatch(/outline: var\(--awe-focus-ring\);/);
  });

  it('keeps the actions of the Columns in one line', () => {
    const css = read('components/AweGridColumnsDialog.less');
    expect(rule(css, '.grid-columns-actions')).toMatch(/flex-wrap:\s*nowrap/);
    expect(rule(css, '.grid-columns-actions > .p-button')).toMatch(/flex:\s*none/);
    expect(rule(css, '.grid-columns-actions > .p-button')).toMatch(/white-space:\s*nowrap/);
  });

  it('keeps the Save of the Columns on its colour while it saves (disabled, not dimmed)', () => {
    const css = read('components/AweGridColumnsDialog.less');
    expect(rule(css, '.grid-columns-actions > .p-button.is-loading:disabled')).toMatch(/opacity:\s*1;/);
    expect(rule(css, '.grid-columns-actions > .p-button.is-loading:disabled')).toMatch(/cursor:\s*progress;/);
  });

  it('drops the margin of a grid in a panel when it is maximized, so it covers the maximize target exactly', () => {
    const css = read('components/AweGrid.less');
    expect(rule(css, '.p-panel-content > .grid-container.maximized')).toMatch(/margin:\s*0;/);
  });
});
