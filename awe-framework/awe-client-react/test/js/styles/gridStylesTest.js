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
