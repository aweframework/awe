import fs from "fs";
import path from "path";
import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// The configurable AWE 5 sign-in page: the sign-in screen reads settings.login (awe.application.login.*) and renders
// the layout, the logo, the background image, the texts and the footer over the markup of its screen XML. The change
// password and two-factor screens take the same frame, fields and buttons (S19)
const SIGNIN = `<div class="fullScreen">
  <div id="page-signin-bg" class="fullScreen"><div class="overlay-image fullScreen"></div><div class="overlay fullScreen"></div></div>
  <div class="awe-signin signin-container">
    <div class="signin-info"><h1><span><span translate>APP_CLIENT</span></span></h1><div class="slogan"><span translate>APP_NAME</span></div></div>
    <div class="signin-form">
      <div class="signin-text"><span><span translate>SCREEN_TITLE_LOGIN</span></span></div>
      <input id="cod_usr">
      <div class="criterion"><div class="form-group group-lg w-icon"><label for="pwd_usr" class="control-label">Password</label>
        <div class="validator input input-group-lg focus-target"><span class="form-icon"></span><input type="password" id="pwd_usr" aria-describedby="pwd_usr-help"><span class="form-control-feedback"></span></div>
      </div></div>
      <div class="signin-with"><div class="form-actions">
        <div><button type="submit" id="ButLogIn"><i class="button-icon"></i><span class="button-text">Login</span></button></div>
        <div><button type="button" id="ButLogInAzure"></button></div>
        <div><button type="button" id="ButLogInSSO"></button></div>
      </div></div>
    </div>
  </div>
</div>`;

/**
 * A large field of a login screen: the label above, the leading icon and the input
 * @param {string} id Input id
 * @param {string} type Input type
 * @returns {string} Markup
 */
const field = (id, type) => `<div class="criterion"><div class="form-group group-lg w-icon"><label for="${id}" class="control-label">${id}</label>
  <div class="validator input input-group-lg focus-target"><span class="form-icon"></span><input type="${type}" id="${id}"><span class="form-control-feedback"></span></div>
</div></div>`;

/**
 * The actions of a login screen: the submit button first, then the other buttons
 * @param {string} prefix Class prefix of the screen (change-password, totp)
 * @param {string} submit Id of the submit button
 * @param {string} [back] Id of the back button
 * @returns {string} Markup
 */
const actions = (prefix, submit, back) => `<div class="${prefix}-with"><div class="form-actions">
  <div><button type="submit" id="${submit}"><i class="button-icon"></i><span class="button-text">Confirm</span></button></div>
  ${back ? `<div><button type="button" id="${back}"><i class="button-icon"></i><span class="button-text">Back</span></button></div>` : ""}
</div></div>`;

// The change password screens (change-password.xml, ChdPwdPub.xml and change-expired-password.xml)
const passwordScreen = (title, fields, submit, back) => `<div class="fullScreen">
  <div id="page-change-password-bg" class="fullScreen"><div class="overlay-image fullScreen"></div><div class="overlay fullScreen"></div></div>
  <div class="awe-change-password change-password-container">
    <div class="change-password-header"><h1><span>APP_CLIENT</span></h1><div class="slogan">APP_NAME</div></div>
    <div class="change-password-form">
      <div class="change-password-text"><span>${title}</span></div>
      <input type="hidden" id="cod_usr">
      ${fields}
      ${actions("change-password", submit, back)}
    </div>
  </div>
</div>`;

// The two-factor screens (check-2fa.xml and activate-2fa.xml)
const totpScreen = (title, content, submit) => `<div class="totp-screen fullScreen">
  <div id="page-totp-bg" class="fullScreen"><div class="overlay-image fullScreen"></div><div class="overlay fullScreen"></div></div>
  <div class="awe-totp totp-container">
    <div class="totp-header"><h1><span>APP_CLIENT</span></h1><div class="slogan">APP_NAME</div></div>
    <div class="totp-form">
      <div class="totp-text"><span>${title}</span></div>
      ${content}
      ${actions("totp", submit)}
    </div>
  </div>
</div>`;

const CHANGE_PASSWORD = passwordScreen("Change your password",
  field("Usr", "text") + field("OldPas", "password") + field("Pas", "password") + field("RetPas", "password"),
  "ButChdPwd");
const EXPIRED_PASSWORD = passwordScreen("Your password has expired", field("Pas", "password") + field("RetPas", "password"),
  "ButChdExpPwd", "ButBckExpPwd");
const CHECK_2FA = totpScreen("Check the code", field("code", "text"), "ButChk2fa");
const ACTIVATE_2FA = totpScreen("Activate two-factor", `<div class="tag-list" id="2fa-code-list">
  <div class="panel-padding text-center">Scan the QR code</div>
  <div class="qr-code-container text-center"><img id="qr-code" class="qr-code" src="access/qr-code"></div>
  <div class="panel panel-info panel-body-colorful"><div class="panel-body text-center text-bold"><div>JBSWY3DPEHPK3PXP</div></div></div>
</div>`, "ButChk2fa");

const TRANSLATIONS = {
  SCREEN_TITLE_LOGIN: "Application login",
  SCREEN_TEXT_POWERED_BY: "Powered by",
  BRAND_TITLE: "Welcome to Acme",
  BRAND_SUBTITLE: "Use your corporate account",
  BRAND_PANEL: "Operations Hub",
  BRAND_PANEL_TEXT: "From the warehouse to the door",
  BRAND_FOOTER: "Acme © 2026",
  "ENUM_LAN_EN-GB": "English",
  "ENUM_LAN_ES-ES": "Spanish",
  "ENUM_LAN_FR-FR": "French",
  PARAMETER_LANGUAGE: "Language",
  PARAMETER_THEME_MODE: "Mode",
  ENUM_THEME_MODE_LIGHT: "Light",
  ENUM_THEME_MODE_DARK: "Dark",
  ENUM_THEME_MODE_SYSTEM: "System",
  SCREEN_TEXT_SHOW_PASSWORD: "Show password",
  SCREEN_TEXT_HIDE_PASSWORD: "Hide password",
  SCREEN_TEXT_CAPS_LOCK_ON: "Caps Lock is on",
  SCREEN_TEXT_SIGNING_IN: "Signing in…",
  SCREEN_TEXT_LOGIN_OR: "or continue with",
  ERROR_TITLE_INVALID_CREDENTIALS: "Incorrect username or password."
};

const LOCALES = path.resolve(__dirname, "../../../../../awe-generic-screens/src/main/resources/application/awe/locale");
const SIGNIN_XML = path.resolve(__dirname,
  "../../../../../awe-generic-screens/src/main/resources/application/awe/screen/security/signin.xml");
const REACT_SIGNIN_XML = path.resolve(__dirname,
  "../../../../../awe-generic-screens/src/main/resources/application/awe-react/screen/signin.xml");
const MODE_KEY = "awe-login-mode";
const LANGUAGES = ["en-GB", "es-ES", "fr-FR"];

/**
 * Value of a locale key in a locale file of awe/locale
 * @param {string} file Locale file
 * @param {string} key Locale key
 * @returns {string|undefined} Value
 */
const localeValue = (file, key) => fs.readFileSync(path.join(LOCALES, file), "utf8")
  .match(new RegExp(`<locale name="${key}" value="([^"]*)"/>`))?.[1];

