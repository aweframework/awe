const fs = require('fs');
const path = require('path');

// Advanced search of a window (#790): the advanced block (disclosure row, region below it), toggle, count badge, filter
// chips and side panel, styled with the --awe-* tokens only, in light and dark, with a visible focus ring and no motion
// when the user asks for less
const FILE = path.join(__dirname, '../../../src/components/AweWindowAdvanced.less');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');

describe('Window advanced search styles (#790)', () => {
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

  it('takes a whole line of the window body: the disclosure row that wraps, the filters in effect after a divider', () => {
    expect(css).toMatch(/\.window-advanced\s*\{[^}]*flex: 0 0 100%;[^}]*width: 100%;/);
    expect(css).toMatch(/\.window-advanced-row\s*\{[^}]*display: flex;[^}]*flex-wrap: wrap;[^}]*align-items: center;[^}]*gap: var\(--awe-space-2\);/);
    expect(css).toMatch(/\.window-filter-chips::before\s*\{[^}]*width: 1px;[^}]*height: var\(--awe-space-5\);[^}]*background: var\(--awe-color-border\);/);
    expect(css).not.toMatch(/\.window-advanced-bar|\.window-advanced-toolbar|margin-left: auto/);
  });

  it('opens the region of the advanced criteria below the row, where their columns flow, and hides it with CSS', () => {
    expect(css).toMatch(/\.window-advanced-region\s*\{[^}]*display: flex;[^}]*flex-wrap: wrap;/);
    expect(css).toMatch(/\.window-advanced-region\[hidden\]\s*\{\s*display: none;/);
    // The row reads as the separator between the essential and the advanced criteria
    expect(css).toMatch(/\.window-advanced-row\s*\{[^}]*padding-top: var\(--awe-space-1\);/);
    // The row keeps the gutter of the criteria columns, so the toggle and its hover start at the edge of the fields
    expect(css).toMatch(/\.window-advanced-row\s*\{[^}]*padding-inline: calc\(var\(--awe-gap-form-column\) \/ 2\);/);
    // The remove icon of a chip is centred in its button, not laid on the text baseline
    expect(css).toMatch(/\.window-filter-chip-remove\s*\{[^}]*line-height: 0;/);
    expect(css).toMatch(/\.window-filter-chip-remove \.awe-icon\s*\{[^}]*display: block;/);
    expect(css).toMatch(/\.window-advanced-region\s*\{[^}]*padding-top: var\(--awe-space-3\);/);
  });

  it('turns the chevron of the toggle while the region is open, without motion when the user asks for less', () => {
    expect(css).toMatch(/\.window-advanced-chevron\s*\{[^}]*transition: rotate var\(--awe-motion-duration-base\) var\(--awe-motion-easing-standard\);/);
    expect(css).toMatch(/\.window-advanced-toggle\[aria-expanded="true"\] \.window-advanced-chevron\s*\{\s*rotate: 180deg;/);
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^@]*\.window-advanced-chevron[^{]*\{\s*transition: none;/);
  });

  it('draws the filters in effect as dashed chips that wrap, and marks the hidden ones', () => {
    expect(css).toMatch(/\.window-filter-chips\s*\{[^}]*flex-wrap: wrap;/);
    expect(css).toMatch(/\.window-filter-chip\s*\{[^}]*border: 1px dashed var\(--awe-color-field-border\);/);
    expect(css).toMatch(/\.window-filter-chip-hidden\s*\{\s*background: var\(--awe-color-surface-subtle\);/);
  });

  it('draws the count badge with the highlight colours', () => {
    expect(css).toMatch(/\.window-advanced-count\s*\{[^}]*color: var\(--awe-color-on-highlight\);[^}]*background: var\(--awe-color-highlight-focus\);/);
  });

  it('shows a focus ring on its controls and stops the chip transition with reduced motion', () => {
    expect(css).toMatch(/\.window-advanced-toggle\s*\{[^]*?&:focus-visible\s*\{\s*outline: var\(--awe-focus-ring\);/);
    expect(css).toMatch(/\.window-filter-chip-remove\s*\{[^]*?&:focus-visible\s*\{\s*outline: var\(--awe-focus-ring\);/);
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^@]*\.window-filter-chip-remove[^{]*\{\s*transition: none;/);
  });
});
