import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR, ROOT} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The AWE 5 login pages of the AngularJS engine (#885), on par with the React login: the sign-in, change password,
// expired password and two-factor screens drawn as the prototype login (form column and image panel) with the
// --awe-login-* tokens, over the classes of their screen XML (the field ids and the test hooks do not change)
const LOGIN_FILES = ["pages/signin.less", "pages/change_password.less", "pages/2fa.less", "button.less"];
const SCREENS = path.resolve(ROOT, "../awe-generic-screens/src/main/resources/application/awe/screen/security");
const REACT_LOGOS = path.resolve(ROOT, "../awe-client-react/src/assets/images/logos");

const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const NARROW = "@media (max-width: 960px)";
const FOCUS_RING = "0 0 0 var(--awe-focus-ring-offset) var(--awe-color-surface), "
  + "0 0 0 calc(var(--awe-focus-ring-offset) + var(--awe-focus-ring-width)) var(--awe-color-focus-ring)";

// The three layouts of the login screens: sign-in (signin.xml, signin-local.xml), change password
// (change-password.xml, change-expired-password.xml) and two-factor (activate-2fa.xml, check-2fa.xml)
const PAGES = [".fullScreen:has(> #page-signin-bg)", ".fullScreen:has(> #page-change-password-bg)",
  ".totp-screen.fullScreen"];
const PANELS = [".fullScreen > #page-signin-bg", ".fullScreen > #page-change-password-bg",
  ".totp-screen.fullScreen > #page-totp-bg"];
const COLUMNS = [".awe-signin.signin-container", ".awe-change-password.change-password-container",
  ".awe-totp.totp-container"];
const BRANDS = [".signin-container > .signin-info", ".change-password-container > .change-password-header",
  ".totp-container > .totp-header"];
const FORMS = [".signin-container > .signin-form", ".change-password-container > .change-password-form",
  ".totp-container > .totp-form"];
const TITLES = [".signin-form > .signin-text", ".change-password-form > .change-password-text",
  ".totp-form > .totp-text"];
const ACTIONS = [".signin-with > .form-actions", ".change-password-with > .form-actions", ".totp-with > .form-actions"];
const FORM_ROOTS = [".signin-form", ".change-password-form", ".totp-form"];

describe("AngularJS login pages (#885)", () => {
  let rules;

  beforeAll(async () => {
    rules = parseRules(await compileStyles()).map(rule => {
      const match = rule.selector.match(MEDIA);
      return {
        media: match ? match[1].trim() : "",
        selectors: splitSelectors(match ? match[2] : rule.selector),
        declarations: rule.declarations
      };
    });
  });

  // Declarations of a selector in the given media (none by default); a later rule overrides an earlier one
  const style = (selector, media = "") => rules
    .filter(rule => rule.media === media && rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  const selectors = () => rules.flatMap(rule => rule.selectors);

  describe("tokens only", () => {
    it.each(LOGIN_FILES)("%s has no colour literal, no px size and no mode or density selector", file => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
      expect(source).not.toMatch(/(?<![\w-])(white|black|whitesmoke)(?![\w-])/);
      expect(source).not.toMatch(/\.dark\b|data-awe-density/);
      // Only hairlines and the breakpoint (a media query cannot read a token) are written in px
      expect(source.replace(/@media[^{]*/g, "").match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
      expect(source).not.toMatch(/var\(--awe-palette-/);
    });

    it("drops the pixel-admin sign-in page and the old login backgrounds, bands and pills", () => {
      expect(selectors().filter(selector => /\.page-signin\b|\.page-change[-_]password\b|change_password-header/
        .test(selector))).toEqual([]);
      const login = rules.filter(rule => rule.selectors.some(selector =>
        /signin|change-password|totp|qr-code/.test(selector)));
      login.forEach(rule => Object.values(rule.declarations).forEach(value => {
        expect(value).not.toMatch(/#[0-9a-fA-F]{3,8}\b|\brgba?\(/);
        expect(value).not.toMatch(/signin-bg-\d\.jpg|logo-awe-nuevo/);
      }));
    });
  });

  describe("page", () => {
    it.each(PAGES)("%s lays out the form column next to the image panel", page => {
      expect(style(page)).toMatchObject({
        "display": "grid",
        "grid-template-columns": "minmax(var(--awe-login-main-min), 5fr) 7fr",
        "grid-template-rows": "1fr auto",
        "overflow-y": "auto",
        "background": "var(--awe-login-background)"
      });
    });

    it.each(PANELS)("%s is the image panel over the AWE brand panel", panel => {
      expect(style(panel)).toMatchObject({
        "position": "relative",
        "grid-row": "1 / span 2",
        "grid-column": "2",
        "width": "auto",
        "height": "auto",
        "min-width": "0",
        "background": "var(--awe-login-scrim), var(--awe-login-image) center / cover no-repeat, "
          + "url('images/logo/awe-logo-light-text.svg') center 44% / min(var(--awe-login-brand-logo-width), 70%) "
          + "auto no-repeat, var(--awe-login-brand-background)"
      });
      expect(style(`${panel} > *`)).toMatchObject({"display": "none"});
    });

    it("keeps the content an application adds to the sign-in page in the form column", () => {
      expect(style(".fullScreen:has(> #page-signin-bg) > :not(#page-signin-bg):not(.signin-container)"))
        .toMatchObject({"grid-column": "1"});
    });

    it("leaves the start-up images to settings.login: no Thymeleaf block in the login styles (S19)", () => {
      // awe.application.paths.image-startup-* reach every login page as the deprecated fallbacks of
      // awe.application.login.logo / background, through settings.login and the login page directive
      LOGIN_FILES.forEach(file => expect(fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8"))
        .not.toMatch(/\[# th:|\[\[\$\{|startupLogo|startupBackground/));
    });

    it("publishes the AWE logo of the panel as the React login", () => {
      expect(fs.readFileSync(path.join(LESS_DIR, "awe/pages/images/logo/awe-logo-light-text.svg"), "utf8"))
        .toBe(fs.readFileSync(path.join(REACT_LOGOS, "awe-logo-light-text.svg"), "utf8"));
    });
  });

  describe("form column", () => {
    it.each(COLUMNS)("%s holds the brand on top and the form in the middle", column => {
      expect(style(column)).toMatchObject({
        "display": "grid",
        "grid-row": "1",
        "grid-column": "1",
        "grid-template-rows": "auto 1fr",
        "grid-template-columns": "minmax(0, var(--awe-login-form-width))",
        "align-self": "stretch",
        "min-width": "0",
        "padding": "var(--awe-login-padding-y) var(--awe-login-padding-x) var(--awe-space-6)",
        "margin": "0",
        "background": "var(--awe-login-background)",
        "border-radius": "0",
        "box-shadow": "none"
      });
    });

    it.each(FORMS)("%s sits in the middle of the free height, with no band", form => {
      expect(style(form)).toMatchObject({
        "align-self": "center",
        "min-width": "0",
        "padding": "var(--awe-space-10) 0",
        "background": "transparent"
      });
    });

    it.each(BRANDS)("%s draws the AWE logo: the mark and the wordmark in the emphasis text colour", brand => {
      expect(style(brand)).toMatchObject({
        "position": "relative",
        "justify-self": "start",
        "height": "var(--awe-login-logo-height)",
        "aspect-ratio": "1160.2 / 360",
        "padding": "0",
        "background": "url('images/logo/awe-logo-mark.svg') no-repeat left center / auto 100%"
      });
      expect(style(`${brand}::after`)).toMatchObject({
        "position": "absolute",
        "inset": "0",
        "content": "\"\"",
        "background-color": "var(--awe-color-text-emphasis)",
        "mask": "url('images/logo/awe-logo-wordmark.svg') no-repeat left center / auto 100%"
      });
    });

    it("keeps the client name and the slogan for assistive technology only", () => {
      [".signin-info > h1", ".signin-info > .slogan", ".change-password-header > h1",
        ".change-password-header > .slogan", ".totp-header > h1", ".totp-header > .slogan"].forEach(hidden =>
        expect(style(hidden)).toMatchObject({
          "position": "absolute",
          "width": "1px",
          "height": "1px",
          "overflow": "hidden",
          "clip-path": "inset(50%)"
        }));
    });

  });

  describe("form", () => {
    it.each(TITLES)("%s is the display title of the step", title => {
      expect(style(title)).toMatchObject({
        "margin": "0 0 var(--awe-space-6)",
        "font-size": "var(--awe-font-size-display)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "font-style": "normal",
        "line-height": "var(--awe-line-height-tight)",
        "letter-spacing": "var(--awe-letter-spacing-display)",
        "color": "var(--awe-color-text-emphasis)",
        "text-align": "start"
      });
      expect(style(`${title}::before`)).toMatchObject({"display": "none"});
      expect(style(`${title} > span`)).toMatchObject({"padding": "0", "background": "transparent"});
    });

    it("writes the sentence of the expired password screen as a muted text, as the React login", () => {
      expect(style(".page-change-expired-password .change-password-form > .change-password-text")).toMatchObject({
        "font-size": "var(--awe-font-size-md)",
        "font-weight": "var(--awe-font-weight-normal)",
        "line-height": "var(--awe-line-height-base)",
        "letter-spacing": "normal",
        "color": "var(--awe-color-text-muted)"
      });
    });

    it.each(FORM_ROOTS)("%s puts the large fields one under the other, the icon inside the field", form => {
      expect(style(`${form} > .criterion`)).toMatchObject({"width": "100%", "padding": "0 0 var(--awe-gap-form-row)"});
      expect(style(`${form} > .criterion .form-group`)).toMatchObject({"margin": "0"});
      expect(style(`${form} .form-group.w-icon .input-group-lg > input`))
        .toMatchObject({"padding-left": "var(--awe-size-control-lg) !important"});
    });

    it.each(ACTIONS)("%s stacks full width buttons, the main one first", actions => {
      expect(style(actions)).toMatchObject({
        "display": "flex",
        "flex-direction": "column",
        "gap": "var(--awe-space-3)",
        "margin": "var(--awe-space-6) 0 0",
        "padding": "0"
      });
    });

    it.each([".signin-with", ".change-password-with", ".totp-with"])("%s has no band", band => {
      expect(style(band)).toMatchObject({"margin": "0", "padding": "0", "background": "transparent", "border": "0"});
    });

    it.each(FORM_ROOTS)("%s draws large full width buttons", form => {
      expect(style(`${form} .form-actions > div`)).toMatchObject({"width": "100%"});
      expect(style(`${form} .form-actions .btn`)).toMatchObject({
        "display": "inline-flex",
        "align-items": "center",
        "justify-content": "center",
        "gap": "var(--awe-gap-inline)",
        "width": "100%",
        "height": "var(--awe-size-control-lg)",
        "margin": "0",
        "padding": "0 var(--awe-button-padding-x)",
        "font-size": "var(--awe-font-size-md)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "line-height": "1",
        "border-radius": "var(--awe-radius-field)"
      });
      expect(style(`${form} .form-actions .btn:focus-visible`)).toMatchObject({"box-shadow": FOCUS_RING});
      // The buttons of the screens show their icon only on small screens: the login buttons keep their text
      expect(style(`${form} .form-actions .btn .button-text`)).toMatchObject({"display": "inline"});
    });

    it.each([".change-password-form", ".totp-form"])("%s fills its primary button (no btn-awe) with the primary", form => {
      expect(style(`${form} .btn.btn-primary`)).toMatchObject({
        "color": "var(--awe-color-on-primary)",
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(`${form} .btn.btn-primary:hover`)).toMatchObject({
        "background": "var(--awe-color-primary-hover) !important"
      });
      expect(style(`${form} .btn.btn-primary:active`)).toMatchObject({
        "background": "var(--awe-color-primary-active) !important"
      });
    });

    it("draws the Azure sign-in artwork at the height of a large control", () => {
      expect(style(".sign-in-azure-btn-light")).toMatchObject({
        "height": "var(--awe-size-control-lg)",
        "background-size": "contain",
        "border": "0 solid"
      });
      expect(style(".signin-form .form-actions .sign-in-azure-btn-light")).toMatchObject({
        "display": "block",
        "width": "auto",
        "aspect-ratio": "217 / 45",
        "margin": "0 auto",
        "padding": "0",
        "background-color": "transparent"
      });
    });
  });

  // The login loading state, the Sign in with Microsoft button and the SSO block of the sign-in page, as the prototype
  describe("sign-in actions (S14-S16)", () => {
    const ACTIONS_OF = ".signin-form .form-actions";
    const MICROSOFT = `${ACTIONS_OF} .signin-microsoft`;
    const VISIBLE = ":not(.ng-hide):not(.hidden)";

    it("shows the spinner and Signing in… on the login button while it loads, over the button skeleton (S14)", () => {
      expect(style(`${ACTIONS_OF} .btn.is-loading`)).toMatchObject({"cursor": "progress", "opacity": "1"});
      expect(style(`${ACTIONS_OF} .btn.is-loading > :not(.signin-busy)`)).toMatchObject({"display": "none"});
      expect(style(".signin-busy")).toMatchObject({
        "display": "inline-flex",
        "align-items": "center",
        "gap": "var(--awe-gap-inline)"
      });
      // The engine draws a skeleton over a loading button: the login button shows its spinner instead
      expect(style(`${ACTIONS_OF} > :has(> #ButLogIn) > .button-loader`)).toMatchObject({"display": "none !important"});
      // The spinner is the fa-spin of the AWE icons: it stops when the user prefers reduced motion
      const icons = fs.readFileSync(path.resolve(ROOT, "../awe-design-tokens/icons/awe-icon.css"), "utf8");
      expect(icons).toMatch(/@media \(prefers-reduced-motion: reduce\) \{\s*\.awe-icon-spin \{\s*animation: none;/);
    });

    it("draws ButLogInAzure as Sign in with Microsoft in the application style with the Microsoft tokens (S15, L18)",
      () => {
        // The official logo and the text centred together, semibold, with the rounded corners of the application
        expect(style(MICROSOFT)).toMatchObject({
          "display": "flex",
          "align-items": "center",
          "justify-content": "center",
          "gap": "var(--awe-login-microsoft-padding)",
          "width": "100%",
          "height": "var(--awe-login-microsoft-height)",
          "margin": "0",
          "padding": "0 var(--awe-login-microsoft-padding)",
          "font-family": "var(--awe-login-microsoft-font-family)",
          "font-size": "var(--awe-login-microsoft-font-size)",
          "font-weight": "var(--awe-font-weight-semibold)",
          "line-height": "1",
          "color": "var(--awe-login-microsoft-text)",
          "background": "var(--awe-login-microsoft-background)",
          "border": "1px solid var(--awe-login-microsoft-border)",
          "border-radius": "var(--awe-login-microsoft-radius)",
          "box-shadow": "none"
        });
        // The official logo, unaltered, in its 21 x 21 frame: 1.4 times the text of the button
        expect(style(`${MICROSOFT}::before`)).toMatchObject({
          "flex": "none",
          "width": "1.4em",
          "height": "1.4em",
          "content": "\"\"",
          "background": "url('../images/logo/microsoft-logo.svg') no-repeat center / contain"
        });
        expect(fs.existsSync(path.join(LESS_DIR, "awe/images/logo/microsoft-logo.svg"))).toBe(true);
        // Hover and active as the outlined buttons of the login (btn-awe): a subtle surface and the hover border, the
        // text keeps its colour; the focus ring is visible
        const hover = style(`${MICROSOFT}:hover`);
        expect(hover).toMatchObject({
          "background": "var(--awe-color-surface-subtle)",
          "border-color": "var(--awe-color-field-border-hover)"
        });
        expect(hover).not.toHaveProperty("color");
        const active = style(`${MICROSOFT}:active`);
        expect(active).toMatchObject({
          "background": "var(--awe-color-surface-hover)",
          "border-color": "var(--awe-color-field-border-hover)",
          "box-shadow": "none"
        });
        expect(active).not.toHaveProperty("color");
        // Disabled (while the login runs) it keeps its resting surface and border
        expect(style(`${MICROSOFT}[disabled]`)).toMatchObject({
          "cursor": "not-allowed",
          "background": "var(--awe-login-microsoft-background)",
          "border-color": "var(--awe-login-microsoft-border)"
        });
        expect(style(`${MICROSOFT}:focus-visible`)).toMatchObject({
          "outline": "var(--awe-focus-ring)",
          "outline-offset": "var(--awe-focus-ring-offset)"
        });
        expect(style(`${MICROSOFT} .button-text`)).toMatchObject({"display": "inline"});
      });

    it("centres the key icon of ButLogInSSO together with its text (S16, L17)", () => {
      // The SSO button is an outlined login button: its icon and its text are the items of a centred flex row
      expect(style(`${ACTIONS_OF} .btn`)).toMatchObject({"display": "inline-flex", "justify-content": "center"});
      const markup = fs.readFileSync(path.join(SCREENS, "signin.xml"), "utf8");
      expect(markup).toMatch(/<button id="ButLogInSSO" style="[^"]*\bbtn\b[^"]*"[^>]*icon="lucide:key-round"/);
      // No rule pulls the icon to the edge or stretches the text over the button
      const PULLS = ["position", "float", "flex", "flex-grow", "width", "margin-left", "margin-right", "margin",
        "text-align", "justify-content"];
      rules.filter(rule => rule.media === "")
        .filter(rule => rule.selectors.some(selector => /\.(button-icon|button-text)\b/.test(selector)
          && /(\.form-actions|#ButLogInSSO|\.sign-in-sso-btn|\.signin-btn)\b/.test(selector)))
        .forEach(rule => PULLS.forEach(property => expect(rule.declarations).not.toHaveProperty(property)));
    });

    it("shows the or continue with divider only when ButLogIn and an SSO button are visible (S16)", () => {
      expect(style(`${ACTIONS_OF} > .signin-divider`)).toMatchObject({
        "display": "none",
        "align-items": "center",
        "gap": "var(--awe-space-3)",
        "margin": "var(--awe-space-3) 0",
        "font-size": "var(--awe-font-size-xs)",
        "font-weight": "var(--awe-font-weight-medium)",
        "letter-spacing": "var(--awe-letter-spacing-caps)",
        "color": "var(--awe-color-text-muted)",
        "text-transform": "uppercase"
      });
      // LESS writes a space after the parenthesis of a :has() that holds a :not(), none in the others
      // The divider follows the wrapper of ButLogIn: shown while that wrapper is visible and an SSO button is too; with
      // the SSO buttons only (local login hidden) there is nothing to divide
      const LOGIN_SHOWN = `${VISIBLE}:has(> #ButLogIn) + .signin-divider`;
      ["ButLogInAzure", "ButLogInSSO"].forEach(id =>
        expect(style(`${ACTIONS_OF}:has( > ${VISIBLE} > #${id}) > ${LOGIN_SHOWN}`)).toMatchObject({"display": "flex"}));
      ["ButLogInAzure", "ButLogInSSO"].forEach(id =>
        expect(style(`${ACTIONS_OF}:has( > ${VISIBLE} > #${id}) > .signin-divider`)).not.toHaveProperty("display"));
      expect(style(`${ACTIONS_OF} > .signin-divider::before`)).toMatchObject({
        "flex": "1",
        "height": "1px",
        "content": "\"\"",
        "background": "var(--awe-color-border)"
      });
      expect(style(`${ACTIONS_OF} > .signin-divider::after`)).toMatchObject(style(`${ACTIONS_OF} > .signin-divider::before`));
    });
  });

  // The failed login alert and the SSO button of the sign-in page, as the prototype and the React login
  describe("failed login and engines unified (S17, S18)", () => {
    const SSO = ".signin-form .form-actions .btn.sign-in-sso-btn";

    it("draws the failed login as the danger message of the prototype, above the fields (S17)", () => {
      expect(style(".signin-form > .signin-alert")).toMatchObject({
        "display": "flex",
        "align-items": "flex-start",
        "gap": "var(--awe-space-2)",
        "margin": "0 0 var(--awe-space-5)",
        "padding": "var(--awe-space-2) var(--awe-space-3)",
        "font-size": "var(--awe-font-size-sm)",
        "line-height": "var(--awe-line-height-base)",
        "color": "var(--awe-color-text)",
        "background": "color-mix(in srgb, var(--awe-color-danger-subtle), var(--awe-color-surface) 45%)",
        "border": "1px solid var(--awe-color-danger-border)",
        "border-radius": "var(--awe-radius-content)"
      });
      expect(style(".signin-form > .signin-alert[hidden]")).toMatchObject({"display": "none"});
      expect(style(".signin-alert > .awe-icon")).toMatchObject({
        "flex": "none",
        "width": "var(--awe-size-icon)",
        "height": "var(--awe-size-icon)",
        "margin-top": "var(--awe-space-0-5)",
        "color": "var(--awe-color-danger-text)"
      });
      expect(style(".signin-alert strong")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-danger-text)"
      });
    });

    it("draws ButLogInSSO neutral outlined as the Microsoft button, not with the primary colour (S18)", () => {
      expect(style(SSO)).toMatchObject({
        "color": "var(--awe-login-microsoft-text)",
        "background": "var(--awe-login-microsoft-background)",
        "border": "1px solid var(--awe-login-microsoft-border)",
        "box-shadow": "none"
      });
      [`${SSO}:hover`, `${SSO}:focus`].forEach(selector => expect(style(selector)).toMatchObject({
        "color": "var(--awe-login-microsoft-text)",
        "background": "var(--awe-color-surface-subtle) !important",
        "border-color": "var(--awe-color-field-border-hover)"
      }));
      expect(style(`${SSO}:active`)).toMatchObject({
        "color": "var(--awe-login-microsoft-text)",
        "background": "var(--awe-color-surface-hover) !important",
        "border-color": "var(--awe-color-field-border-hover)",
        "box-shadow": "none"
      });
      // No shadow at rest, but the focus ring of the login buttons stays
      expect(style(`${SSO}:focus-visible`)).toMatchObject({"box-shadow": FOCUS_RING});
      rules.filter(rule => rule.selectors.some(selector => /sign-in-sso-btn|#ButLogInSSO/.test(selector)))
        .forEach(rule => expect(JSON.stringify(rule.declarations)).not.toMatch(/--awe-color-primary/));
    });

    it("draws the Login button text semibold, without an icon (S18)", () => {
      expect(style(".signin-form .form-actions .btn")).toMatchObject({"font-weight": "var(--awe-font-weight-semibold)"});
      const markup = fs.readFileSync(path.join(SCREENS, "signin.xml"), "utf8");
      expect(markup.match(/<button [^>]*id="ButLogIn"[^>]*>/)[0]).not.toMatch(/icon=/);
    });
  });

  describe("two-factor activation", () => {
    it("draws the help texts muted, the QR code on a card and the secret code in monospace", () => {
      expect(style(".totp-form .panel-padding")).toMatchObject({
        "margin": "var(--awe-space-4) 0 var(--awe-space-2)",
        "padding": "0",
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text-muted)"
      });
      expect(style(".totp-form .qr-code-container")).toMatchObject({"display": "flex", "justify-content": "center"});
      expect(style(".totp-form .qr-code")).toMatchObject({
        "width": "calc(var(--awe-space-16) * 3)",
        "max-width": "100%",
        "height": "auto",
        "padding": "var(--awe-space-2)",
        "background": "var(--awe-color-surface)",
        "border": "1px solid var(--awe-color-border)",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".totp-form .panel")).toMatchObject({
        "margin": "0", "background": "transparent", "border": "0", "box-shadow": "none"
      });
      expect(style(".totp-form .panel > .panel-body")).toMatchObject({
        "padding": "var(--awe-space-3)",
        "font-family": "var(--awe-font-family-monospace)",
        "font-size": "var(--awe-font-size-md)",
        "letter-spacing": "var(--awe-letter-spacing-caps)",
        "color": "var(--awe-color-text-emphasis)",
        "overflow-wrap": "anywhere",
        "background": "var(--awe-color-surface-subtle)",
        "border": "1px solid var(--awe-color-border)",
        "border-radius": "var(--awe-radius-md)"
      });
    });
  });

  // The sign-in page that reads settings.login (awe.application.login.*): the aweSignin directive sets the layout on
  // the page root and adds the logo, the subtitle, the caption and the footer
  describe("configurable sign-in page", () => {
    const LAYOUT = layout => `.fullScreen[data-awe-login-layout="${layout}"]`;
    const SIGNIN_PAGE = ".fullScreen:has(> #page-signin-bg)";
    const CARD_WIDTH = "min(var(--awe-login-card-width), calc(100% - 2 * var(--awe-space-6)))";

    it("adds the footer row under the form column (split, the default)", () => {
      expect(style(".fullScreen[data-awe-login-layout]:has(> #page-signin-bg)"))
        .toMatchObject({"grid-template-rows": "1fr auto auto"});
      expect(style(".fullScreen[data-awe-login-layout] > #page-signin-bg")).toMatchObject({"grid-row": "1 / -1"});
      expect(style(`${SIGNIN_PAGE} > .signin-footer`)).toMatchObject({
        "display": "flex",
        "flex-wrap": "wrap",
        "align-items": "center",
        "justify-content": "space-between",
        "gap": "var(--awe-space-3) var(--awe-space-4)",
        "grid-row": "3",
        "grid-column": "1",
        "min-width": "0",
        "margin": "0 var(--awe-login-padding-x) var(--awe-space-6)",
        "padding": "var(--awe-space-4) 0 0",
        "font-size": "var(--awe-font-size-xs)",
        "color": "var(--awe-color-text-muted)",
        "border-top": "1px solid var(--awe-color-border)"
      });
      expect(style(`${SIGNIN_PAGE} > .signin-footer`, NARROW))
        .toMatchObject({"margin": "0 var(--awe-space-6) var(--awe-space-6)"});
    });

    it("puts the footer text over Powered by AWE 5 (with the AWE mark) on the left, the selectors on the right", () => {
      expect(style(".signin-footer > .signin-legal")).toMatchObject({
        "display": "flex",
        "flex-direction": "column",
        "gap": "var(--awe-space-1)",
        "min-width": "0"
      });
      expect(style(".signin-footer > .signin-prefs")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "gap": "var(--awe-space-2)",
        "margin-inline-start": "auto"
      });
      expect(style(".signin-legal > .signin-powered")).toMatchObject({
        "display": "inline-flex",
        "align-items": "center",
        "gap": "var(--awe-space-1-5)"
      });
      expect(style(".signin-legal > .signin-powered::before")).toMatchObject({
        "content": "\"\"",
        "height": "1em",
        "aspect-ratio": "560.3 / 360",
        "background": "url('images/logo/awe-logo-mark.svg') no-repeat left center / auto 100%"
      });
      expect(style(".signin-powered > b")).toMatchObject({"font-weight": "var(--awe-font-weight-semibold)"});
    });

    it("draws the language selector as a small select field with its options in a listbox above it (S11)", () => {
      expect(style(".signin-prefs > .signin-language")).toMatchObject({"position": "relative"});
      expect(style(".signin-language > .signin-language-trigger")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "gap": "var(--awe-space-1-5)",
        "height": "var(--awe-size-control-sm)",
        "padding": "0 var(--awe-space-2)",
        "font-size": "var(--awe-font-size-xs)",
        "color": "var(--awe-color-field-text)",
        "cursor": "pointer",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)"
      });
      expect(style(".signin-language > .signin-language-trigger:hover"))
        .toMatchObject({"border-color": "var(--awe-color-field-border-hover)"});
      expect(style(".signin-language > .signin-language-trigger:focus-visible")).toMatchObject({
        "outline": "var(--awe-focus-ring)",
        "outline-offset": "var(--awe-focus-ring-offset)"
      });
      expect(style(".signin-language.is-open > .signin-language-trigger > svg:last-child"))
        .toMatchObject({"rotate": "180deg"});
      expect(style(".signin-language > .signin-language-list")).toMatchObject({
        "position": "absolute",
        "bottom": "calc(100% + var(--awe-space-1))",
        "inset-inline-end": "0",
        "z-index": "var(--awe-z-dropdown)",
        "min-width": "100%",
        "padding": "var(--awe-list-padding)",
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-overlay)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".signin-language > .signin-language-list[hidden]")).toMatchObject({"display": "none"});
      expect(style(".signin-language-list > [role=\"option\"]")).toMatchObject({
        "padding": "var(--awe-list-option-padding)",
        "color": "var(--awe-color-list-option-text)",
        "border-radius": "var(--awe-radius-list-option)"
      });
      expect(style(".signin-language-list > .is-active")).toMatchObject({
        "color": "var(--awe-color-list-option-text-focus)",
        "background": "var(--awe-color-list-option-focus)"
      });
      expect(style(".signin-language-list > [aria-selected=\"true\"]")).toMatchObject({
        "color": "var(--awe-color-list-option-text-selected)",
        "background": "var(--awe-color-list-option-selected)"
      });
    });

    it("draws the colour mode selector as a small segmented control (S11)", () => {
      expect(style(".signin-prefs > .signin-mode")).toMatchObject({
        "display": "inline-flex",
        "gap": "var(--awe-space-0-5)",
        "padding": "var(--awe-space-0-5)",
        "background": "var(--awe-segmented-track)",
        "border": "1px solid var(--awe-segmented-border)",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".signin-mode input")).toMatchObject({"position": "absolute", "inset": "0", "opacity": "0"});
      expect(style(".signin-mode .signin-mode-segment")).toMatchObject({
        "height": "calc(var(--awe-size-control-sm) - 2 * (var(--awe-space-0-5) + 1px))",
        "padding": "0 var(--awe-space-2)",
        "color": "var(--awe-segmented-text)",
        "border": "1px solid transparent",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".signin-mode label:hover > .signin-mode-segment"))
        .toMatchObject({"color": "var(--awe-segmented-hover-text)"});
      expect(style(".signin-mode input:checked + .signin-mode-segment")).toMatchObject({
        "color": "var(--awe-segmented-selected-text)",
        "background": "var(--awe-segmented-selected)",
        "border-color": "var(--awe-segmented-selected-border)",
        "box-shadow": "var(--awe-shadow-sm)"
      });
      expect(style(".signin-mode input:focus-visible + .signin-mode-segment")).toMatchObject({
        "outline": "var(--awe-focus-ring)",
        "outline-offset": "calc(-1 * var(--awe-focus-ring-width))"
      });
    });

    it("puts the eye button inside the password field and the Caps Lock warning under it (S12)", () => {
      expect(style(".signin-form .input:has(> .signin-password-toggle) > input"))
        .toMatchObject({"padding-right": "var(--awe-size-control-lg) !important"});
      expect(style(".signin-form .input:has(> .signin-password-toggle) > .form-control-feedback"))
        .toMatchObject({"display": "none"});
      expect(style(".signin-form .signin-password-toggle")).toMatchObject({
        "position": "absolute",
        "top": "50%",
        "inset-inline-end": "var(--awe-space-2)",
        "display": "grid",
        "place-items": "center",
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "translate": "0 -50%",
        "color": "var(--awe-color-field-icon)",
        "background": "none",
        "border": "0",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".signin-form .signin-password-toggle:hover")).toMatchObject({
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-surface-hover)"
      });
      expect(style(".signin-form .signin-password-toggle:focus-visible")).toMatchObject({
        "outline": "var(--awe-focus-ring)",
        "outline-offset": "0"
      });
      expect(style(".signin-form .signin-caps")).toMatchObject({
        "display": "flex",
        "gap": "var(--awe-space-1)",
        "font-size": "var(--awe-font-size-xs)",
        "font-weight": "var(--awe-font-weight-medium)",
        "color": "var(--awe-color-warning-text)"
      });
      expect(style(".signin-form .signin-caps[hidden]")).toMatchObject({"display": "none"});
    });

    it("shows the configured logo alone, at the height of the AWE logo", () => {
      expect(style(".signin-container > .signin-info:has(> .signin-logo)"))
        .toMatchObject({"aspect-ratio": "auto", "background-image": "none"});
      expect(style(".signin-container > .signin-info:has(> .signin-logo)::after")).toMatchObject({"display": "none"});
      expect(style(".signin-info > .signin-logo")).toMatchObject({
        "display": "block",
        "width": "auto",
        "max-width": "100%",
        "height": "100%",
        "object-fit": "contain"
      });
    });

    it("writes the subtitle muted under the title", () => {
      expect(style(".signin-form > .signin-text:has(+ .signin-subtitle)"))
        .toMatchObject({"margin-bottom": "var(--awe-space-2)"});
      expect(style(".signin-form > .signin-subtitle")).toMatchObject({
        "margin": "0 0 var(--awe-space-6)",
        "font-size": "var(--awe-font-size-md)",
        "line-height": "var(--awe-line-height-base)",
        "color": "var(--awe-color-text-muted)"
      });
    });

    it("draws the caption at the bottom left of the panel, over the scrim", () => {
      expect(style(".fullScreen > #page-signin-bg > .signin-caption")).toMatchObject({
        "position": "absolute",
        "inset": "auto 0 0",
        "display": "block",
        "padding": "var(--awe-space-10)",
        "color": "var(--awe-login-caption-text)"
      });
      expect(style(".signin-caption > .signin-caption-title")).toMatchObject({
        "margin": "0",
        "font-size": "var(--awe-font-size-2xl)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "line-height": "var(--awe-line-height-tight)",
        "letter-spacing": "var(--awe-letter-spacing-display)"
      });
      expect(style(".signin-caption > .signin-caption-text")).toMatchObject({
        "max-width": "46ch",
        "margin": "0",
        "font-size": "var(--awe-font-size-md)",
        "color": "var(--awe-login-caption-muted)"
      });
      expect(style(".signin-caption-title + .signin-caption-text")).toMatchObject({"margin-top": "var(--awe-space-2)"});
    });

    it("form-only centres the form alone, without the panel", () => {
      expect(style(`${LAYOUT("form-only")}:has(> #page-signin-bg)`))
        .toMatchObject({"grid-template-columns": "minmax(0, 1fr)"});
      expect(style(`${LAYOUT("form-only")} > #page-signin-bg`)).toMatchObject({"display": "none"});
    });

    it("form-only puts the logo at the top left of the page and the form centred in the free space (T16)", () => {
      // The column spans the page with the page padding, so the logo (justify-self: start) sits at its top left
      const column = style(`${LAYOUT("form-only")} > .awe-signin.signin-container`);
      expect(column).toMatchObject({"grid-template-columns": "minmax(0, 1fr)"});
      expect(column).not.toHaveProperty("justify-content");
      expect(column).not.toHaveProperty("padding");
      expect(style(".signin-container > .signin-info")).toMatchObject({"justify-self": "start"});
      expect(style(".awe-signin.signin-container"))
        .toMatchObject({"padding": "var(--awe-login-padding-y) var(--awe-login-padding-x) var(--awe-space-6)"});
      expect(style(".awe-signin.signin-container", NARROW)).toMatchObject({"padding": "var(--awe-space-6)"});
      // The form at the form width, centred horizontally and in the middle of the free height
      expect(style(`${LAYOUT("form-only")} > .signin-container > .signin-form`)).toMatchObject({
        "justify-self": "center",
        "width": "min(var(--awe-login-form-width), 100%)"
      });
      expect(style(".signin-container > .signin-form")).toMatchObject({"align-self": "center"});
    });

    it("form-only spans the footer and its separator over the whole page, with the page padding (T16)", () => {
      // No form-only rule narrows the footer: it takes the margins of the split footer
      const footer = `${LAYOUT("form-only")}:has(> #page-signin-bg) > .signin-footer`;
      expect(selectors()).not.toContain(footer);
      expect(style(`${SIGNIN_PAGE} > .signin-footer`)).toMatchObject({
        "grid-column": "1",
        "margin": "0 var(--awe-login-padding-x) var(--awe-space-6)",
        "border-top": "1px solid var(--awe-color-border)"
      });
      expect(style(`${SIGNIN_PAGE} > .signin-footer`)).not.toHaveProperty("width");
      expect(style(`${SIGNIN_PAGE} > .signin-footer`, NARROW))
        .toMatchObject({"margin": "0 var(--awe-space-6) var(--awe-space-6)"});
    });

    it("split and card keep their form column, form and footer (T16)", () => {
      expect(style(".awe-signin.signin-container"))
        .toMatchObject({"grid-template-columns": "minmax(0, var(--awe-login-form-width))"});
      expect(style(".signin-container > .signin-form")).not.toHaveProperty("justify-self");
      expect(style(".signin-container > .signin-form")).not.toHaveProperty("width");
      ["split", "card"].forEach(layout => {
        expect(style(`${LAYOUT(layout)} > .signin-container > .signin-form`)).not.toHaveProperty("justify-self");
        expect(style(`${LAYOUT(layout)} > .signin-container > .signin-form`)).not.toHaveProperty("width");
      });
      expect(selectors()).not.toContain(`${LAYOUT("split")} > .awe-signin.signin-container`);
      expect(style(`${LAYOUT("card")} > .awe-signin.signin-container`))
        .toMatchObject({"grid-template-columns": "minmax(0, 1fr)", "justify-self": "center", "width": CARD_WIDTH});
      expect(style(`${LAYOUT("card")}:has(> #page-signin-bg) > .signin-footer`))
        .toMatchObject({"justify-self": "center", "width": CARD_WIDTH, "margin": "0"});
    });

    it("card draws the form in a card over the full-screen background, under the veil", () => {
      expect(style(`${LAYOUT("card")}:has(> #page-signin-bg)`)).toMatchObject({
        "grid-template-columns": "minmax(0, 1fr)",
        "grid-template-rows": "none",
        "grid-auto-rows": "auto",
        "align-content": "safe center",
        "gap": "var(--awe-space-4)",
        "padding": "var(--awe-space-6) 0"
      });
      expect(style(`${LAYOUT("card")} > #page-signin-bg`)).toMatchObject({
        "display": "block",
        "background": "var(--awe-login-scrim), linear-gradient(var(--awe-login-backdrop-veil), "
          + "var(--awe-login-backdrop-veil)), var(--awe-login-image) center / cover no-repeat, "
          + "var(--awe-login-brand-background)"
      });
      expect(style(`${LAYOUT("card")} > .awe-signin.signin-container`)).toMatchObject({
        "position": "relative",
        "grid-row": "1",
        "grid-template-columns": "minmax(0, 1fr)",
        "justify-self": "center",
        "align-self": "center",
        "width": CARD_WIDTH,
        "padding": "var(--awe-login-card-padding)",
        "margin": "0",
        "background": "var(--awe-login-card-background)",
        "border-radius": "var(--awe-login-card-radius)",
        "box-shadow": "var(--awe-login-card-shadow)"
      });
      expect(style(`${LAYOUT("card")} > .signin-container > .signin-form`)).toMatchObject({"padding": "var(--awe-space-8) 0 0"});
    });

    it("card puts the footer under the card on the card surface, with the footer text colours (AA over any image)",
      () => {
        const footer = style(`${LAYOUT("card")}:has(> #page-signin-bg) > .signin-footer`);
        expect(footer).toMatchObject({
          "position": "relative",
          "grid-row": "2",
          "justify-self": "center",
          "width": CARD_WIDTH,
          "margin": "0",
          "padding": "var(--awe-space-3) var(--awe-space-4)",
          "background": "var(--awe-login-card-background)",
          "border-top": "0",
          "border-radius": "var(--awe-login-card-radius)"
        });
        // The muted text colour of the footer of the other layouts, never the caption colours of the image
        expect(footer).not.toHaveProperty("color");
        expect(style(`${SIGNIN_PAGE} > .signin-footer`)).toMatchObject({"color": "var(--awe-color-text-muted)"});
      });

    it("card fixes the backdrop to the viewport: it covers the whole page, also the content an application adds and "
      + "while the page scrolls", () => {
      // The page (.fullScreen) is the scroll container: a grid item or an absolute panel would only cover its rows or
      // its first screen
      expect(style(`${LAYOUT("card")} > #page-signin-bg`)).toMatchObject({"position": "fixed", "inset": "0"});
      expect(style(`${LAYOUT("card")} > #page-signin-bg`)).not.toHaveProperty("grid-row");
      // The caption stays at the bottom left of the panel, so of the viewport
      expect(style(".fullScreen > #page-signin-bg > .signin-caption")).toMatchObject({"position": "absolute",
        "inset": "auto 0 0"});
    });

    it("card stacks the content an application adds under the card footer, centred over the backdrop", () => {
      const extra = `${LAYOUT("card")}:has(> #page-signin-bg) > :not(#page-signin-bg):not(.signin-container)`;
      // Auto-placed in the column after the card (row 1) and its footer (row 2)
      expect(style(extra)).toMatchObject({"position": "relative", "justify-self": "center", "max-width": CARD_WIDTH});
      expect(style(extra)).not.toHaveProperty("grid-row");
      expect(style(".fullScreen:has(> #page-signin-bg) > :not(#page-signin-bg):not(.signin-container)"))
        .toMatchObject({"grid-column": "1"});
    });

    it("card keeps the backdrop below 960px and drops the caption there", () => {
      // The card panel rule (display: block) outranks the narrow rule that hides the panel of the split layout
      expect(style(`${LAYOUT("card")} > #page-signin-bg`, NARROW)).not.toHaveProperty("display");
      expect(style(`${LAYOUT("card")} > #page-signin-bg > .signin-caption`, NARROW)).toMatchObject({"display": "none"});
    });
  });

  // The canvas effect of the image panel (awe.application.login.background-effect, S23): the directive adds the canvas
  // the application registered in a .signin-effect wrapper inside the panel of each login page, while the effect runs
  describe("canvas effect of the image panel (S23)", () => {
    const EFFECT_PANELS = [".fullScreen[data-awe-login-effect-running] > #page-signin-bg",
      ".fullScreen[data-awe-login-effect-running] > #page-change-password-bg",
      ".totp-screen.fullScreen[data-awe-login-effect-running] > #page-totp-bg"];
    const CARD_PANELS = [".fullScreen[data-awe-login-layout=\"card\"] > #page-signin-bg",
      ".fullScreen[data-awe-login-layout=\"card\"] > #page-change-password-bg",
      ".totp-screen.fullScreen[data-awe-login-layout=\"card\"] > #page-totp-bg"];

    it.each(EFFECT_PANELS)("%s draws the page background instead of the image and the AWE logo while the effect runs",
      panel => {
        expect(style(panel)).toMatchObject({"background": "var(--awe-login-background)", "overflow": "hidden"});
      });

    it("leaves the image panel untouched while no effect runs, so an unknown name keeps the image", () => {
      expect(style(".fullScreen[data-awe-login-effect] > #page-signin-bg")).toEqual({});
    });

    it.each(PANELS)("%s shows the effect over the whole panel, not hidden as the old overlays", panel => {
      expect(style(`${panel} > .signin-effect`)).toMatchObject({"position": "absolute", "inset": "0", "display": "block"});
    });

    it("fills the effect with its canvas and keeps the scrim of the caption over it, the caption above both", () => {
      expect(style(".signin-effect > canvas")).toMatchObject({"display": "block", "width": "100%", "height": "100%"});
      expect(style(".signin-effect::after")).toMatchObject({
        "position": "absolute",
        "inset": "0",
        "content": "\"\"",
        "background": "var(--awe-login-scrim)"
      });
      EFFECT_PANELS.forEach(panel => expect(style(`${panel} > .signin-caption`)).toMatchObject({"z-index": "1"}));
    });

    it.each(CARD_PANELS)("%s puts the veil and the scrim over the effect in the card layout", panel => {
      expect(style(`${panel} > .signin-effect::after`)).toMatchObject({
        "background": "var(--awe-login-scrim), linear-gradient(var(--awe-login-backdrop-veil), "
          + "var(--awe-login-backdrop-veil))"
      });
    });

    it("comes after the card backdrop, so the page background replaces the image there too", () => {
      const index = selector => rules.findIndex(rule => rule.media === "" && rule.selectors.includes(selector));
      EFFECT_PANELS.forEach((panel, position) => expect(index(panel)).toBeGreaterThan(index(CARD_PANELS[position])));
    });
  });

  // The change password and two-factor screens take the frame, the fields and the buttons of the configurable sign-in
  // page: the same declarations as the sign-in rules, over the classes of their own screen XML
  describe("password and two-factor screens (S19)", () => {
    const signin = {
      page: layout => `.fullScreen${layout}:has(> #page-signin-bg)`,
      panel: layout => `.fullScreen${layout} > #page-signin-bg`,
      column: layout => `.fullScreen${layout} > .awe-signin.signin-container`,
      form: layout => `.fullScreen${layout} > .signin-container > .signin-form`,
      brand: ".signin-container > .signin-info", header: ".signin-info", formRoot: ".signin-form"
    };
    const OTHERS = [["change password", {
      page: layout => `.fullScreen${layout}:has(> #page-change-password-bg)`,
      panel: layout => `.fullScreen${layout} > #page-change-password-bg`,
      column: layout => `.fullScreen${layout} > .awe-change-password.change-password-container`,
      form: layout => `.fullScreen${layout} > .change-password-container > .change-password-form`,
      brand: ".change-password-container > .change-password-header", header: ".change-password-header",
      formRoot: ".change-password-form"
    }], ["two-factor", {
      page: layout => `.totp-screen.fullScreen${layout}`,
      panel: layout => `.totp-screen.fullScreen${layout} > #page-totp-bg`,
      column: layout => `.totp-screen.fullScreen${layout} > .awe-totp.totp-container`,
      form: layout => `.totp-screen.fullScreen${layout} > .totp-container > .totp-form`,
      brand: ".totp-container > .totp-header", header: ".totp-header", formRoot: ".totp-form"
    }]];
    const LAYOUTS = ["", "[data-awe-login-layout]", "[data-awe-login-layout=\"form-only\"]",
      "[data-awe-login-layout=\"card\"]"];

    // The rule of the other screen has the declarations of the sign-in rule (and the sign-in rule has some)
    const same = (other, sign, media = "") => {
      expect(Object.keys(style(sign, media)).length).toBeGreaterThan(0);
      expect({[other]: style(other, media)}).toEqual({[other]: style(sign, media)});
    };

    it.each(OTHERS)("the %s screens take the layouts of settings.login: split, form-only and card", (_name, other) => {
      LAYOUTS.slice(1).forEach(layout => ["page", "panel"].forEach(part => same(other[part](layout), signin[part](layout))));
      LAYOUTS.slice(2).forEach(layout => ["column", "form"].forEach(part => same(other[part](layout), signin[part](layout))));
    });

    it.each(OTHERS)("the %s screens show the configured logo alone", (_name, other) => {
      same(`${other.brand}:has(> .signin-logo)`, `${signin.brand}:has(> .signin-logo)`);
      same(`${other.brand}:has(> .signin-logo)::after`, `${signin.brand}:has(> .signin-logo)::after`);
      same(`${other.header} > .signin-logo`, `${signin.header} > .signin-logo`);
    });

    it.each(OTHERS)("the %s screens show the caption of the panel and the footer", (_name, other) => {
      same(`${other.panel("")} > .signin-caption`, `${signin.panel("")} > .signin-caption`);
      same(`${other.page("")} > .signin-footer`, `${signin.page("")} > .signin-footer`);
      same(`${other.page("")} > .signin-footer`, `${signin.page("")} > .signin-footer`, NARROW);
      const card = "[data-awe-login-layout=\"card\"]";
      same(`${other.page(card)} > .signin-footer`, `${signin.page(card)} > .signin-footer`);
      same(`${other.panel(card)} > .signin-caption`, `${signin.panel(card)} > .signin-caption`, NARROW);
      // The card backdrop stays on narrow screens: no narrow rule hides it
      expect(style(other.panel(card), NARROW)).not.toHaveProperty("display");
    });

    it("the change password screens put the eye in their password fields and the Caps Lock warning under them", () => {
      [".input:has(> .signin-password-toggle) > input", ".input:has(> .signin-password-toggle) > .form-control-feedback",
        ".signin-password-toggle", ".signin-password-toggle:hover", ".signin-password-toggle:focus-visible",
        ".signin-caps", ".signin-caps[hidden]"].forEach(part =>
        same(`.change-password-form ${part}`, `.signin-form ${part}`));
    });

    it.each(OTHERS)("the %s screens show the loading state on their submit button, over the button skeleton",
      (_name, other) => {
        same(`${other.formRoot} .form-actions .btn.is-loading`, ".signin-form .form-actions .btn.is-loading");
        same(`${other.formRoot} .form-actions .btn.is-loading > :not(.signin-busy)`,
          ".signin-form .form-actions .btn.is-loading > :not(.signin-busy)");
        expect(style(`${other.formRoot} .form-actions > :has(> [type="submit"]) > .button-loader`))
          .toMatchObject({"display": "none !important"});
      });

    it("draws the back buttons neutral outlined, as the SSO button of the sign-in page", () => {
      ["", ":hover", ":focus", ":active", ":focus-visible"].forEach(state =>
        same(`.change-password-form .form-actions .btn.btn-default${state}`,
          `.signin-form .form-actions .btn.sign-in-sso-btn${state}`));
    });
  });

  describe("narrow screens", () => {
    it("shows the form alone and centred below 960px", () => {
      PAGES.forEach(page => expect(style(page, NARROW)).toMatchObject({"grid-template-columns": "minmax(0, 1fr)"}));
      PANELS.forEach(panel => expect(style(panel, NARROW)).toMatchObject({"display": "none"}));
      COLUMNS.forEach(column => expect(style(column, NARROW)).toMatchObject({
        "justify-content": "center",
        "padding": "var(--awe-space-6)"
      }));
    });
  });

  describe("markup", () => {
    const read = file => fs.readFileSync(path.join(SCREENS, file), "utf8");

    it("styles the classes and keeps the test hooks of the login screens", () => {
      ["signin.xml", "signin-local.xml"].forEach(file => ["id=\"page-signin-bg\"", "awe-signin signin-container",
        "\"signin-info\"", "\"signin-form\"", "\"signin-text\"", "\"signin-with\"", "id=\"cod_usr\"", "id=\"pwd_usr\"",
        "id=\"ButLogIn\""].forEach(hook => expect(read(file)).toContain(hook)));
      [["change-password.xml", ["id=\"Usr\"", "id=\"OldPas\"", "id=\"ButChdPwd\""]],
        ["ChdPwdPub.xml", ["id=\"Usr\"", "id=\"OldPas\"", "id=\"ButCnf\"", "id=\"ButBck\""]],
        ["change-expired-password.xml", ["id=\"ButChdExpPwd\"", "id=\"ButBckExpPwd\""]]].forEach(([file, ids]) =>
        ["id=\"page-change-password-bg\"", "awe-change-password change-password-container",
          "\"change-password-header\"", "\"change-password-form\"", "\"change-password-text\"",
          "\"change-password-with\"", "id=\"Pas\"", "id=\"RetPas\"", ...ids]
          .forEach(hook => expect(read(file)).toContain(hook)));
      ["activate-2fa.xml", "check-2fa.xml"].forEach(file => ["totp-screen fullScreen", "id=\"page-totp-bg\"",
        "awe-totp totp-container", "\"totp-header\"", "\"totp-form\"", "\"totp-text\"", "\"totp-with\"",
        "id=\"ButChk2fa\""].forEach(hook => expect(read(file)).toContain(hook)));
      expect(read("check-2fa.xml")).toContain("id=\"code\"");
      expect(read("activate-2fa.xml")).toContain("id=\"qr-code\"");
    });
  });
});
