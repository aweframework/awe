const fs = require('fs');
const path = require('path');

// Row grouping of the React grid (#833): group and subtotal rows, their toggle and the Group by panel, styled with the
// --awe-* tokens only, in light and dark, with a visible focus ring and no motion when the user asks for less
const FILE = path.join(__dirname, '../../../src/components/AweGridGroups.less');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');

describe('Grid row grouping styles (#833)', () => {
  const css = withoutComments(fs.readFileSync(FILE, 'utf8'));

  it('uses tokens only: no colour literals, sizes or mode selectors', () => {
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|color-mix)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    expect(css.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em|ms|s)\b/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--(awe-|_)/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
  });

  it('keeps the grouping controls at the start of the toolbar: a spacer pushes Columns and Maximize to its end', () => {
    const grid = withoutComments(fs.readFileSync(path.join(__dirname, '../../../src/components/AweGrid.less'), 'utf8'));
    const spacer = grid.match(/\.grid-toolbar-spacer\s*\{([^}]*)\}/);
    expect(spacer).not.toBeNull();
    expect(spacer[1]).toMatch(/flex: 1 1 0;/);
    expect(spacer[1]).toMatch(/min-width: 0;/);
  });

  it('styles the group and subtotal rows and indents the label by level', () => {
    expect(css).toMatch(/tr\.grid-group-row/);
    expect(css).toMatch(/tr\.grid-subtotal-row/);
    expect(css).toMatch(/background: var\(--awe-color-surface-subtle\);/);
    expect(css).toMatch(/padding-left: calc\(var\(--_group-level, 0\) \* var\(--awe-size-control-sm\)\);/);
  });

  it('shows a focus ring on the toggle and stops the chevron rotation with reduced motion', () => {
    expect(css).toMatch(/\.grid-group-toggle\s*\{[^]*?&:focus-visible\s*\{\s*outline: var\(--awe-focus-ring\);/);
    expect(css).toMatch(/&\[aria-expanded="true"\] \.awe-icon\s*\{\s*rotate: 90deg;/);
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.grid-group-toggle \.awe-icon\s*\{\s*transition: none;/);
  });
});