describe("awe-signin: configurable sign-in page", () => {
  let $compile, $rootScope, $settings, $translate, $control, $controller, $actionController, translations, darkQuery;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$compile", "$rootScope", "$httpBackend", "AweSettings", "$translate", "Control", "$controller",
      "ActionController",
      (_$compile_, _$rootScope_, $httpBackend, AweSettings, _$translate_, Control, _$controller_, ActionController) => {
        $control = Control;
        $controller = _$controller_;
        $actionController = ActionController;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $settings = AweSettings;
        $translate = _$translate_;
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
      }]);
    translations = {...TRANSLATIONS};
    jest.spyOn($translate, "instant").mockImplementation(key => key in translations ? translations[key] : key);
    darkQuery = {matches: false, listeners: [], addEventListener: jest.fn((type, fn) => darkQuery.listeners.push(fn)),
      removeEventListener: jest.fn((type, fn) => darkQuery.listeners = darkQuery.listeners.filter(l => l !== fn))};
    window.matchMedia = jest.fn(() => darkQuery);
  });

  afterEach(() => {
    document.documentElement.classList.remove("dark", "light");
    document.body.innerHTML = "";
    window.localStorage.clear();
    delete window.matchMedia;
  });

  /**
   * Render the sign-in markup with the given login settings
   * @param {object|undefined} login settings.login (undefined: a server that does not send it)
   * @param {string} markup Screen markup
   * @returns {{page: HTMLElement, scope: object}} Page root and its scope
   */
  function render(login, markup = SIGNIN) {
    $settings.update({login});
    const scope = $rootScope.$new();
    const element = $compile(angular.element("<div>").html(markup).contents())(scope);
    const page = element.filter((_index, node) => node.nodeType === 1)[0];
    document.body.appendChild(page);
    scope.$digest();
    return {page, scope};
  }

  const text = (page, selector) => page.querySelector(selector)?.textContent.trim();
  const flushObserver = () => new Promise(resolve => setTimeout(resolve, 0));
  const serverDefaults = {layout: "split", title: "SCREEN_TITLE_LOGIN", poweredBy: true};

  describe("layout (S4)", () => {
    it.each(["split", "form-only", "card"])("sets data-awe-login-layout=%s on the page root", layout => {
      const {page} = render({...serverDefaults, layout});
      expect(page.getAttribute("data-awe-login-layout")).toBe(layout);
    });

    it("falls back to split for an unknown or absent layout", () => {
      expect(render({...serverDefaults, layout: "diagonal"}).page.getAttribute("data-awe-login-layout")).toBe("split");
      document.body.innerHTML = "";
      expect(render({title: "SCREEN_TITLE_LOGIN"}).page.getAttribute("data-awe-login-layout")).toBe("split");
    });

  });

  describe("images (S5)", () => {
    it("publishes a relative background as --awe-login-image resolved against the base URI", () => {
      const {page} = render({...serverDefaults, background: "images/brand/bg.jpg"});
      expect(page.style.getPropertyValue("--awe-login-image")).toBe(`url("${new URL("images/brand/bg.jpg", document.baseURI).href}")`);
    });

    it("keeps an absolute background as it is", () => {
      const {page} = render({...serverDefaults, background: "https://cdn.acme.com/bg.jpg"});
      expect(page.style.getPropertyValue("--awe-login-image")).toBe('url("https://cdn.acme.com/bg.jpg")');
    });

    it("escapes the quotes of the background URL", () => {
      const {page} = render({...serverDefaults, background: 'https://cdn.acme.com/a"b.jpg'});
      expect(page.style.getPropertyValue("--awe-login-image")).toBe('url("https://cdn.acme.com/a%22b.jpg")');
    });

    it("keeps the AWE panel when no background is configured", () => {
      const {page} = render(serverDefaults);
      expect(page.style.getPropertyValue("--awe-login-image")).toBe("");
    });

    it("replaces the AWE logo with the configured logo, resolved against the base URI", () => {
      const {page} = render({...serverDefaults, logo: "images/brand/logo.svg"});
      const logos = page.querySelectorAll(".signin-info > img.signin-logo");
      expect(logos).toHaveLength(1);
      expect(logos[0].getAttribute("src")).toBe(new URL("images/brand/logo.svg", document.baseURI).href);
      expect(logos[0].getAttribute("alt")).toBe("");
    });

    it("keeps the AWE logo when no logo is configured", () => {
      const {page} = render(serverDefaults);
      expect(page.querySelector(".signin-logo")).toBeNull();
    });

    it("uses logoDark while the colour mode is dark and follows a mode change", async () => {
      document.documentElement.classList.add("dark");
      const {page} = render({...serverDefaults, logo: "https://acme.com/logo.svg", logoDark: "https://acme.com/logo-dark.svg"});
      expect(page.querySelector(".signin-logo").getAttribute("src")).toBe("https://acme.com/logo-dark.svg");

      document.documentElement.classList.replace("dark", "light");
      await flushObserver();
      expect(page.querySelector(".signin-logo").getAttribute("src")).toBe("https://acme.com/logo.svg");
    });

    it("uses logoDark in the system mode when the system prefers dark", () => {
      darkQuery.matches = true;
      const {page} = render({...serverDefaults, logo: "https://acme.com/logo.svg", logoDark: "https://acme.com/logo-dark.svg"});
      expect(page.querySelector(".signin-logo").getAttribute("src")).toBe("https://acme.com/logo-dark.svg");

      darkQuery.matches = false;
      darkQuery.listeners.forEach(listener => listener());
      expect(page.querySelector(".signin-logo").getAttribute("src")).toBe("https://acme.com/logo.svg");
    });

    it("ignores the system preference when the light mode is forced", () => {
      darkQuery.matches = true;
      document.documentElement.classList.add("light");
      const {page} = render({...serverDefaults, logo: "https://acme.com/logo.svg", logoDark: "https://acme.com/logo-dark.svg"});
      expect(page.querySelector(".signin-logo").getAttribute("src")).toBe("https://acme.com/logo.svg");
    });

    it("stops following the mode when the screen is destroyed", () => {
      const {scope} = render({...serverDefaults, logo: "https://acme.com/logo.svg", logoDark: "https://acme.com/logo-dark.svg"});
      scope.$destroy();
      expect(darkQuery.removeEventListener).toHaveBeenCalledWith("change", expect.any(Function));
      expect(darkQuery.listeners).toHaveLength(0);
    });
  });

  describe("texts (S5, S8)", () => {
    it("translates a title that is a locale key and shows any other value as is", () => {
      expect(text(render({...serverDefaults, title: "BRAND_TITLE"}).page, ".signin-form > .signin-text")).toBe("Welcome to Acme");
      document.body.innerHTML = "";
      expect(text(render({...serverDefaults, title: "Sign in to Acme"}).page, ".signin-form > .signin-text")).toBe("Sign in to Acme");
    });

    it("shows no title when the title is empty", () => {
      const {page} = render({layout: "split", poweredBy: true});
      expect(page.querySelector(".signin-text")).toBeNull();
    });

    it("writes the texts as text, never as markup", () => {
      const {page} = render({...serverDefaults, subtitle: "<b>bold</b>"});
      expect(page.querySelector(".signin-subtitle b")).toBeNull();
      expect(text(page, ".signin-subtitle")).toBe("<b>bold</b>");
    });

    it("shows the subtitle under the title, translated or as is", () => {
      const {page} = render({...serverDefaults, subtitle: "BRAND_SUBTITLE"});
      const subtitle = page.querySelector(".signin-form > .signin-subtitle");
      expect(subtitle.textContent).toBe("Use your corporate account");
      expect(subtitle.previousElementSibling.classList.contains("signin-text")).toBe(true);
      document.body.innerHTML = "";
      expect(text(render({...serverDefaults, subtitle: "Corporate account"}).page, ".signin-subtitle")).toBe("Corporate account");
    });

    it("shows the caption of the panel (title and one line)", () => {
      const {page} = render({...serverDefaults, panelTitle: "BRAND_PANEL", panelSubtitle: "Built on AWE"});
      expect(text(page, "#page-signin-bg > .signin-caption > .signin-caption-title")).toBe("Operations Hub");
      expect(text(page, "#page-signin-bg > .signin-caption > .signin-caption-text")).toBe("Built on AWE");
    });

    it("shows only the caption parts that are configured", () => {
      const {page} = render({...serverDefaults, panelSubtitle: "BRAND_PANEL_TEXT"});
      expect(page.querySelector(".signin-caption-title")).toBeNull();
      expect(text(page, ".signin-caption-text")).toBe("From the warehouse to the door");
    });

    it("shows the footer text over Powered by AWE 5 in the left column of the footer", () => {
      const {page} = render({...serverDefaults, footerText: "BRAND_FOOTER"});
      const legal = page.querySelector(":scope > footer.signin-footer > .signin-legal:first-child");
      expect([...legal.children].map(child => child.className)).toEqual(["signin-footer-text", "signin-powered"]);
      expect(legal.firstElementChild.textContent).toBe("Acme © 2026");
    });

    it("translates the texts again when the language changes", () => {
      const {page} = render({...serverDefaults, subtitle: "BRAND_SUBTITLE", panelTitle: "BRAND_PANEL", footerText: "BRAND_FOOTER"});
      translations = {
        SCREEN_TITLE_LOGIN: "Acceso a la aplicación",
        SCREEN_TEXT_POWERED_BY: "Desarrollado con",
        BRAND_SUBTITLE: "Usa tu cuenta corporativa",
        BRAND_PANEL: "Centro de operaciones",
        BRAND_FOOTER: "Acme © 2026 ES"
      };
      $rootScope.$broadcast("$translateChangeSuccess", {language: "es"});
      expect(text(page, ".signin-text")).toBe("Acceso a la aplicación");
      expect(text(page, ".signin-subtitle")).toBe("Usa tu cuenta corporativa");
      expect(text(page, ".signin-caption-title")).toBe("Centro de operaciones");
      expect(text(page, ".signin-footer-text")).toBe("Acme © 2026 ES");
      expect(text(page, ".signin-powered").replace(/\s+/g, " ")).toBe("Desarrollado con AWE 5");
    });

    it("shows a value that is not a locale key as is with the engine translate", () => {
      $translate.instant.mockRestore();
      const {page} = render({...serverDefaults, title: "Sign in to Acme", footerText: "Acme Corp © 2026"});
      expect(text(page, ".signin-text")).toBe("Sign in to Acme");
      expect(text(page, ".signin-footer-text")).toBe("Acme Corp © 2026");
    });
  });

  describe("footer (S2, S5)", () => {
    it("shows Powered by AWE 5 by default", () => {
      const {page} = render(serverDefaults);
      expect(text(page, ".signin-footer > .signin-legal > .signin-powered").replace(/\s+/g, " ")).toBe("Powered by AWE 5");
      expect(page.querySelector(".signin-powered > b").textContent).toBe("AWE");
    });

    it("hides Powered by AWE 5 when poweredBy is false", () => {
      const {page} = render({...serverDefaults, poweredBy: false, footerText: "BRAND_FOOTER"});
      expect(page.querySelector(".signin-powered")).toBeNull();
      expect(text(page, ".signin-footer-text")).toBe("Acme © 2026");
    });

    it("shows no footer without footer text, Powered by and selectors", () => {
      const {page} = render({...serverDefaults, poweredBy: false, languageSelector: false, modeSelector: false,
        languages: LANGUAGES});
      expect(page.querySelector(".signin-footer")).toBeNull();
    });

    it.each(["Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"])("%s translates SCREEN_TEXT_POWERED_BY", file => {
      expect(fs.readFileSync(path.join(LOCALES, file), "utf8")).toMatch(/<locale name="SCREEN_TEXT_POWERED_BY" value="[^"]+"\/>/);
    });
  });

  describe("framework defaults (S6, S9, S13)", () => {
    // settings.login of the server without any awe.application.login.* property (BaseConfigProperties.Login)
    const frameworkDefaults = {layout: "split", title: "SCREEN_TEXT_LOGIN_TITLE", subtitle: "SCREEN_TEXT_LOGIN_SUBTITLE",
      panelTitle: "SCREEN_TEXT_LOGIN_PANEL_TITLE", panelSubtitle: "SCREEN_TEXT_LOGIN_PANEL_SUBTITLE", poweredBy: true,
      languageSelector: true, modeSelector: true, languages: LANGUAGES};

    it.each([["the server defaults", frameworkDefaults], ["no login settings", undefined]])("renders the AWE defaults with %s", (_name, login) => {
      ["SCREEN_TEXT_LOGIN_TITLE", "SCREEN_TEXT_LOGIN_SUBTITLE", "SCREEN_TEXT_LOGIN_PANEL_TITLE",
        "SCREEN_TEXT_LOGIN_PANEL_SUBTITLE", "SCREEN_TEXT_POWERED_BY"].forEach(key =>
        translations[key] = localeValue("Locale-en-GB.xml", key));
      const {page} = render(login);
      expect(page.getAttribute("data-awe-login-layout")).toBe("split");
      expect(page.style.getPropertyValue("--awe-login-image")).toBe("");
      expect(page.querySelector(".signin-logo")).toBeNull();
      expect(text(page, ".signin-form > .signin-text")).toBe("Welcome to AWE");
      expect(text(page, ".signin-subtitle")).toBe("Enter your credentials to continue.");
      expect(text(page, ".signin-caption-title")).toBe("AWE Framework");
      expect(text(page, ".signin-caption-text")).toBe("Low coding complete functional web applications");
      expect(page.querySelector(".signin-footer-text")).toBeNull();
      expect(text(page, ".signin-powered").replace(/\s+/g, " ")).toBe("Powered by AWE 5");
      expect(page.querySelector(".signin-mode")).not.toBeNull();
    });

    it("keeps the screen ids and the panel of the sign-in screen", () => {
      const {page} = render({...serverDefaults, layout: "card", logo: "logo.svg", subtitle: "x", panelTitle: "y", footerText: "z"});
      ["page-signin-bg", "cod_usr", "pwd_usr", "ButLogIn", "ButLogInAzure", "ButLogInSSO"].forEach(id =>
        expect(page.querySelector(`#${id}`)).not.toBeNull());
      expect(page.querySelector(":scope > #page-signin-bg")).not.toBeNull();
      expect(page.querySelector(":scope > .awe-signin.signin-container > .signin-form")).not.toBeNull();
    });
  });

  describe("language selector (S11)", () => {
    const withLanguages = {...serverDefaults, languages: LANGUAGES};
    const combobox = page => page.querySelector(".signin-prefs > .signin-language > [role=combobox]");
    const listbox = page => page.querySelector(".signin-language > [role=listbox]");
    const options = page => [...listbox(page).querySelectorAll("[role=option]")];
    const key = (target, name) => target.dispatchEvent(new window.KeyboardEvent("keydown", {key: name, bubbles: true, cancelable: true}));

    beforeEach(() => {
      $settings.update({language: "en-GB"});
      jest.spyOn($settings, "changeLanguage").mockImplementation(() => undefined);
    });

    it("shows the languages of settings.login on the right of the footer, the current one selected", () => {
      const {page} = render(withLanguages);
      const footer = page.querySelector(":scope > footer.signin-footer");
      expect(footer.lastElementChild.className).toBe("signin-prefs");
      expect(combobox(page).getAttribute("aria-label")).toBe("Language");
      expect(combobox(page).getAttribute("aria-haspopup")).toBe("listbox");
      expect(combobox(page).getAttribute("aria-expanded")).toBe("false");
      expect(combobox(page).getAttribute("tabindex")).toBe("0");
      expect(combobox(page).querySelector(".signin-language-value").textContent).toBe("English");
      expect(combobox(page).querySelectorAll("svg use")[0].getAttribute("href")).toMatch(/#globe$/);
      expect(combobox(page).querySelectorAll("svg use")[1].getAttribute("href")).toMatch(/#chevron-down$/);
      expect(listbox(page).hidden).toBe(true);
      expect(options(page).map(option => option.textContent)).toEqual(["English", "Spanish", "French"]);
      expect(options(page).map(option => option.getAttribute("aria-selected"))).toEqual(["true", "false", "false"]);
      expect(page.querySelector("select")).toBeNull();
    });

    it("changes the language with the engine language switch when an option is chosen", () => {
      const {page} = render(withLanguages);
      combobox(page).click();
      expect(listbox(page).hidden).toBe(false);
      expect(combobox(page).getAttribute("aria-expanded")).toBe("true");
      options(page)[1].click();
      expect($settings.changeLanguage).toHaveBeenCalledWith("es-ES");
      expect(listbox(page).hidden).toBe(true);
      expect(options(page).map(option => option.getAttribute("aria-selected"))).toEqual(["false", "true", "false"]);
    });

    it("opens with the keyboard, moves with the arrows, chooses with Enter and closes with Escape", () => {
      const {page} = render(withLanguages);
      key(combobox(page), "ArrowDown");
      expect(listbox(page).hidden).toBe(false);
      expect(combobox(page).getAttribute("aria-activedescendant")).toBe(options(page)[0].id);
      key(combobox(page), "ArrowDown");
      key(combobox(page), "ArrowDown");
      key(combobox(page), "ArrowDown");
      expect(combobox(page).getAttribute("aria-activedescendant")).toBe(options(page)[2].id);
      key(combobox(page), "Home");
      expect(combobox(page).getAttribute("aria-activedescendant")).toBe(options(page)[0].id);
      key(combobox(page), "End");
      key(combobox(page), "ArrowUp");
      key(combobox(page), "Enter");
      expect($settings.changeLanguage).toHaveBeenCalledWith("es-ES");
      expect(listbox(page).hidden).toBe(true);
      expect(combobox(page).hasAttribute("aria-activedescendant")).toBe(false);

      key(combobox(page), " ");
      expect(listbox(page).hidden).toBe(false);
      key(combobox(page), "Escape");
      expect(listbox(page).hidden).toBe(true);
      expect($settings.changeLanguage).toHaveBeenCalledTimes(1);
    });

    it("does not change the language when the current one is chosen again", () => {
      const {page} = render(withLanguages);
      combobox(page).click();
      options(page)[0].click();
      expect($settings.changeLanguage).not.toHaveBeenCalled();
    });

    it("translates the selector again when the language changes", () => {
      const {page} = render(withLanguages);
      translations = {...translations, "ENUM_LAN_ES-ES": "Español", "ENUM_LAN_EN-GB": "Inglés", PARAMETER_LANGUAGE: "Idioma"};
      $settings.update({language: "es-ES"});
      $rootScope.$broadcast("$translateChangeSuccess", {language: "es-ES"});
      expect(combobox(page).querySelector(".signin-language-value").textContent).toBe("Español");
      expect(combobox(page).getAttribute("aria-label")).toBe("Idioma");
      expect(options(page)[0].textContent).toBe("Inglés");
      expect(options(page).map(option => option.getAttribute("aria-selected"))).toEqual(["false", "true", "false"]);
    });

    it.each([
      ["languageSelector is false", {languageSelector: false}],
      ["there are no languages", {languages: undefined}],
      ["there is a single language", {languages: ["en-GB"]}]
    ])("is not shown when %s", (_name, login) => {
      const {page} = render({...withLanguages, ...login});
      expect(page.querySelector(".signin-language")).toBeNull();
      expect(page.querySelector(".signin-mode")).not.toBeNull();
    });
  });

  describe("colour mode selector (S11)", () => {
    const radios = page => [...page.querySelectorAll(".signin-prefs > .signin-mode[role=radiogroup] input[type=radio]")];
    const checked = page => radios(page).find(radio => radio.checked)?.value;
    const choose = (page, mode) => {
      const radio = radios(page).find(input => input.value === mode);
      radio.checked = true;
      radio.dispatchEvent(new Event("change", {bubbles: true}));
    };
    const root = document.documentElement;

    it("shows light, dark and system as a radiogroup with the sun, moon and monitor icons", () => {
      const {page} = render(serverDefaults);
      const group = page.querySelector(".signin-footer > .signin-prefs > .signin-mode");
      expect(group.getAttribute("role")).toBe("radiogroup");
      expect(group.getAttribute("aria-label")).toBe("Mode");
      expect(radios(page).map(radio => radio.value)).toEqual(["light", "dark", "system"]);
      expect(new Set(radios(page).map(radio => radio.name)).size).toBe(1);
      expect([...group.querySelectorAll("label")].map(label => label.getAttribute("title"))).toEqual(["Light", "Dark", "System"]);
      expect([...group.querySelectorAll("label .sr-only")].map(label => label.textContent)).toEqual(["Light", "Dark", "System"]);
      expect([...group.querySelectorAll("svg use")].map(use => use.getAttribute("href").split("#")[1]))
        .toEqual(["sun", "moon", "monitor"]);
    });

    it.each([["dark", "dark"], ["light", "light"], ["system", null]])("checks the current mode (%s)", (mode, cssClass) => {
      if (cssClass) {
        root.classList.add(cssClass);
      }
      expect(checked(render(serverDefaults).page)).toBe(mode);
    });

    it("switches the page mode at once with the engine theme appearance and remembers it", () => {
      const {page} = render(serverDefaults);
      choose(page, "dark");
      expect(root.classList.contains("dark")).toBe(true);
      expect($settings.get("themeMode")).toBe("dark");
      expect(window.localStorage.getItem(MODE_KEY)).toBe("dark");
      choose(page, "light");
      expect(root.classList.contains("light")).toBe(true);
      expect(root.classList.contains("dark")).toBe(false);
      choose(page, "system");
      expect(root.classList.contains("light") || root.classList.contains("dark")).toBe(false);
      expect(window.localStorage.getItem(MODE_KEY)).toBe("system");
      expect(checked(page)).toBe("system");
    });

    it("applies the remembered mode when the sign-in page opens", () => {
      window.localStorage.setItem(MODE_KEY, "dark");
      const {page} = render(serverDefaults);
      expect(root.classList.contains("dark")).toBe(true);
      expect(checked(page)).toBe("dark");
    });

    it("ignores a remembered value that is not a mode", () => {
      window.localStorage.setItem(MODE_KEY, "sepia");
      root.classList.add("light");
      expect(checked(render(serverDefaults).page)).toBe("light");
      expect(root.classList.contains("light")).toBe(true);
    });

    it("follows a mode change made elsewhere", async () => {
      const {page} = render(serverDefaults);
      root.classList.add("dark");
      await flushObserver();
      expect(checked(page)).toBe("dark");
    });

    it("is not shown, and does not apply the remembered mode, when modeSelector is false", () => {
      window.localStorage.setItem(MODE_KEY, "dark");
      const {page} = render({...serverDefaults, modeSelector: false});
      expect(page.querySelector(".signin-mode")).toBeNull();
      expect(root.classList.contains("dark")).toBe(false);
    });
  });

  describe("password field (S12)", () => {
    const password = page => page.querySelector("#pwd_usr");
    const toggle = page => page.querySelector("#pwd_usr + button.signin-password-toggle");
    const caps = page => page.querySelector(".signin-caps");
    const keyEvent = (type, capsLock) => Object.assign(new window.KeyboardEvent(type, {key: "A"}),
      {getModifierState: modifier => modifier === "CapsLock" && capsLock});

    it("shows and hides the password with the eye button", () => {
      const {page} = render(serverDefaults);
      expect(toggle(page).getAttribute("type")).toBe("button");
      expect(toggle(page).getAttribute("aria-controls")).toBe("pwd_usr");
      expect(toggle(page).getAttribute("aria-pressed")).toBe("false");
      expect(toggle(page).getAttribute("aria-label")).toBe("Show password");
      expect(toggle(page).querySelector("use").getAttribute("href")).toMatch(/#eye$/);

      toggle(page).click();
      expect(password(page).type).toBe("text");
      expect(toggle(page).getAttribute("aria-pressed")).toBe("true");
      expect(toggle(page).getAttribute("aria-label")).toBe("Hide password");
      expect(toggle(page).querySelector("use").getAttribute("href")).toMatch(/#eye-off$/);

      toggle(page).click();
      expect(password(page).type).toBe("password");
      expect(toggle(page).getAttribute("aria-pressed")).toBe("false");
      expect(toggle(page).getAttribute("aria-label")).toBe("Show password");
    });

    it("warns under the password while Caps Lock is on", () => {
      const {page} = render(serverDefaults);
      const warning = caps(page);
      expect(warning.hidden).toBe(true);
      expect(warning.parentElement.classList.contains("form-group")).toBe(true);
      expect(warning.textContent.trim()).toBe("Caps Lock is on");
      expect(warning.querySelector("use").getAttribute("href")).toMatch(/#triangle-alert$/);
      expect(password(page).getAttribute("aria-describedby")).toBe(`pwd_usr-help ${warning.id}`);

      password(page).dispatchEvent(keyEvent("keydown", true));
      expect(warning.hidden).toBe(false);
      password(page).dispatchEvent(keyEvent("keyup", false));
      expect(warning.hidden).toBe(true);
      password(page).dispatchEvent(keyEvent("keyup", true));
      password(page).dispatchEvent(new window.FocusEvent("blur"));
      expect(warning.hidden).toBe(true);
    });

    it("translates the eye button and the warning again when the language changes", () => {
      const {page} = render(serverDefaults);
      translations = {...translations, SCREEN_TEXT_SHOW_PASSWORD: "Mostrar contraseña", SCREEN_TEXT_CAPS_LOCK_ON: "Bloq Mayús"};
      $rootScope.$broadcast("$translateChangeSuccess", {language: "es"});
      expect(toggle(page).getAttribute("aria-label")).toBe("Mostrar contraseña");
      expect(caps(page).textContent.trim()).toBe("Bloq Mayús");
    });

    it("enhances the password field when its criterion renders after the page", () => {
      const markup = SIGNIN.replace(/<div class="criterion">[\s\S]*?<\/div><\/div>\n/, "");
      const {page, scope} = render(serverDefaults, markup);
      expect(page.querySelector(".signin-password-toggle")).toBeNull();
      const input = Object.assign(document.createElement("input"), {type: "password", id: "pwd_usr"});
      const field = document.createElement("div");
      field.appendChild(input);
      page.querySelector(".signin-form").appendChild(field);
      scope.$digest();
      scope.$digest();
      expect(page.querySelectorAll(".signin-password-toggle")).toHaveLength(1);
      expect(page.querySelectorAll(".signin-caps")).toHaveLength(1);
    });

    it("draws a single eye button: the one of the page replaces the one of the password criterion (#793 S17)", () => {
      const markup = SIGNIN.replace('aria-describedby="pwd_usr-help">',
        'aria-describedby="pwd_usr-help"><button type="button" class="password-toggle" aria-controls="pwd_usr"></button>');
      const {page} = render(serverDefaults, markup);
      expect(page.querySelectorAll("button[aria-controls=\"pwd_usr\"]")).toHaveLength(1);
      expect(page.querySelector(".password-toggle")).toBeNull();
      expect(toggle(page)).not.toBeNull();

      toggle(page).click();
      expect(password(page).type).toBe("text");
      expect(toggle(page).getAttribute("aria-label")).toBe("Hide password");
    });

    it("labels the user and password fields above them, with the user and lock icons", () => {
      const xml = fs.readFileSync(SIGNIN_XML, "utf8");
      const criterion = id => xml.match(new RegExp(`<criteria [^>]*id="${id}"[^>]*>`))[0];
      expect(criterion("cod_usr")).toMatch(/label="SCREEN_TEXT_USER"/);
      expect(criterion("cod_usr")).toMatch(/icon="user signin-form-icon"/);
      expect(criterion("pwd_usr")).toMatch(/label="SCREEN_TEXT_PASS"/);
      expect(criterion("pwd_usr")).toMatch(/icon="lock signin-form-icon"/);
      ["cod_usr", "pwd_usr"].forEach(id => expect(criterion(id)).not.toMatch(/no-label|placeholder=/));
    });
  });

  describe("login loading (S14)", () => {
    const button = page => page.querySelector("#ButLogIn");
    const form = page => page.querySelector(".signin-form");
    // The engine marks the button of a running server action as loading (ServerData.launchServerAction) and ends it
    // with the end-load action of the answer, of the error answer and of a failed request (Control.changeControllerAttribute)
    const loading = (component, value) => {
      $control.publishControllerChanged({view: "base", component}, {loading: value});
      $rootScope.$digest();
    };

    it("shows the spinner and Signing in…, disables the button and the fields and marks the form busy while the login "
      + "request runs", () => {
      const {page} = render(serverDefaults);
      loading("ButLogIn", true);
      expect(button(page).classList.contains("is-loading")).toBe(true);
      expect(button(page).disabled).toBe(true);
      const busy = button(page).querySelector(":scope > .signin-busy");
      expect(busy.textContent.trim()).toBe("Signing in…");
      const spinner = busy.querySelector("svg.awe-icon");
      expect(spinner.classList.contains("awe-icon-spin")).toBe(true);
      expect(spinner.getAttribute("aria-hidden")).toBe("true");
      expect(spinner.querySelector("use").getAttribute("href")).toMatch(/#loader-circle$/);
      ["#cod_usr", "#pwd_usr", ".signin-password-toggle"].forEach(field =>
        expect(page.querySelector(field).disabled).toBe(true));
      expect(form(page).getAttribute("aria-busy")).toBe("true");
    });

    it("returns to normal when the request ends, also on error, and keeps the fields that were already disabled", () => {
      const {page} = render(serverDefaults);
      page.querySelector("#cod_usr").disabled = true;
      loading("ButLogIn", true);
      loading("ButLogIn", false);
      expect(button(page).classList.contains("is-loading")).toBe(false);
      expect(button(page).disabled).toBe(false);
      expect(button(page).querySelector(".signin-busy")).toBeNull();
      expect(button(page).querySelector(".button-text").textContent).toBe("Login");
      expect(page.querySelector("#pwd_usr").disabled).toBe(false);
      expect(page.querySelector(".signin-password-toggle").disabled).toBe(false);
      expect(page.querySelector("#cod_usr").disabled).toBe(true);
      expect(form(page).hasAttribute("aria-busy")).toBe(false);
    });

    it("follows the login button only", () => {
      const {page} = render(serverDefaults);
      loading("ButLogInSSO", true);
      expect(button(page).classList.contains("is-loading")).toBe(false);
      expect(page.querySelector("#pwd_usr").disabled).toBe(false);
      expect(form(page).hasAttribute("aria-busy")).toBe(false);
    });

    it("translates Signing in… again when the language changes", () => {
      const {page} = render(serverDefaults);
      loading("ButLogIn", true);
      translations = {...translations, SCREEN_TEXT_SIGNING_IN: "Iniciando sesión…"};
      $rootScope.$broadcast("$translateChangeSuccess", {language: "es"});
      expect(button(page).querySelector(".signin-busy").textContent.trim()).toBe("Iniciando sesión…");
    });
  });

  describe("failed login (S17)", () => {
    const LOGIN = {view: "base", component: "ButLogIn"};
    let messages;

    // The alert zone of the application (index.html): the toasts of the message actions
    beforeEach(() => {
      messages = $controller("MessageController", {$scope: $rootScope.$new()});
      jest.spyOn($actionController, "acceptAction").mockImplementation(() => null);
      jest.spyOn($actionController, "sendMessage").mockImplementation(() => null);
    });

    /**
     * The message action of an answer, as the engine launches it (ActionController -> "/action/message")
     * @param {object} parameters Message parameters (type, title, message)
     * @param {object} callbackTarget Address of the component whose server action answered
     * @returns {object} Action
     */
    const answer = (parameters, callbackTarget = LOGIN) => {
      const action = {attr: key => ({parameters, view: "base", callbackTarget})[key], isAlive: () => true};
      $rootScope.$broadcast("/action/message", action);
      return action;
    };
    const failed = {type: "warning", title: "ERROR_TITLE_INVALID_CREDENTIALS",
      message: "Check your credentials. 2 attempts left before the account is locked."};
    const alert = page => page.querySelector(".signin-form > .signin-alert");
    const user = page => page.querySelector("#cod_usr");
    const password = page => page.querySelector("#pwd_usr");
    const type = (input, value) => {
      input.value = value;
      input.dispatchEvent(new Event("input", {bubbles: true}));
    };
    const loading = (component, value) => {
      $control.publishControllerChanged({view: "base", component}, {loading: value});
      $rootScope.$digest();
    };

    it("shows the message of the failed login in a danger alert above the fields instead of the toast", () => {
      const {page} = render({...serverDefaults, subtitle: "BRAND_SUBTITLE"});
      type(user(page), "jdoe");
      type(password(page), "wrong");
      const action = answer(failed);

      const box = alert(page);
      expect(box.hidden).toBe(false);
      expect(box.getAttribute("role")).toBe("alert");
      expect(box.previousElementSibling.classList.contains("signin-subtitle")).toBe(true);
      expect(box.nextElementSibling.id).toBe("cod_usr");
      const icon = box.querySelector(":scope > svg.awe-icon");
      expect(icon.getAttribute("aria-hidden")).toBe("true");
      expect(icon.querySelector("use").getAttribute("href")).toMatch(/#circle-x$/);
      expect(box.querySelector("strong").textContent).toBe("Incorrect username or password.");
      expect(box.textContent.replace(/\s+/g, " ").trim())
        .toBe("Incorrect username or password. Check your credentials. 2 attempts left before the account is locked.");
      // No toast: the message action ends at once, so the rest of the answer runs
      expect(messages.alerts).toEqual([]);
      expect($actionController.acceptAction).toHaveBeenCalledWith(action);
      // The user is kept, the password cleared and focused
      expect(user(page).value).toBe("jdoe");
      expect(password(page).value).toBe("");
      expect(document.activeElement).toBe(password(page));
      expect(page.querySelectorAll(".signin-alert")).toHaveLength(1);
    });

    it("carries the contract of an alert of the toast: the type of the message, its title and its text", () => {
      const {page} = render(serverDefaults);
      type(user(page), "jdoe");
      type(password(page), "wrong");
      answer(failed);

      const box = alert(page);
      expect(box.getAttribute("data-testid")).toBe("alert");
      expect(box.getAttribute("data-type")).toBe("warning");
      // The way a test locates the message of a failed login (the toast of the message actions is not drawn)
      const text = page.querySelector("[data-testid='alert'][data-type='warning'] [data-testid='alert-message']");
      expect(text.textContent).toBe(failed.message);
      const title = page.querySelector("[data-testid='alert'][data-type='warning'] [data-testid='alert-title']");
      expect(title.tagName).toBe("STRONG");
      expect(title.textContent).toBe("Incorrect username or password.");
      expect(messages.alerts).toEqual([]);
    });

    it("carries the type of the message on an alert without a title", () => {
      const {page} = render(serverDefaults);
      answer({type: "error", message: "User <b>jdoe</b> is locked"});
      // Named as the toast names an error, so a test that looks for the alert of the toast finds it
      expect(alert(page).getAttribute("data-type")).toBe("danger");
      expect(alert(page).querySelector("[data-testid='alert-title']")).toBeNull();
      expect(alert(page).querySelector("[data-testid='alert-message']").textContent).toBe("User <b>jdoe</b> is locked");
    });

    it("puts the alert right under the title when there is no subtitle", () => {
      const {page} = render(serverDefaults);
      answer(failed);
      expect(alert(page).previousElementSibling.classList.contains("signin-text")).toBe(true);
    });

    it("clears the password in the model of the field too", () => {
      const {page, scope} = render(serverDefaults, SIGNIN.replace('id="pwd_usr"', 'id="pwd_usr" ng-model="login.password"'));
      scope.login = {password: "wrong"};
      scope.$digest();
      answer(failed);
      scope.$digest();
      expect(scope.login.password).toBe("");
      expect(password(page).value).toBe("");
    });

    it("shows an error message too, its text alone when it has no title, written as text", () => {
      const {page} = render(serverDefaults);
      answer({type: "error", message: "User <b>jdoe</b> is locked"});
      expect(alert(page).hidden).toBe(false);
      expect(alert(page).querySelector("strong")).toBeNull();
      expect(alert(page).querySelector("b")).toBeNull();
      expect(alert(page).textContent.trim()).toBe("User <b>jdoe</b> is locked");
      expect(messages.alerts).toEqual([]);
    });

    it("shows the last message only", () => {
      const {page} = render(serverDefaults);
      answer(failed);
      answer({type: "warning", title: "Locked", message: "The account is locked"});
      expect(alert(page).querySelectorAll("strong")).toHaveLength(1);
      expect(alert(page).textContent.replace(/\s+/g, " ").trim()).toBe("Locked The account is locked");
    });

    it("hides the alert when the user edits a field", () => {
      const {page} = render(serverDefaults);
      answer(failed);
      type(user(page), "jdoe2");
      expect(alert(page).hidden).toBe(true);
      answer(failed);
      type(password(page), "s");
      expect(alert(page).hidden).toBe(true);
    });

    it("hides the alert when a new login starts", () => {
      const {page} = render(serverDefaults);
      answer(failed);
      loading("ButLogIn", true);
      expect(alert(page).hidden).toBe(true);
    });

    it("has no visible alert before a failed login", () => {
      const {page} = render(serverDefaults);
      expect(alert(page).hidden).toBe(true);
      expect(alert(page).getAttribute("role")).toBe("alert");
    });

    it("keeps the toast for the other messages of the sign-in page", () => {
      const {page} = render(serverDefaults);
      answer({type: "warning", title: "Heads up", message: "From elsewhere"}, {view: "base", component: "ButLogInSSO"});
      answer({type: "ok", title: "Done", message: "Signed in"});
      answer({type: "info", title: "Info", message: "Note"}, {view: "base"});
      expect(alert(page).hidden).toBe(true);
      expect(messages.alerts.map(message => message.title)).toEqual(["Heads up", "Done", "Info"]);
    });

    it("keeps the toast on the other pages", () => {
      const {scope} = render(serverDefaults);
      scope.$destroy();
      answer(failed);
      expect(messages.alerts).toEqual([expect.objectContaining({type: "warning", title: "ERROR_TITLE_INVALID_CREDENTIALS",
        msg: failed.message})]);
    });

    it("sends the message as a toast when the failed login leaves the sign-in page (expired password)", () => {
      const {scope} = render(serverDefaults);
      answer(failed);
      scope.$destroy();
      expect($actionController.sendMessage).toHaveBeenCalledWith({view: "base"}, "warning",
        "ERROR_TITLE_INVALID_CREDENTIALS", failed.message);
    });

    it("sends nothing when the sign-in page closes without a visible alert", () => {
      const {page, scope} = render(serverDefaults);
      answer(failed);
      type(user(page), "x");
      scope.$destroy();
      expect($actionController.sendMessage).not.toHaveBeenCalled();
    });
  });

  describe("engines unified (S18)", () => {
    const buttonTag = (file, id) => fs.readFileSync(file, "utf8")
      .match(new RegExp(`<button [^>]*id="${id}"[^>]*>[\\s\\S]*?</button>`))[0];

    it("draws the Login button without an icon, as the React one, keeping its actions and dependency", () => {
      const login = buttonTag(SIGNIN_XML, "ButLogIn");
      expect(login.match(/<button [^>]*>/)[0]).not.toMatch(/icon=/);
      expect(buttonTag(REACT_SIGNIN_XML, "ButLogIn").match(/<button [^>]*>/)[0]).not.toMatch(/icon=/);
      expect(login).toMatch(/button-type="submit"/);
      expect(login).toMatch(/label="BUTTON_LOGIN"/);
      expect(login).toMatch(/<button-action type="validate" \/>/);
      expect(login).toMatch(/<button-action type="server" server-action="login" \/>/);
      expect(login).toMatch(/<dependency-element id="local-login" condition="eq" value="false"\/>/);
    });
  });

  // The change password, expired password and two-factor screens take the frame, the fields and the buttons of the
  // sign-in page, but keep their own titles and their toasts
  describe("password and two-factor screens (S19)", () => {
    const SCREENS = [
      ["change password", CHANGE_PASSWORD, ".change-password", "page-change-password-bg", "ButChdPwd"],
      ["expired password", EXPIRED_PASSWORD, ".change-password", "page-change-password-bg", "ButChdExpPwd"],
      ["two-factor check", CHECK_2FA, ".totp", "page-totp-bg", "ButChk2fa"],
      ["two-factor activation", ACTIVATE_2FA, ".totp", "page-totp-bg", "ButChk2fa"]
    ];
    const branded = {...serverDefaults, layout: "card", background: "https://cdn.acme.com/bg.jpg",
      logo: "https://acme.com/logo.svg", logoDark: "https://acme.com/logo-dark.svg", title: "BRAND_TITLE",
      subtitle: "BRAND_SUBTITLE", panelTitle: "BRAND_PANEL", panelSubtitle: "BRAND_PANEL_TEXT", footerText: "BRAND_FOOTER",
      languages: LANGUAGES};
    const loading = (component, value) => {
      $control.publishControllerChanged({view: "base", component}, {loading: value});
      $rootScope.$digest();
    };
    const keyEvent = (type, capsLock) => Object.assign(new window.KeyboardEvent(type, {key: "A"}),
      {getModifierState: modifier => modifier === "CapsLock" && capsLock});

    beforeEach(() => {
      $settings.update({language: "en-GB"});
    });

    it.each(SCREENS)("the %s screen takes the layout, background, logo, caption and footer of settings.login",
      (_name, markup, prefix, panelId) => {
        const {page} = render(branded, markup);
        expect(page.getAttribute("data-awe-login-layout")).toBe("card");
        expect(page.style.getPropertyValue("--awe-login-image")).toBe('url("https://cdn.acme.com/bg.jpg")');
        const logos = page.querySelectorAll(`${prefix}-header > img.signin-logo`);
        expect(logos).toHaveLength(1);
        expect(logos[0].getAttribute("src")).toBe("https://acme.com/logo.svg");
        expect(text(page, `#${panelId} > .signin-caption > .signin-caption-title`)).toBe("Operations Hub");
        expect(text(page, `#${panelId} > .signin-caption > .signin-caption-text`)).toBe("From the warehouse to the door");
        const footer = page.querySelector(":scope > footer.signin-footer");
        expect(text(footer, ".signin-legal > .signin-footer-text")).toBe("Acme © 2026");
        expect(text(footer, ".signin-legal > .signin-powered").replace(/\s+/g, " ")).toBe("Powered by AWE 5");
        expect(footer.querySelector(".signin-prefs > .signin-language [role=combobox]")).not.toBeNull();
        expect(footer.querySelectorAll(".signin-prefs > .signin-mode input[type=radio]")).toHaveLength(3);
      });

    it.each(SCREENS)("the %s screen keeps its own title, without the subtitle, alert and divider of the sign-in",
      (_name, markup, prefix) => {
        const {page} = render(branded, markup);
        const title = page.querySelector(`${prefix}-form > ${prefix}-text`).textContent.trim();
        expect(title).not.toBe("Welcome to Acme");
        expect(markup).toContain(`<span>${title}</span>`);
        [".signin-subtitle", ".signin-alert", ".signin-divider"].forEach(selector =>
          expect(page.querySelector(selector)).toBeNull());
      });

    it.each(SCREENS)("the %s screen uses logoDark in dark mode and the AWE defaults without login settings",
      (_name, markup) => {
        document.documentElement.classList.add("dark");
        expect(render(branded, markup).page.querySelector(".signin-logo").getAttribute("src"))
          .toBe("https://acme.com/logo-dark.svg");
        document.body.innerHTML = "";
        const {page} = render(undefined, markup);
        expect(page.getAttribute("data-awe-login-layout")).toBe("split");
        expect(page.style.getPropertyValue("--awe-login-image")).toBe("");
        expect(page.querySelector(".signin-logo")).toBeNull();
        expect(page.querySelector(".signin-powered")).not.toBeNull();
      });

    it.each([["change password", CHANGE_PASSWORD, ["OldPas", "Pas", "RetPas"]],
      ["expired password", EXPIRED_PASSWORD, ["Pas", "RetPas"]]])("the %s screen puts the eye and the Caps Lock warning "
      + "on every password field", (_name, markup, ids) => {
      const {page} = render(serverDefaults, markup);
      expect(page.querySelectorAll(".signin-password-toggle")).toHaveLength(ids.length);
      expect(page.querySelectorAll(".signin-caps")).toHaveLength(ids.length);
      ids.forEach(id => {
        const input = page.querySelector(`#${id}`);
        const toggle = page.querySelector(`#${id} + button.signin-password-toggle`);
        const caps = page.querySelector(`#${id}-caps`);
        expect(toggle.getAttribute("aria-controls")).toBe(id);
        expect(toggle.getAttribute("aria-label")).toBe("Show password");
        expect(input.getAttribute("aria-describedby")).toBe(caps.id);
        expect(caps.hidden).toBe(true);
        input.dispatchEvent(keyEvent("keydown", true));
        expect(caps.hidden).toBe(false);
        input.dispatchEvent(new window.FocusEvent("blur"));
        toggle.click();
        expect(input.type).toBe("text");
        expect(toggle.getAttribute("aria-pressed")).toBe("true");
        // Only the field of the button
        ids.filter(other => other !== id).forEach(other => expect(page.querySelector(`#${other}`).type).toBe("password"));
        toggle.click();
      });
      // The text fields have no eye
      expect(page.querySelector("#Usr + .signin-password-toggle")).toBeNull();
    });

    it("the two-factor screens have no eye: the code is not a password", () => {
      expect(render(serverDefaults, CHECK_2FA).page.querySelector(".signin-password-toggle, .signin-caps")).toBeNull();
    });

    it.each(SCREENS)("the %s screen shows the loading state on its submit button while its server action runs",
      (_name, markup, prefix, _panelId, submit) => {
        const {page} = render(serverDefaults, markup);
        const form = page.querySelector(`${prefix}-form`);
        const button = page.querySelector(`#${submit}`);
        const inputs = [...form.querySelectorAll("input:not([type=hidden]), .signin-password-toggle")];
        loading(submit, true);
        expect(button.classList.contains("is-loading")).toBe(true);
        expect(button.disabled).toBe(true);
        const busy = button.querySelector(":scope > .signin-busy");
        // The label of the button with the spinner: these screens do not sign in
        expect(busy.textContent.trim()).toBe("Confirm");
        expect(busy.querySelector("svg.awe-icon").classList.contains("awe-icon-spin")).toBe(true);
        inputs.forEach(input => expect(input.disabled).toBe(true));
        expect(form.getAttribute("aria-busy")).toBe("true");

        loading(submit, false);
        expect(button.classList.contains("is-loading")).toBe(false);
        expect(button.disabled).toBe(false);
        expect(button.querySelector(".signin-busy")).toBeNull();
        inputs.forEach(input => expect(input.disabled).toBe(false));
        expect(form.hasAttribute("aria-busy")).toBe(false);
      });

    it("follows the submit button only (the back button of the expired password screen)", () => {
      const {page} = render(serverDefaults, EXPIRED_PASSWORD);
      loading("ButBckExpPwd", true);
      expect(page.querySelector(".is-loading")).toBeNull();
      expect(page.querySelector("#Pas").disabled).toBe(false);
    });

    it.each(SCREENS)("the %s screen keeps the toast for its errors", (_name, markup, _prefix, _panelId, submit) => {
      const messages = $controller("MessageController", {$scope: $rootScope.$new()});
      jest.spyOn($actionController, "acceptAction").mockImplementation(() => null);
      render(serverDefaults, markup);
      const action = {attr: key => ({parameters: {type: "error", title: "Wrong", message: "The code is not valid"},
        view: "base", callbackTarget: {view: "base", component: submit}})[key], isAlive: () => true};
      $rootScope.$broadcast("/action/message", action);
      expect(messages.alerts).toEqual([expect.objectContaining({type: "danger", title: "Wrong",
        msg: "The code is not valid"})]);
      expect(document.querySelector(".signin-alert")).toBeNull();
    });

    it("the two-factor activation keeps the QR code and the secret code", () => {
      const {page} = render(branded, ACTIVATE_2FA);
      expect(page.querySelector(".totp-form .qr-code-container > img#qr-code").getAttribute("src")).toBe("access/qr-code");
      expect(text(page, ".totp-form .panel > .panel-body")).toBe("JBSWY3DPEHPK3PXP");
    });
  });

  describe("SSO block (S15, S16)", () => {
    const xml = () => fs.readFileSync(SIGNIN_XML, "utf8");
    const buttonTag = id => xml().match(new RegExp(`<button [^>]*id="${id}"[^>]*>[\\s\\S]*?</button>`))[0];

    it("puts the or continue with divider right after the login button, before the SSO buttons", () => {
      const {page} = render(serverDefaults);
      const divider = page.querySelector(".form-actions > .signin-divider");
      expect(divider.previousElementSibling.querySelector("#ButLogIn")).not.toBeNull();
      expect(divider.nextElementSibling.querySelector("#ButLogInAzure")).not.toBeNull();
      expect(divider.textContent.trim()).toBe("or continue with");
      expect(page.querySelectorAll(".signin-divider")).toHaveLength(1);
    });

    it("adds the divider when the buttons render after the page and translates it again", () => {
      const markup = SIGNIN.replace(/<div class="signin-with">[\s\S]*?<\/div><\/div>/, "");
      const {page, scope} = render(serverDefaults, markup);
      expect(page.querySelector(".signin-divider")).toBeNull();
      const actions = Object.assign(document.createElement("div"), {className: "form-actions"});
      actions.innerHTML = "<div><button id=\"ButLogIn\"></button></div><div><button id=\"ButLogInSSO\"></button></div>";
      page.querySelector(".signin-form").appendChild(actions);
      scope.$digest();
      scope.$digest();
      expect(actions.querySelectorAll(":scope > .signin-divider")).toHaveLength(1);
      translations = {...translations, SCREEN_TEXT_LOGIN_OR: "o continúe con"};
      $rootScope.$broadcast("$translateChangeSuccess", {language: "es"});
      expect(text(page, ".signin-divider")).toBe("o continúe con");
    });

    it("draws ButLogInAzure as the Sign in with Microsoft button, keeping its dependency (S15)", () => {
      const azure = buttonTag("ButLogInAzure");
      expect(azure).toMatch(/label="SCREEN_TEXT_SIGN_IN_MICROSOFT"/);
      expect(azure).toMatch(/style="no-class signin-microsoft"/);
      expect(azure).not.toMatch(/icon=/);
      expect(azure).toMatch(/<dependency-element id="azure-enabled" condition="eq" value="false"\/>/);
      expect(azure).toMatch(/<dependency-element id="azure-enabled" condition="is empty"\/>/);
      expect(azure).toMatch(/server-action="login-azure"/);
      ["Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"].forEach(file =>
        expect(localeValue(file, "SCREEN_TEXT_SIGN_IN_MICROSOFT")).toBeTruthy());
    });

    it("draws ButLogInSSO outlined with the key icon, keeping its label and dependency (S16)", () => {
      const sso = buttonTag("ButLogInSSO");
      expect(sso).toMatch(/label="BUTTON_LOGIN_SSO"/);
      expect(sso).toMatch(/icon="lucide:key-round"/);
      expect(sso).toMatch(/style="no-class btn btn-awe signin-btn sign-in-sso-btn"/);
      expect(sso).toMatch(/<dependency-element id="sso-enabled" condition="eq" value="false"\/>/);
      expect(sso).toMatch(/<dependency-element id="sso-enabled" condition="is empty"\/>/);
      expect(sso).toMatch(/server-action="login-sso"/);
      ["Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"].forEach(file =>
        ["SCREEN_TEXT_LOGIN_OR", "SCREEN_TEXT_SIGNING_IN"].forEach(key => expect(localeValue(file, key)).toBeTruthy()));
    });
  });
});
