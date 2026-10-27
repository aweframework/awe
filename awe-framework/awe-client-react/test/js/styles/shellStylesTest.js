const fs = require('fs');
const path = require('path');

// The AWE 5 shell of the React engine (#885): brand slot, side menu, header and page, styled with the --awe-* tokens
// over the classes of the home screens (the screen XML does not change)
const REPOSITORY = path.join(__dirname, '../../../../..');
const REACT_SRC = path.join(REPOSITORY, 'awe-framework', 'awe-client-react', 'src');
const HOME_SIDEBAR = [
  path.join(REPOSITORY, 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'application', 'awe-react',
    'screen', 'home', 'home-sidebar.xml'),
  path.join(REPOSITORY, 'awe-tests', 'awe-boot-react', 'src', 'main', 'resources', 'application', 'app', 'screen',
    'home-sidebar.xml')
];
const SHELL_STYLES = [
  'assets/css/shell.css',
  'assets/css/pages/home.css',
  'components/AweMenu.css',
  'components/AweMenuSearch.css',
  'templates/WindowTemplate.css'
];

const read = file => fs.readFileSync(path.join(REACT_SRC, file), 'utf8');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '');
const rule = (css, selector) => {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = withoutComments(css).match(new RegExp(`(?:^|[},])\\s*${escaped}\\s*\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('AWE 5 shell styles (#885)', () => {
  const shell = read('assets/css/shell.css');

  it('loads the shell styles after the layout styles', () => {
    const main = read('main.jsx');
    const layout = main.indexOf("import './assets/css/layout.css';");
    const shellImport = main.indexOf("import './assets/css/shell.css';");
    expect(layout).toBeGreaterThan(-1);
    expect(shellImport).toBeGreaterThan(layout);
  });

  it.each(SHELL_STYLES)('styles %s with tokens only: no colour literals, sizes or mode selectors', file => {
    const css = withoutComments(read(file));
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|color-mix)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Only hairlines and the breakpoints (a media query cannot read a token) are written in px: every other size comes
    // from a token
    expect(css.replace(/@media[^{]*/g, '').match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em)\b/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    // Only AWE tokens (and private variables of a rule), never the old theme variables
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--(awe-|_)/));
  });

  it('styles the classes that the home screen gives to the shell', () => {
    HOME_SIDEBAR.forEach(file => {
      const xml = fs.readFileSync(file, 'utf8');
      ['layout-container layout-static', 'layout-sidebar', 'sidebar-header', 'application-logo',
        'layout-menu-container', 'layout-content-wrapper', 'layout-topbar', 'topbar-start', 'topbar-end',
        'layout-content expand', 'target-action="layout-static-inactive"']
        .forEach(name => expect(xml).toContain(name));
    });
    ['.layout-sidebar', '.sidebar-header', '.layout-menu-container', '.layout-content-wrapper', '.layout-topbar',
      '.layout-content'].forEach(selector => expect(rule(shell, selector)).not.toBeNull());
  });

  it('sizes the side menu, the header and the page with the shell tokens', () => {
    expect(rule(shell, '.layout-sidebar')).toMatch(/width: var\(--awe-menu-width\);/);
    expect(rule(shell, '.layout-sidebar')).toMatch(/background: var\(--awe-menu-background\);/);
    expect(rule(shell, '.layout-content-wrapper')).toMatch(/margin-left: var\(--awe-menu-width\);/);
    expect(rule(shell, '.sidebar-header')).toMatch(/height: var\(--awe-header-height\);/);
    expect(rule(shell, '.layout-topbar')).toMatch(/height: var\(--awe-header-height\);/);
    expect(rule(shell, '.layout-topbar')).toMatch(/background: var\(--awe-header-background\);/);
    expect(rule(shell, '.layout-topbar')).toMatch(/position: sticky;/);
    expect(rule(shell, '.layout-content')).toMatch(/var\(--awe-page-padding\)/);
  });

  it('collapses the side menu to the icon rail on desktop and opens it as a drawer below 1024px', () => {
    const css = withoutComments(shell);
    const desktop = css.slice(css.indexOf('@media (min-width: 1025px)'));
    expect(desktop).toMatch(/\.layout-static-inactive \.layout-sidebar\s*\{[^}]*width: var\(--awe-menu-width-collapsed\);/);
    expect(desktop).toMatch(/\.layout-static-inactive \.layout-content-wrapper\s*\{[^}]*margin-left: var\(--awe-menu-width-collapsed\);/);
    // The nested levels of a rail option open as a flyout next to the rail
    expect(desktop).toMatch(/width: var\(--awe-menu-flyout-width\);/);
    const tablet = css.slice(css.indexOf('@media (max-width: 1024px)'));
    expect(tablet).toMatch(/\.layout-static-inactive \.layout-sidebar\s*\{[^}]*transform: none;/);
  });

  it('marks the active option, its trail and the nested levels with the menu tokens', () => {
    const css = withoutComments(shell);
    expect(css).toMatch(/\.p-menuitem-active[^{]*\{[^}]*background: var\(--awe-menu-item-active-background\);/);
    expect(css).toMatch(/\.p-menuitem-active-path[^{]*\{[^}]*color: var\(--awe-color-text-emphasis\);/);
    expect(css).toMatch(/padding-left: var\(--awe-menu-indent\);/);
    expect(css).toMatch(/padding-left: var\(--awe-menu-indent-l3\);/);
    expect(css).toMatch(/background: var\(--awe-menu-guide\);/);
    expect(css).toMatch(/background: var\(--awe-menu-guide-active\);/);
    expect(css).toMatch(/color: var\(--awe-menu-section-label\);/);
  });

  it('keeps the guide line of a nested level unbroken: the gap under an option stays inside its item', () => {
    // The gap is the margin of the option content: without a new formatting context it collapses out of the item,
    // and the guide line, which spans the item, shows a cut between options
    const css = withoutComments(shell);
    expect(css).toMatch(/\.p-panelmenu-content \.p-menuitem \{[^}]*position: relative;[^}]*display: flow-root;/);
  });

  it('draws the AWE wordmark with the emphasis text colour, so the logo reads in light and dark mode', () => {
    const home = withoutComments(read('assets/css/pages/home.css'));
    expect(home).toMatch(/h1\.application-logo\s*\{[^}]*height: var\(--awe-header-logo-height\);/);
    expect(home).toMatch(/awe-logo-mark\.svg/);
    expect(home).toMatch(/h1\.application-logo::after\s*\{[^}]*background-color: var\(--awe-color-text-emphasis\);[^}]*mask:[^;]*awe-logo-wordmark\.svg/);
    // The two layers come from the same artwork, so they share the frame of the logo
    const viewBox = file => read(`assets/images/logos/${file}`).match(/viewBox="([^"]+)"/)[1];
    expect(viewBox('awe-logo-mark.svg')).toBe(viewBox('awe-logo2.svg'));
    expect(viewBox('awe-logo-wordmark.svg')).toBe(viewBox('awe-logo2.svg'));
  });

  it('removes the old template shell rules that the new shell replaces', () => {
    const layout = withoutComments(read('assets/css/layout.css'));
    expect(layout).not.toMatch(/\.layout-wrapper\b|\.layout-config\b|\.layout-sidebar\b|\.layout-topbar\b/);
    // The launcher buttons of the menu screens stay
    expect(layout).toMatch(/\.menu-screen-button\s*\{/);
  });
});
