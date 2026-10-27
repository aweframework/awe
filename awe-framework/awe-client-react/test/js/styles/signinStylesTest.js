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
  path.join(SCREENS, 'awe-react', 'screen', 'signin-local.xml')
];
const EXPIRED_PASSWORD_SCREEN = path.join(SCREENS, 'awe-react', 'screen', 'security', 'change-expired-password.xml');
const CHANGE_PASSWORD_SCREENS = [
  path.join(SCREENS, 'awe', 'screen', 'security', 'change-password.xml'),
  path.join(SCREENS, 'awe', 'screen', 'security', 'ChdPwdPub.xml')
];
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
    CHANGE_PASSWORD_SCREENS.forEach(file => {
      const xml = read(file);
      ['id="page-change-password-bg"', 'change-password-container', 'change-password-header', 'change-password-form',
        'change-password-text', 'change-password-with', 'form-actions'].forEach(hook => expect(xml).toContain(hook));
    });
  });

  it('draws every login page with the rules of the sign-in page: the page class and the classes of its XML (S19)', () => {
    const css = withoutComments(signin);
    // SigninPage gives the class to every login page and the classes of the sign-in XML to their column, brand and form
    expect(css).not.toMatch(/#page-signin\b|\.totp-screen|\.totp-container|\.totp-header|\.change-password-container/);
    expect(css).not.toMatch(/\.totp-form \.field|\.totp-form \.p-button|\.totp-form > \.field/);
    // The image panel of the shared XML is the panel of the page (::after): its own element is not drawn
    ['.signin-page > #page-change-password-bg', '.signin-page > #page-totp-bg'].forEach(selector =>
      expect(rule(signin, selector)).toMatch(/display: none;/));
    // Their titles in the display type, the expired password sentence muted
    const title = rule(signin, '.signin-form > .totp-text');
    expect(rule(signin, '.signin-form > .change-password-text')).toBe(title);
    // Their actions as the sign-in actions, the back buttons neutral outlined as the SSO button
    const buttons = rule(signin, '.signin-form > .p-2:last-child');
    ['.signin-form > .change-password-with > .form-actions', '.signin-form > .totp-with > .form-actions']
      .forEach(selector => expect(rule(signin, selector)).toBe(buttons));
    [['', ''], [':enabled:hover', ''], [':enabled:active', ''], [':focus', ''], [':focus-visible', '']]
      .forEach(([state]) => expect(rule(signin, `.signin-form .p-button.signin-secondary${state}`))
        .toBe(rule(signin, `.signin-form .p-button.signin-sso${state}`)));
    expect(rule(signin, '.signin-form .p-button.bg-primary')).toMatch(/background-color: var\(--awe-color-primary\) !important;/);
    // The slogan of the shared brand stays for assistive technology, as the name of the client
    expect(rule(signin, '.signin-info > .slogan')).toBe(rule(signin, '.signin-info > h1'));
  });

  it('lays the page out as the prototype: the form column next to the image panel', () => {
    const page = rule(signin, '.signin-page');
    expect(page).toMatch(/display: grid;/);
    expect(page).toMatch(/grid-template-columns: minmax\(var\(--awe-login-main-min\), 5fr\) 7fr;/);
    expect(page).toMatch(/min-height: 100dvh;/);
    expect(page).toMatch(/background: var\(--awe-login-background\);/);
  });

  it('paints the image panel as the prototype: the product image over the AWE brand panel, under the scrim', () => {
    const panel = rule(signin, '.signin-page::after');
    expect(panel).toMatch(/grid-column: 2;/);
    const layers = panel.match(/background:([^;]*);/)[1];
    const order = ['var(--awe-login-scrim)', 'var(--awe-login-image)', 'awe-logo-light-text.svg',
      'var(--awe-login-brand-background)'].map(layer => layers.indexOf(layer));
    order.forEach(position => expect(position).toBeGreaterThan(-1));
    expect([...order].sort((a, b) => a - b)).toEqual(order);
    expect(layers).toMatch(/var\(--awe-login-brand-logo-width\)/);
    // The panel logo is the unmodified AWE artwork of the prototype (white text, for the dark panel)
    expect(read('assets/images/logos/awe-logo-light-text.svg')).toBe(read(PROTOTYPE_LOGO));
  });

  it('draws the form column with the login paddings and the form width of the density', () => {
    const column = rule(signin, '.signin-page > .signin-container');
    expect(column).toMatch(/padding: var\(--awe-login-padding-y\) var\(--awe-login-padding-x\) var\(--awe-space-6\) !important;/);
    expect(column).toMatch(/grid-template-columns: minmax\(0, var\(--awe-login-form-width\)\);/);
    expect(column).toMatch(/background: var\(--awe-login-background\);/);
    expect(column).toMatch(/box-shadow: none !important;/);
  });

  it('shows the AWE logo at the top of the form column, its wordmark in the emphasis text colour', () => {
    const brand = rule(signin, '.signin-container > .signin-info');
    expect(brand).toMatch(/height: var\(--awe-login-logo-height\);/);
    expect(brand).toMatch(/awe-logo-mark\.svg/);
    const wordmark = rule(signin, '.signin-container > .signin-info::after');
    expect(wordmark).toMatch(/background-color: var\(--awe-color-text-emphasis\);/);
    expect(wordmark).toMatch(/mask: url\("[^"]*awe-logo-wordmark\.svg"\)/);
    // The name of the client stays for assistive technology
    expect(rule(signin, '.signin-info > h1')).toMatch(/clip-path: inset\(50%\);/);
  });

  it('writes the step title and its text in the display type of the prototype', () => {
    const title = rule(signin, '.signin-form > .totp-text');
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
    expect(read('criteria/AweCriterion.less')).toContain('.field .p-inputtext:not(textarea):not(.p-dropdown-label),');
    const button = rule(signin, '.signin-form .p-button');
    expect(button).toMatch(/width: 100%;/);
    expect(button).toMatch(/height: var\(--awe-size-control-lg\);/);
    expect(button).toMatch(/margin: 0 !important;/);
    expect(rule(signin, '.signin-form .p-button:not(.p-button-outlined)')).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    // The theme gives the label of a button its own weight (medium): the label of every button of the login pages takes
    // the semibold weight of its button, as the buttons of AngularJS (S18, S20)
    expect(rule(signin, '.signin-form .p-button > .p-button-label')).toMatch(/font-weight: inherit;/);
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

  it('draws the "Sign in with Microsoft" button in the application style with the --awe-login-microsoft-* tokens (S15)', () => {
    // The old artwork image is gone
    expect(withoutComments(signin)).not.toMatch(/azure_signin_light\.svg|sign-in-azure-btn-light/);
    const button = rule(signin, '.signin-form .p-button.signin-microsoft');
    // The logo and the text centred together
    expect(button).toMatch(/justify-content: center;/);
    expect(button).not.toMatch(/justify-content: flex-start;/);
    expect(button).toMatch(/align-items: center;/);
    expect(button).toMatch(/gap: var\(--awe-login-microsoft-padding\);/);
    expect(button).toMatch(/width: 100%;/);
    expect(button).toMatch(/height: var\(--awe-login-microsoft-height\);/);
    expect(button).toMatch(/padding: 0 var\(--awe-login-microsoft-padding\);/);
    expect(button).toMatch(/font-family: var\(--awe-login-microsoft-font-family\);/);
    expect(button).toMatch(/font-size: var\(--awe-login-microsoft-font-size\);/);
    expect(button).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(button).toMatch(/color: var\(--awe-login-microsoft-text\);/);
    expect(button).toMatch(/background: var\(--awe-login-microsoft-background\);/);
    expect(button).toMatch(/border: 1px solid var\(--awe-login-microsoft-border\);/);
    expect(button).toMatch(/border-radius: var\(--awe-login-microsoft-radius\);/);
    expect(button).not.toMatch(/border-radius: 0;/);
    // Hover and active as the outlined buttons of the login (the highlight of the theme), the colours of the tokens
    const hover = rule(signin, '.signin-form .p-button.signin-microsoft:enabled:hover');
    expect(hover).toMatch(/color: var\(--awe-login-microsoft-text\);/);
    expect(hover).toMatch(/background: var\(--awe-color-highlight\);/);
    expect(hover).toMatch(/border-color: var\(--awe-login-microsoft-border\);/);
    const active = rule(signin, '.signin-form .p-button.signin-microsoft:enabled:active');
    expect(active).toMatch(/color: var\(--awe-login-microsoft-text\);/);
    expect(active).toMatch(/background: var\(--awe-color-highlight-focus\);/);
    expect(active).toMatch(/border-color: var\(--awe-login-microsoft-border\);/);
    const focus = rule(signin, '.signin-form .p-button.signin-microsoft:focus-visible');
    expect(focus).toMatch(/outline: var\(--awe-focus-ring\);/);
    expect(focus).toMatch(/outline-offset: var\(--awe-focus-ring-offset\);/);
    // The official 21 x 21 logo, unaltered (the same asset in both engines), drawn at its own size
    expect(rule(signin, '.signin-microsoft-logo::before'))
      .toMatch(/content: url\("\.\.\/\.\.\/images\/logos\/microsoft-logo\.svg"\);/);
    const logo = read('assets/images/logos/microsoft-logo.svg');
    expect(logo).toMatch(/width="21" height="21"/);
    ['#f25022', '#00a4ef', '#7fba00', '#ffb900'].forEach(colour => expect(logo).toContain(`fill="${colour}"`));
    expect(logo).toBe(read(path.join(REPOSITORY, 'awe-framework', 'awe-client-angular', 'src', 'main', 'resources',
      'less', 'awe', 'images', 'logo', 'microsoft-logo.svg')));
  });

  it('draws the generic SSO button neutral outlined exactly as the Microsoft button, not in the primary colour (S18)', () => {
    [['', ''], [':enabled:hover', ''], [':enabled:active', ''], [':focus', ''], [':focus-visible', '']]
      .forEach(([state]) => {
        const microsoft = rule(signin, `.signin-form .p-button.signin-microsoft${state}`);
        expect(microsoft).not.toBeNull();
        expect(rule(signin, `.signin-form .p-button.signin-sso${state}`)).toBe(microsoft);
      });
    const button = rule(signin, '.signin-form .p-button.signin-sso');
    expect(button).not.toMatch(/--awe-color-primary/);
    expect(button).toMatch(/border: 1px solid var\(--awe-login-microsoft-border\);/);
    expect(button).toMatch(/color: var\(--awe-login-microsoft-text\);/);
    expect(rule(signin, '.signin-sso > .p-button-label')).toMatch(/font-weight: inherit;/);
  });

  it('removes the placeholders of the user and password fields, the labels stay (S18)', () => {
    const xml = read(SIGNIN_SCREENS[0]);
    ['cod_usr', 'pwd_usr'].forEach(id => {
      const field = xml.match(new RegExp(`<criteria [^>]*id="${id}"[^>]*>`))[0];
      expect(field).not.toMatch(/placeholder=/);
      expect(field).toMatch(/label="SCREEN_TEXT_(USER|PASS)"/);
    });
    // The Login button has no icon (as the AngularJS engine)
    expect(xml.match(/<button [^>]*id="ButLogIn"[^>]*>/)[0]).not.toMatch(/icon=/);
  });

  it('draws the inline login error as the prototype danger message, with the message tokens (S17)', () => {
    const alert = rule(signin, '.signin-form > .signin-alert');
    expect(alert).toMatch(/display: flex;/);
    expect(alert).toMatch(/align-items: flex-start;/);
    expect(alert).toMatch(/gap: var\(--awe-space-2\);/);
    expect(alert).toMatch(/padding: var\(--awe-space-2\) var\(--awe-space-3\);/);
    expect(alert).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    expect(alert).toMatch(/color: var\(--awe-color-text\);/);
    expect(alert).toMatch(/border-radius: var\(--awe-radius-content\);/);
    const danger = rule(signin, '.signin-alert--danger');
    expect(danger).toMatch(/background: var\(--awe-color-danger-subtle\);/);
    expect(danger).toMatch(/border: 1px solid var\(--awe-color-danger-border\);/);
    const accent = rule(signin, '.signin-alert--danger > .signin-alert-icon');
    expect(accent).toMatch(/color: var\(--awe-color-danger-text\);/);
    expect(rule(signin, '.signin-alert--danger strong')).toMatch(/color: var\(--awe-color-danger-text\);/);
    expect(rule(signin, '.signin-alert strong')).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    // Under the subtitle, then the fields
    expect(rule(signin, '.signin-form > .signin-alert + *')).toMatch(/margin-top: var\(--awe-space-5\);/);
  });

  it('centres the icon and the text of the sign-in buttons together, not the text alone (S15, S16)', () => {
    expect(rule(signin, '.signin-form .p-button')).toMatch(/justify-content: center;/);
    // The label of a PrimeReact button grows to the free width, which leaves the icon at the start of the button
    const label = rule(signin, '.signin-form .p-button > .p-button-label');
    expect(label).toMatch(/flex: none;/);
    // The text of the Microsoft button keeps the semibold weight of the button
    expect(rule(signin, '.signin-microsoft > .p-button-label')).toMatch(/font-weight: inherit;/);
  });

  it('puts the key icon of the SSO button right next to its label, at the gap of the Microsoft button (T16)', () => {
    // The gap between the icon and the label is the gap of the button, as the logo of the Microsoft button
    expect(rule(signin, '.signin-form .p-button.signin-sso')).toMatch(/gap: var\(--awe-login-microsoft-padding\);/);
    // The theme adds a margin after the left icon of a button (.p-button .p-button-icon-left), which widens that gap
    const icon = rule(signin, '.signin-form .p-button.signin-sso > .p-button-icon');
    expect(icon).toMatch(/margin: 0;/);
    expect(icon).toMatch(/flex: none;/);
  });

  it('shows the Login button loading as the prototype: a spinner, opaque, with the progress cursor (S14)', () => {
    const loading = rule(signin, '.signin-form .p-button.is-loading:disabled');
    expect(loading).toMatch(/cursor: progress;/);
    expect(loading).toMatch(/opacity: 1;/);
  });

  it('draws the "or continue with" divider as the prototype: lines on both sides, small muted capitals (S16)', () => {
    const divider = rule(signin, '.signin-divider');
    expect(divider).toMatch(/display: flex;/);
    expect(divider).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(divider).toMatch(/letter-spacing: var\(--awe-letter-spacing-caps\);/);
    expect(divider).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(divider).toMatch(/text-transform: uppercase;/);
    const line = rule(signin, '.signin-divider::before');
    expect(line).toMatch(/flex: 1;/);
    expect(line).toMatch(/height: 1px;/);
    expect(line).toMatch(/background: var\(--awe-color-border\);/);
    expect(rule(signin, '.signin-divider::after')).toBe(line);
  });

  it('puts the footer of the sign-in page under the form column, the image panel and its caption beside both', () => {
    expect(rule(signin, '.signin-page')).toBeTruthy();
    expect(signin).toMatch(/\.signin-page \{\s*grid-template-rows: minmax\(0, 1fr\) auto;\s*\}/);
    expect(signin).toMatch(/\.signin-page::after \{\s*grid-row: 1 \/ -1;\s*\}/);
    const footer = rule(signin, '.signin-page > .signin-footer');
    expect(footer).toMatch(/grid-row: 2;/);
    expect(footer).toMatch(/grid-column: 1;/);
    expect(footer).toMatch(/justify-content: space-between;/);
    expect(footer).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(footer).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(footer).toMatch(/border-top: 1px solid var\(--awe-color-border\);/);
    expect(rule(signin, '.signin-powered > .signin-powered-mark')).toMatch(/awe-logo-mark\.svg/);
    const caption = rule(signin, '.signin-page > .signin-caption');
    expect(caption).toMatch(/grid-column: 2;/);
    expect(caption).toMatch(/align-self: end;/);
    expect(caption).toMatch(/z-index: 1;/);
    expect(caption).toMatch(/color: var\(--awe-login-caption-text\);/);
    expect(rule(signin, '.signin-caption > .signin-caption-title')).toMatch(/font-size: var\(--awe-font-size-2xl\);/);
    expect(rule(signin, '.signin-caption > .signin-caption-text')).toMatch(/color: var\(--awe-login-caption-muted\);/);
  });

  it('writes the sign-in title in the display type and the subtitle muted under it', () => {
    const title = rule(signin, '.signin-form > .signin-title');
    expect(title).toMatch(/font-size: var\(--awe-font-size-display\);/);
    expect(title).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    expect(rule(signin, '.signin-form > .signin-subtitle')).toMatch(/color: var\(--awe-color-text-muted\);/);
  });

  it('shows the logo of the product alone, instead of the AWE logo', () => {
    expect(rule(signin, '.signin-container > .signin-info--logo')).toMatch(/background: none;/);
    expect(rule(signin, '.signin-container > .signin-info--logo::after')).toMatch(/content: none;/);
    expect(rule(signin, '.signin-info--logo > .signin-logo')).toMatch(/height: 100%;/);
  });

  it('centres the form alone, without the image panel, in the form-only layout', () => {
    const layout = '.signin-page[data-awe-login-layout="form-only"]';
    expect(rule(signin, layout)).toMatch(/grid-template-columns: minmax\(0, 1fr\);/);
    expect(rule(signin, `${layout}::after`)).toMatch(/display: none;/);
  });

  it('puts the logo at the top left of the page and the form centred in the free space in the form-only layout (T16)', () => {
    const layout = '.signin-page[data-awe-login-layout="form-only"]';
    // The column takes the whole page, inside the paddings of the page (the paddings of the split form column)
    const column = rule(signin, `${layout} > .signin-container`);
    expect(column).toMatch(/grid-template-columns: minmax\(0, 1fr\);/);
    expect(column).not.toMatch(/padding|margin|justify-content/);
    expect(rule(signin, '.signin-page > .signin-container'))
      .toMatch(/padding: var\(--awe-login-padding-y\) var\(--awe-login-padding-x\) var\(--awe-space-6\) !important;/);
    expect(rule(media(signin, '(max-width: 960px)'), '.signin-page > .signin-container'))
      .toMatch(/padding: var\(--awe-space-6\) !important;/);
    // The logo at the start of the first row: the top left of the page
    const logo = rule(signin, '.signin-container > .signin-info');
    expect(logo).toMatch(/justify-self: start;/);
    expect(rule(signin, `${layout} > .signin-container > .signin-info`)).toBeNull();
    // The form at the form width, centred in the row of the free height
    const form = rule(signin, `${layout} > .signin-container > .signin-form`);
    expect(form).toMatch(/justify-self: center;/);
    expect(form).toMatch(/width: min\(var\(--awe-login-form-width\), 100%\);/);
    expect(rule(signin, '.signin-container > .signin-form')).toMatch(/align-self: center;/);
  });

  it('spans the footer of the form-only layout, with its line, over the whole page inside the page paddings (T16)', () => {
    const layout = '.signin-page[data-awe-login-layout="form-only"]';
    // No rule of the layout narrows the footer to the form width
    expect(rule(signin, `${layout} > .signin-footer`)).toBeNull();
    expect(rule(media(signin, '(max-width: 960px)'), `${layout} > .signin-footer`)).toBeNull();
    // The footer of the only column of the page: the paddings of the page, the line on top
    const footer = rule(signin, '.signin-page > .signin-footer');
    expect(footer).toMatch(/grid-column: 1;/);
    expect(footer).toMatch(/margin: 0 var\(--awe-login-padding-x\);/);
    expect(footer).toMatch(/border-top: 1px solid var\(--awe-color-border\);/);
    expect(footer).toMatch(/justify-content: space-between;/);
    expect(footer).not.toMatch(/justify-self|(^|\s)width:/);
    expect(rule(media(signin, '(max-width: 960px)'), '.signin-page > .signin-footer'))
      .toMatch(/margin: 0 var\(--awe-space-6\);/);
  });

  it('keeps the split and card layouts as they were (T16)', () => {
    // Split: the form column at the form width next to the image panel, the footer under the column
    expect(withoutComments(signin)).not.toMatch(/data-awe-login-layout="split"/);
    expect(rule(signin, '.signin-page > .signin-container'))
      .toMatch(/grid-template-columns: minmax\(0, var\(--awe-login-form-width\)\);/);
    expect(rule(signin, '.signin-container > .signin-form')).not.toMatch(/justify-self|(^|\s)width:/);
    // Card: the form in the card, the footer on the card surface under it
    const card = '.signin-page[data-awe-login-layout="card"]';
    expect(rule(signin, `${card} > .signin-container`)).toMatch(/grid-template-columns: minmax\(0, 1fr\);/);
    expect(rule(signin, `${card} > .signin-container > .signin-form`)).toMatch(/padding: var\(--awe-space-8\) 0 0;/);
    expect(rule(signin, `${card} > .signin-container > .signin-form`)).not.toMatch(/justify-self|(^|\s)width:/);
    const footer = rule(signin, `${card} > .signin-footer`);
    expect(footer).toMatch(/grid-row: auto;/);
    expect(footer).toMatch(/padding: var\(--awe-space-3\) var\(--awe-space-4\);/);
    expect(footer).toMatch(/margin: 0;/);
    expect(footer).toMatch(/border-top: 0;/);
    // The rules of the form-only layout reach the form-only page only
    const formOnly = withoutComments(signin).match(/[^{}]*form-only[^{}]*\{/g);
    formOnly.forEach(selectors => selectors.replace(/\{$/, '').split(',')
      .forEach(selector => expect(selector.trim()).toMatch(/^\.signin-page\[data-awe-login-layout="form-only"\]/)));
  });

  it('draws the form in a card over the full-screen image, under the veil, in the card layout', () => {
    const layout = '.signin-page[data-awe-login-layout="card"]';
    expect(rule(signin, layout)).toMatch(/grid-template-columns: minmax\(0, var\(--awe-login-card-width\)\);/);
    const backdrop = rule(signin, `${layout}::after`);
    expect(backdrop).toMatch(/position: absolute;/);
    expect(backdrop).toMatch(/inset: 0;/);
    const layers = backdrop.match(/background:([^;]*);/)[1];
    const order = ['var(--awe-login-backdrop-veil)', 'var(--awe-login-scrim)', 'var(--awe-login-image)',
      'var(--awe-login-brand-background)'].map(layer => layers.indexOf(layer));
    order.forEach(position => expect(position).toBeGreaterThan(-1));
    expect([...order].sort((a, b) => a - b)).toEqual(order);
    const card = rule(signin, `${layout} > .signin-container`);
    ['padding: var(--awe-login-card-padding) !important;', 'background: var(--awe-login-card-background);',
      'border-radius: var(--awe-login-card-radius);', 'box-shadow: var(--awe-login-card-shadow) !important;']
      .forEach(declaration => expect(card).toContain(declaration));
    expect(rule(signin, `${layout} > .signin-footer`)).toMatch(/background: var\(--awe-login-card-background\);/);
    // Below the breakpoint the card keeps its backdrop
    expect(rule(media(signin, '(max-width: 960px)'), `${layout}::after`)).toMatch(/display: block;/);
  });

  it('places the caption of the card layout at the bottom left of the page, out of the grid of the card', () => {
    const caption = rule(signin, '.signin-page[data-awe-login-layout="card"] > .signin-caption');
    expect(caption).toMatch(/position: absolute;/);
    expect(caption).toMatch(/bottom: 0;/);
    expect(caption).toMatch(/left: 0;/);
    // An absolute grid item takes its grid area as containing block: without these it sits beside the card
    expect(caption).toMatch(/grid-row: auto;/);
    expect(caption).toMatch(/grid-column: auto;/);
    // Below the breakpoint the caption is hidden in every layout, the card one included
    expect(rule(media(signin, '(max-width: 960px)'), '.signin-page > .signin-caption')).toMatch(/display: none;/);
  });

  it('shows the whole AWE mark of "Powered by AWE 5" at the footer icon size', () => {
    const mark = rule(signin, '.signin-powered > .signin-powered-mark');
    expect(mark).toMatch(/awe-logo-mark\.svg/);
    expect(mark).toMatch(/height: var\(--awe-space-3\);/);
    // The mark is the left 560.3 of the 1160.2 x 360 frame of the AWE logo: a square box clips it
    expect(mark).toMatch(/aspect-ratio: 560\.3 \/ 360;/);
    expect(mark).not.toMatch(/(^|[^-])width: var\(/);
    expect(read('assets/images/logos/awe-logo-mark.svg')).toMatch(/viewBox="0 0 1160\.2 360"/);
  });

  it('draws the password fields of the expired password screen as the AngularJS one: label, lock, no placeholder (S19)', () => {
    const xml = read(EXPIRED_PASSWORD_SCREEN);
    [['Pas', 'PARAMETER_PASSWD'], ['RetPas', 'PARAMETER_RETYPE_PASSWD']].forEach(([id, label]) => {
      const field = xml.match(new RegExp(`<criteria [^>]*id="${id}"[^>]*>`))[0];
      expect(field).not.toMatch(/placeholder=/);
      expect(field).toContain(`label="${label}"`);
      expect(field).toMatch(/icon="lock"/);
      expect(field).toMatch(/component="password"/);
    });
  });

  it('gives the user and password fields of the sign-in screen their leading icon (S12)', () => {
    const xml = read(SIGNIN_SCREENS[0]);
    expect(xml).toMatch(/<criteria [^>]*icon="user"[^>]*id="cod_usr"/);
    expect(xml).toMatch(/<criteria [^>]*icon="lock"[^>]*id="pwd_usr"/);
  });

  it('draws the fields as the prototype: the icon inside the field, the eye button at its end (S12)', () => {
    const icon = rule(signin, '.signin-form .field > .p-inputgroup > .p-inputgroup-addon:first-child');
    expect(icon).toMatch(/position: absolute;/);
    expect(icon).toMatch(/color: var\(--awe-color-field-icon\);/);
    expect(icon).toMatch(/background: none;/);
    expect(icon).toMatch(/pointer-events: none;/);
    // Over the focused field (PrimeReact raises a focused field of an input group to z-index 1)
    expect(icon).toMatch(/z-index: 2;/);
    expect(rule(signin, '.signin-form .field > .p-inputgroup > .p-inputgroup-addon:first-child + .p-inputtext'))
      .toMatch(/padding-inline-start: /);
    const toggle = rule(signin, '.signin-password-toggle');
    expect(toggle).toMatch(/position: absolute;/);
    expect(toggle).toMatch(/color: var\(--awe-color-field-icon\);/);
    expect(rule(signin, '.signin-password-toggle:focus-visible')).toMatch(/outline: var\(--awe-focus-ring\);/);
    const caps = rule(signin, '.signin-caps');
    expect(caps).toMatch(/color: var\(--awe-color-warning-text\);/);
    expect(caps).toMatch(/font-size: var\(--awe-font-size-xs\);/);
  });

  it('draws the selectors of the footer as the prototype: a small select and a segmented control (S11)', () => {
    const legal = rule(signin, '.signin-footer > .signin-legal');
    expect(legal).toMatch(/flex-direction: column;/);
    expect(rule(signin, '.signin-footer > .signin-prefs')).toMatch(/gap: var\(--awe-space-2\);/);
    expect(rule(signin, '.signin-prefs > .signin-language')).toMatch(/height: var\(--awe-size-control-sm\);/);
    const mode = rule(signin, '.signin-mode');
    expect(mode).toMatch(/background: var\(--awe-segmented-track\);/);
    expect(mode).toMatch(/border: 1px solid var\(--awe-segmented-border\);/);
    const checked = rule(signin, '.signin-mode input:checked + span');
    expect(checked).toMatch(/color: var\(--awe-segmented-selected-text\);/);
    expect(checked).toMatch(/background: var\(--awe-segmented-selected\);/);
    expect(rule(signin, '.signin-mode input:focus-visible + span')).toMatch(/outline: var\(--awe-focus-ring\);/);
  });

  describe('canvas effect of the image panel (S23)', () => {
    const effect = '.signin-page > .signin-effect';
    const card = '.signin-page[data-awe-login-layout="card"] > .signin-effect';

    it('replaces the image panel only while the effect runs, so an unknown name keeps the image', () => {
      expect(rule(signin, '.signin-page[data-awe-login-effect-running]::after')).toMatch(/content: none;/);
      expect(rule(signin, '.signin-page[data-awe-login-effect]::after')).toBeNull();
    });

    it('places the effect as the image panel of the split layout, its canvas filling it, with no colour of its own', () => {
      const panel = rule(signin, effect);
      expect(panel).toMatch(/position: relative;/);
      expect(panel).toMatch(/grid-row: 1 \/ -1;/);
      expect(panel).toMatch(/grid-column: 2;/);
      expect(panel).toMatch(/overflow: hidden;/);
      // The effect draws its own colours: the framework paints no background behind the canvas
      expect(panel).not.toMatch(/background:/);
      const canvas = rule(signin, '.signin-effect > canvas');
      expect(canvas).toMatch(/position: absolute;/);
      expect(canvas).toMatch(/inset: 0;/);
      expect(canvas).toMatch(/width: 100%;/);
      expect(canvas).toMatch(/height: 100%;/);
    });

    it('keeps the scrim of the caption over the effect, and the caption above both', () => {
      const scrim = rule(signin, '.signin-effect::after');
      expect(scrim).toMatch(/position: absolute;/);
      expect(scrim).toMatch(/inset: 0;/);
      expect(scrim).toMatch(/background: var\(--awe-login-scrim\);/);
      expect(rule(signin, '.signin-page > .signin-caption')).toMatch(/z-index: 1;/);
    });

    it('fills the page under the card, under the veil and the scrim, in the card layout', () => {
      const backdrop = rule(signin, card);
      expect(backdrop).toMatch(/position: absolute;/);
      expect(backdrop).toMatch(/inset: 0;/);
      expect(backdrop).toMatch(/z-index: -1;/);
      expect(backdrop).toMatch(/grid-row: auto;/);
      expect(backdrop).toMatch(/grid-column: auto;/);
      const layers = rule(signin, `${card}::after`).match(/background:([^;]*);/)[1];
      const order = ['var(--awe-login-backdrop-veil)', 'var(--awe-login-scrim)'].map(layer => layers.indexOf(layer));
      order.forEach(position => expect(position).toBeGreaterThan(-1));
      expect(order[0]).toBeLessThan(order[1]);
    });

    it('is hidden where the panel is hidden: the form-only layout and narrow screens, except the card backdrop', () => {
      expect(rule(signin, '.signin-page[data-awe-login-layout="form-only"] > .signin-effect')).toMatch(/display: none;/);
      const narrow = media(signin, '(max-width: 960px)');
      expect(rule(narrow, effect)).toMatch(/display: none;/);
      expect(rule(narrow, card)).toMatch(/display: block;/);
    });
  });

  it('hides the image panel and centres the form on narrow screens', () => {
    const narrow = media(signin, '(max-width: 960px)');
    expect(narrow).not.toBeNull();
    expect(rule(narrow, '.signin-page')).toMatch(/grid-template-columns: minmax\(0, 1fr\);/);
    expect(rule(narrow, '.signin-page::after')).toMatch(/display: none;/);
    expect(rule(narrow, '.signin-page > .signin-container')).toMatch(/justify-content: center;/);
  });
});
