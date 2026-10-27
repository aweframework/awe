const fs = require('fs');
const path = require('path');

// The horizontal tabs of the React engine as the AWE 5 prototype (Aura): muted medium labels, the active label and a 2px
// underline in the primary text, a line under the list, styled with the --awe-* tokens
const TABS = path.join(__dirname, '../../../src/components/AweTabs.less');

const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[},])\\s*${escape(selector)}\\s*\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('AWE 5 tabs styles', () => {
  const css = fs.readFileSync(TABS, 'utf8');
  const nav = '.p-tabmenu-container.orientation-horizontal > .p-tabmenu .p-tabmenu-nav';
  const tab = `${nav} .p-tabmenuitem .p-menuitem-link`;

  it('draws the list of tabs on the surface with a line under it and the gap of the prototype between the tabs', () => {
    const list = rule(css, nav);
    expect(list).toMatch(/gap: var\(--awe-space-4\);/);
    expect(list).toMatch(/background: none;/);
    expect(list).toMatch(/border-bottom: 1px solid var\(--awe-color-border\);/);
  });

  it('draws a tab as a muted medium label with the font of the page, without a box', () => {
    const link = rule(css, tab);
    expect(link).toMatch(/padding: var\(--awe-space-3\) var\(--awe-space-2\);/);
    expect(link).toMatch(/font: inherit;/);
    expect(link).toMatch(/font-weight: var\(--awe-font-weight-medium\);/);
    expect(link).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(link).toMatch(/background: none;/);
    expect(link).toMatch(/border-bottom: 2px solid transparent;/);
    expect(link).toMatch(/border-radius: 0;/);
    expect(rule(css, `${tab}:hover`)).toMatch(/color: var\(--awe-color-text-hover\);/);
  });

  it('marks the active tab with the primary text and a 2px underline', () => {
    const active = rule(css, `${nav} .p-tabmenuitem.p-highlight .p-menuitem-link`);
    expect(active).toMatch(/color: var\(--awe-color-primary-text\);/);
    expect(active).toMatch(/border-bottom-color: var\(--awe-color-primary-text\);/);
  });

  it('styles the tabs with tokens only', () => {
    const horizontal = withoutComments(css).slice(withoutComments(css).indexOf(nav));
    expect(horizontal.slice(0, horizontal.indexOf('@media'))).not.toMatch(/#[0-9a-fA-F]{3,8}\b|\b(?!1px\b|2px\b)\d+(\.\d+)?px\b/);
  });
});
