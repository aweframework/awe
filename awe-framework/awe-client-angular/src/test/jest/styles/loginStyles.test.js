import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR, ROOT} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The AWE 5 login pages of the AngularJS engine (#885), on par with the React login: the sign-in, change password,
// expired password and two-factor screens drawn as the prototype login (form column and image panel) with the
// --awe-login-* tokens, over the classes of their screen XML (the screen XML, the field ids and the test hooks do not
// change)
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

    it("shows the start-up background of the application on the panel when it configures one", () => {
      // styles.css is a Thymeleaf text template: the block only renders when the background is not the default one
      const less = fs.readFileSync(path.join(LESS_DIR, "awe/pages/signin.less"), "utf8");
      expect(less).toMatch(new RegExp(String.raw`/\*\[# th:unless="\$\{#strings\.endsWith\(startupBackground, `
        + String.raw`'background/signin-bg-1\.jpg'\)\}"\]\*/[^{]*\{\s*/\*\[\+background-image: var\(--awe-login-scrim\), `
        + String.raw`url\("\[\[\$\{startupBackground\}\]\]"\), none, var\(--awe-login-brand-background\);\+\]\*/`
        + String.raw`\s*\}\s*/\*\[/\]\*/`));
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

    it("shows the start-up logo of the application alone when it configures one", () => {
      const less = fs.readFileSync(path.join(LESS_DIR, "awe/pages/signin.less"), "utf8");
      expect(less).toMatch(new RegExp(String.raw`/\*\[# th:unless="\$\{#strings\.endsWith\(startupLogo, `
        + String.raw`'logo/logo-awe-nuevo\.svg'\)\}"\]\*/[^{]*\{\s*background-image: none;\s*`
        + String.raw`/\*\[\+background-image: url\("\[\[\$\{startupLogo\}\]\]"\);\+\]\*/\s*\}`
        + String.raw`[^{]*::after[^{]*\{\s*display: none;\s*\}\s*/\*\[/\]\*/`));
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
