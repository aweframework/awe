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

  it('draws a separator without label of the side menu as a divider line, as the footer line of the prototype', () => {
    const css = withoutComments(shell);
    const divider = '.awe-menu-container-vertical .p-panelmenu .p-panelmenu-panel.p-menuitem-divider';
    expect(rule(css, divider)).toMatch(/border-top: 1px solid var\(--awe-menu-border\);/);
    expect(rule(css, `${divider} > .p-panelmenu-header`)).toMatch(/display: none;/);
  });

  it('gives the side menu a footer with the AWE brand and the button that collapses the menu', () => {
    HOME_SIDEBAR.forEach(file => {
      const xml = fs.readFileSync(file, 'utf8');
      const footer = xml.indexOf('style="sidebar-footer"');
      expect(footer).toBeGreaterThan(xml.indexOf('style="layout-menu-container"'));
      expect(footer).toBeLessThan(xml.indexOf('style="layout-content-wrapper"'));
      const markup = xml.slice(footer, xml.indexOf('style="layout-content-wrapper"'));
      expect(markup).toMatch(/style="sidebar-powered"[\s\S]*label="SIDEBAR_POWERED_BY"[\s\S]*type="strong" label="SIDEBAR_POWERED_BRAND"/);
      expect(markup).toMatch(/<info-button id="collapse-menu" title="BUTTON_TOGGLE_MENU" icon="lucide:chevrons-left" style="sidebar-collapse">/);
      expect(markup).toMatch(/<button-action type="toggle-class" target-action="layout-static-inactive" target="\.layout-container"\/>/);
    });
  });

  it('toggles the side menu from the header with the panel-left icon of the prototype', () => {
    HOME_SIDEBAR.forEach(file => {
      expect(fs.readFileSync(file, 'utf8')).toMatch(/<info-button id="toggle-menu" title="BUTTON_TOGGLE_MENU" icon="lucide:panel-left">/);
    });
  });

  it('draws the footer of the side menu as the prototype: a line over the muted brand and the collapse button', () => {
    const css = withoutComments(shell);
    const footer = rule(css, '.sidebar-footer');
    expect(footer).toMatch(/display: flex;/);
    expect(footer).toMatch(/flex: none;/);
    expect(footer).toMatch(/border-top: 1px solid var\(--awe-menu-border\);/);
    const powered = rule(css, '.sidebar-powered');
    expect(powered).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(powered).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(rule(css, '.sidebar-powered strong')).toMatch(/color: var\(--awe-color-text\);/);
    expect(rule(css, '.sidebar-powered::before')).toMatch(/background: url\("\.\.\/images\/logos\/awe-logo-mark\.svg"\)/);
    // Collapsed rail, as the prototype: no brand, only the button, centred and turned to expand the menu
    const rail = css.slice(css.indexOf('@media (min-width: 1025px)'));
    expect(rail).toMatch(/\.layout-static-inactive \.sidebar-powered\s*\{[^}]*display: none;/);
    expect(rail).toMatch(/\.layout-static-inactive \.sidebar-footer\s*\{[^}]*justify-content: center;/);
    expect(rail).toMatch(/\.layout-static-inactive \.sidebar-collapse \.p-button-icon\s*\{[^}]*transform: rotate\(180deg\);/);
  });

  it('draws the buttons that collapse the menu as the icon buttons of the prototype: square with rounded corners', () => {
    const css = withoutComments(shell);
    expect(rule(css, '.layout-topbar .p-button.p-button-rounded,\n.sidebar-footer .sidebar-collapse.p-button'))
      .toMatch(/border-radius: var\(--awe-radius-field\);/);
  });

  it('shows the initials of the user in the avatar of the user menu, as the prototype', () => {
    HOME_SIDEBAR.forEach(file => {
      const avatar = fs.readFileSync(file, 'utf8').match(/<avatar id="ButUsrAct"[^>]*>/)[0];
      // With an icon the avatar would show it instead of the initials
      expect(avatar).not.toMatch(/\sicon=/);
    });
    const initials = rule(withoutComments(shell), '.layout-topbar .avatar-component .p-avatar');
    // As high as the hover of the icon buttons of the header
    expect(initials).toMatch(/width: var\(--awe-size-control\);/);
    expect(initials).toMatch(/height: var\(--awe-size-control\);/);
    expect(initials).toMatch(/margin: 0;/);
    expect(initials).toMatch(/color: var\(--awe-color-on-primary-subtle\);/);
    expect(initials).toMatch(/background: var\(--awe-color-primary-subtle\);/);
  });

  it('hides the option search on the collapsed rail, as the prototype', () => {
    const rail = withoutComments(shell).slice(withoutComments(shell).indexOf('@media (min-width: 1025px)'));
    expect(rail).toMatch(/\.layout-static-inactive \.awe-menu-search\s*\{[^}]*display: none;/);
  });

  it('shows on the collapsed rail only the options of the flyout, not the groups expanded on the path to the open screen', () => {
    const rail = withoutComments(shell).slice(withoutComments(shell).indexOf('@media (min-width: 1025px)'));
    expect(rail).toMatch(/\.layout-static-inactive \.awe-menu-container-vertical \.p-panelmenu-panel:not\(\.awe-menu-flyout\) > \.p-toggleable-content\s*\{[^}]*display: none;/);
    expect(rail).not.toMatch(/:not\(:hover, :focus-within\)/);
  });

  it('places the flyout of the collapsed rail at the top its option measures, and keeps it in the window', () => {
    const rail = withoutComments(shell).slice(withoutComments(shell).indexOf('@media (min-width: 1025px)'));
    const flyout = rail.match(/\.layout-static-inactive \.awe-menu-container-vertical \.p-panelmenu-panel > \.p-toggleable-content\s*\{([^}]*)\}/)[1];
    expect(flyout).toMatch(/top: var\(--_flyout-top\);/);
    expect(flyout).toMatch(/max-height: calc\(100vh - var\(--_flyout-top\) - var\(--awe-space-2\)\);/);
  });

  it('emphasises the options of the first list of the flyout of the collapsed rail, as the prototype', () => {
    const rail = withoutComments(shell).slice(withoutComments(shell).indexOf('@media (min-width: 1025px)'));
    const first = rail.match(/\.layout-static-inactive \.awe-menu-container-vertical \.p-panelmenu-root-list > \.p-menuitem > \.p-menuitem-content \.p-menuitem-link \.p-menuitem-text\s*\{([^}]*)\}/)[1];
    expect(first).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(first).toMatch(/color: var\(--awe-color-text-emphasis\);/);
  });

  it('titles the flyout of the collapsed rail with its option, as the prototype', () => {
    const rail = withoutComments(shell).slice(withoutComments(shell).indexOf('@media (min-width: 1025px)'));
    const title = rail.match(/\.p-panelmenu-panel > \.p-toggleable-content::before\s*\{([^}]*)\}/)[1];
    expect(title).toMatch(/content: attr\(data-flyout-title\);/);
    expect(title).toMatch(/color: var\(--awe-menu-section-label\);/);
    expect(title).toMatch(/text-transform: uppercase;/);
  });

  it('lists the options of the header dropdowns as the options of the prototype, not as rounded centred buttons', () => {
    const css = withoutComments(shell);
    const option = rule(css, '.info-dropdown .p-overlaypanel-content .p-button.p-button-text');
    expect(option).toMatch(/justify-content: flex-start;/);
    expect(option).toMatch(/width: 100%;/);
    expect(option).toMatch(/padding: var\(--awe-list-option-padding\);/);
    expect(option).toMatch(/color: var\(--awe-color-list-option-text\);/);
    expect(option).toMatch(/border-radius: var\(--awe-radius-list-option\);/);
    expect(rule(css, '.info-dropdown .p-overlaypanel-content .p-button.p-button-text:not(:disabled):hover'))
      .toMatch(/background: var\(--awe-color-list-option-focus\);/);
    expect(rule(css, '.info-dropdown .p-overlaypanel-content .p-button.p-button-text .p-button-icon'))
      .toMatch(/color: var\(--awe-color-list-option-icon\);/);
    expect(rule(css, '.info-dropdown .dropdown-separator')).toMatch(/background: var\(--awe-color-overlay-border\);/);
    expect(rule(css, '.info-dropdown .p-overlaypanel-content .p-button.p-button-text.dropdown-option-danger,\n.info-dropdown .p-overlaypanel-content .p-button.p-button-text.dropdown-option-danger .p-button-icon'))
      .toMatch(/color: var\(--awe-color-danger-text\);/);
    expect(rule(css, '.avatar-dropdown.info-dropdown')).toMatch(/width: var\(--awe-menu-flyout-width\);/);
  });

  it('separates the logout from the options of the user menu with a line, and shows it as the danger option', () => {
    HOME_SIDEBAR.forEach(file => {
      expect(fs.readFileSync(file, 'utf8')).toMatch(/<tag type="div" style="dropdown-separator"\/>\s*<include target-screen="info-buttons" target-source="logout"\/>/);
    });
    [path.join(REPOSITORY, 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'application', 'awe-react',
      'screen', 'home', 'info-buttons.xml'),
    path.join(REPOSITORY, 'awe-tests', 'awe-boot-react', 'src', 'main', 'resources', 'application', 'app', 'screen',
      'info-buttons.xml')].forEach(file => {
      expect(fs.readFileSync(file, 'utf8')).toMatch(/<info-button id="ButLogOut"[^>]*style="col-xs-12 dropdown-option-danger"/);
    });
  });

  it('centres the magnifier of the menu option search on its field: the icon does not sit on the text baseline', () => {
    const search = withoutComments(read('components/AweMenuSearch.css'));
    expect(rule(search, '.awe-menu-search-toggle i > .awe-icon')).toMatch(/display: block;/);
  });

  it('separates the header icons from the user menu with a vertical line', () => {
    const css = withoutComments(shell);
    const line = rule(css, '.layout-topbar .avatar-component:not(:first-child)::before');
    expect(line).toMatch(/width: 1px;/);
    expect(line).toMatch(/background: var\(--awe-color-border\);/);
  });

  it('shows the name of the user emphasised and the profile muted below it, as the prototype', () => {
    const css = withoutComments(shell);
    expect(rule(css, '.layout-topbar .avatar-component .avatar-text')).toMatch(/flex-direction: column;/);
    expect(rule(css, '.layout-topbar .avatar-component .avatar-name')).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    const description = rule(css, '.layout-topbar .avatar-component .avatar-description');
    expect(description).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(description).toMatch(/color: var\(--awe-color-text-muted\);/);
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
