const fs = require('fs');
const path = require('path');

// The AWE 5 login page of the React engine (#885): the sign-in, expired password and two-factor screens drawn as the
// prototype login (form column and image panel) with the --awe-login-* tokens, over the classes of their screen XML
// (the screen XML, the field ids and the test hooks do not change)
const REPOSITORY = path.join(__dirname, '../../../../..');
const SRC = path.join(REPOSITORY, 'awe-framework', 'awe-client-react', 'src');
const SCREENS = path.join(REPOSITORY, 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'application');
const SIGNIN_SCREENS = [
  path.join(SCREENS, 'awe-react', 'screen', 'signin.xml'),
  path.join(SCREENS, 'awe-react', 'screen', 'signin-local.xml'),
  path.join(REPOSITORY, 'awe-tests', 'awe-boot-react', 'src', 'main', 'resources', 'application', 'app', 'screen',
    'signin.xml')
];
const EXPIRED_PASSWORD_SCREEN = path.join(SCREENS, 'awe-react', 'screen', 'security', 'change-expired-password.xml');
const TWO_FACTOR_SCREENS = [
  path.join(SCREENS, 'awe', 'screen', 'security', 'activate-2fa.xml'),
  path.join(SCREENS, 'awe', 'screen', 'security', 'check-2fa.xml')
];
const PROTOTYPE_LOGO = path.join(REPOSITORY, 'website', 'static', 'design-system', 'prototype', 'assets', 'logos',
  'awe-logo-light-text.svg');

const read = file => fs.readFileSync(path.isAbsolute(file) ? file : path.join(SRC, file), 'utf8');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule whose selector list holds the selector
const rule = (css, selector) => {
  const match = withoutComments(css)
    .match(new RegExp(`(?:^|[},])\\s*(?:[^{}]*,\\s*)?${escape(selector)}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};
// The content of the first media query of the condition
const media = (css, condition) => {
  const source = withoutComments(css);
  const start = source.indexOf(`@media ${condition}`);
  if (start < 0) {
    return null;
  }
  let depth = 0;
  for (let index = source.indexOf('{', start); index < source.length; index++) {
    depth += source[index] === '{' ? 1 : source[index] === '}' ? -1 : 0;
    if (depth === 0) {
      return source.slice(source.indexOf('{', start) + 1, index);
    }
  }
  return null;
};

describe('AWE 5 login page styles (#885)', () => {
  const signin = read('assets/css/pages/signin.css');

  it('styles the login pages with tokens only: no colour literals, sizes or mode selectors', () => {
    const css = withoutComments(signin);
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|color-mix)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Only hairlines and the breakpoint (a media query cannot read a token) are written in px
    expect(css.replace(/@media[^{]*/g, '').match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em)\b/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--awe-/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
    // The old photo background is gone: the panel is the AWE brand panel, or the image of the product
    expect(css).not.toMatch(/background01\.jpg/);
  });

  it('styles the classes that the login screens give to the page', () => {
    SIGNIN_SCREENS.forEach(file => {
      const xml = read(file);
      ['id="page-signin"', 'signin-container', 'signin-info', 'signin-form', 'id="cod_usr"', 'id="pwd_usr"',
        'id="ButLogIn"'].forEach(hook => expect(xml).toContain(hook));
    });
    const expired = read(EXPIRED_PASSWORD_SCREEN);
    ['id="page-change-expired-password"', 'signin-container', 'signin-info', 'signin-form', 'id="Pas"', 'id="RetPas"',
      'id="ButChdExpPwd"', 'id="ButBckExpPwd"'].forEach(hook => expect(expired).toContain(hook));
    TWO_FACTOR_SCREENS.forEach(file => {
      const xml = read(file);
      ['totp-screen fullScreen', 'id="page-totp-bg"', 'totp-container', 'totp-header', 'totp-form', 'totp-text',
        'totp-btn', 'id="ButChk2fa"'].forEach(hook => expect(xml).toContain(hook));
    });
  });

  it('lays the page out as the prototype: the form column next to the image panel', () => {
    const page = rule(signin, '#page-signin');
    expect(page).toMatch(/display: grid;/);
    expect(page).toMatch(/grid-template-columns: minmax\(var\(--awe-login-main-min\), 5fr\) 7fr;/);
    expect(page).toMatch(/min-height: 100dvh;/);
    expect(page).toMatch(/background: var\(--awe-login-background\);/);
    ['#page-change-expired-password', '.totp-screen.fullScreen'].forEach(selector =>
      expect(rule(signin, selector)).toBe(page));
  });

  it('paints the image panel as the prototype: the product image over the AWE brand panel, under the scrim', () => {
    const panel = rule(signin, '#page-signin::after');
    expect(panel).toMatch(/grid-column: 2;/);
    const layers = panel.match(/background:([^;]*);/)[1];
    const order = ['var(--awe-login-scrim)', 'var(--awe-login-image)', 'awe-logo-light-text.svg',
      'var(--awe-login-brand-background)'].map(layer => layers.indexOf(layer));
    order.forEach(position => expect(position).toBeGreaterThan(-1));
    expect([...order].sort((a, b) => a - b)).toEqual(order);
    expect(layers).toMatch(/var\(--awe-login-brand-logo-width\)/);
    ['#page-change-expired-password::after', '.totp-screen.fullScreen > #page-totp-bg'].forEach(selector =>
      expect(rule(signin, selector)).toBe(panel));
    // The panel logo is the unmodified AWE artwork of the prototype (white text, for the dark panel)
    expect(read('assets/images/logos/awe-logo-light-text.svg')).toBe(read(PROTOTYPE_LOGO));
  });

  it('draws the form column with the login paddings and the form width of the density', () => {
    const column = rule(signin, '#page-signin > .signin-container');
    expect(column).toMatch(/padding: var\(--awe-login-padding-y\) var\(--awe-login-padding-x\) var\(--awe-space-6\) !important;/);
    expect(column).toMatch(/grid-template-columns: minmax\(0, var\(--awe-login-form-width\)\);/);
    expect(column).toMatch(/background: var\(--awe-login-background\);/);
    expect(column).toMatch(/box-shadow: none !important;/);
    ['#page-change-expired-password > .signin-container', '.totp-screen.fullScreen > .totp-container'].forEach(selector =>
      expect(rule(signin, selector)).toBe(column));
  });

  it('shows the AWE logo at the top of the form column, its wordmark in the emphasis text colour', () => {
    const brand = rule(signin, '.signin-container > .signin-info');
    expect(brand).toMatch(/height: var\(--awe-login-logo-height\);/);
    expect(brand).toMatch(/awe-logo-mark\.svg/);
    expect(rule(signin, '.totp-container > .totp-header')).toBe(brand);
    const wordmark = rule(signin, '.signin-container > .signin-info::after');
    expect(wordmark).toMatch(/background-color: var\(--awe-color-text-emphasis\);/);
    expect(wordmark).toMatch(/mask: url\("[^"]*awe-logo-wordmark\.svg"\)/);
    // The name of the client stays for assistive technology
    expect(rule(signin, '.signin-info > h1')).toMatch(/clip-path: inset\(50%\);/);
  });

  it('writes the step title and its text in the display type of the prototype', () => {
    const title = rule(signin, '.totp-form > .totp-text');
    expect(title).toMatch(/font-size: var\(--awe-font-size-display\);/);
    expect(title).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(title).toMatch(/letter-spacing: var\(--awe-letter-spacing-display\);/);
    expect(title).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    expect(rule(signin, '#page-change-expired-password .signin-form > .p-2:first-child')).toMatch(/color: var\(--awe-color-text-muted\);/);
  });

  it('draws large fields and full width buttons, the main one semibold', () => {
    // More specific than the field size of the criteria (AweCriterion.less), which it overrides
    const field = rule(signin, '.signin-form .field .p-inputtext:not(textarea):not(.p-dropdown-label)');
    expect(field).toMatch(/min-height: var\(--awe-size-control-lg\);/);
    expect(rule(signin, '.totp-form .field .p-inputtext:not(textarea):not(.p-dropdown-label)')).toBe(field);
    expect(read('criteria/AweCriterion.less')).toContain('.field .p-inputtext:not(textarea):not(.p-dropdown-label),');
    const button = rule(signin, '.signin-form .p-button');
    expect(button).toMatch(/width: 100%;/);
    expect(button).toMatch(/height: var\(--awe-size-control-lg\);/);
    expect(button).toMatch(/margin: 0 !important;/);
    expect(rule(signin, '.totp-form .p-button')).toBe(button);
    expect(rule(signin, '.signin-form .p-button:not(.p-button-outlined)')).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    // The buttons follow each other with the gap of the form rows
    expect(rule(signin, '.signin-form > .p-2:last-child')).toMatch(/gap: var\(--awe-space-3\);/);
  });

  it('shows the code of the two-factor activation in a monospace box and the QR code on a light card', () => {
    const code = rule(signin, '.totp-form .panel-body');
    expect(code).toMatch(/font-family: var\(--awe-font-family-monospace\);/);
    expect(code).toMatch(/background: var\(--awe-color-surface-subtle\);/);
    expect(code).toMatch(/border: 1px solid var\(--awe-color-border\);/);
    expect(rule(signin, '.totp-form .qr-code')).toMatch(/border: 1px solid var\(--awe-color-border\);/);
  });

  it('keeps the Azure sign-in button artwork with the height of a large control', () => {
    const azure = rule(signin, '.sign-in-azure-btn-light');
    expect(azure).toMatch(/azure_signin_light\.svg/);
    expect(azure).toMatch(/height: var\(--awe-size-control-lg\);/);
  });

  it('hides the image panel and centres the form on narrow screens', () => {
    const narrow = media(signin, '(max-width: 960px)');
    expect(narrow).not.toBeNull();
    expect(rule(narrow, '#page-signin')).toMatch(/grid-template-columns: minmax\(0, 1fr\);/);
    expect(rule(narrow, '#page-signin::after')).toMatch(/display: none;/);
    expect(rule(narrow, '.totp-screen.fullScreen > #page-totp-bg')).toMatch(/display: none;/);
    expect(rule(narrow, '#page-signin > .signin-container')).toMatch(/justify-content: center;/);
  });
});
