import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {register, unregister} from "awe-client-react/utilities/loginEffects";

// The canvas effect of the image panel (S23): settings.login.backgroundEffect names an effect the application
// registered, drawn inside the panel of the page (the panel of split, the backdrop of card; form-only has none). An
// unknown name draws nothing and leaves the image, and an effect registered later starts when it registers
const EFFECT = "brand-wave";
const panelMarkup = id => `<div id="${id}" class="fullScreen">
  <div class="overlay-image fullScreen"></div><div class="overlay fullScreen"></div>
</div>`;
const SIGNIN = `<div class="fullScreen">
  ${panelMarkup("page-signin-bg")}
  <div class="awe-signin signin-container">
    <div class="signin-info"></div>
    <div class="signin-form"><div class="signin-text"><span>SCREEN_TITLE_LOGIN</span></div><input id="cod_usr"></div>
  </div>
</div>`;
const CHANGE_PASSWORD = `<div class="fullScreen">
  ${panelMarkup("page-change-password-bg")}
  <div class="awe-change-password change-password-container">
    <div class="change-password-header"></div>
    <div class="change-password-form"><div class="change-password-text"><span>Change</span></div></div>
  </div>
</div>`;
const TWO_FACTOR = `<div class="totp-screen fullScreen">
  ${panelMarkup("page-totp-bg")}
  <div class="awe-totp totp-container">
    <div class="totp-header"></div>
    <div class="totp-form"><div class="totp-text"><span>Check</span></div></div>
  </div>
</div>`;

describe("awe-signin: canvas effect of the image panel (S23)", () => {
  let $compile, $rootScope, $settings, $log, effect, stop;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$compile", "$rootScope", "$httpBackend", "AweSettings", "$translate", "$log",
      (_$compile_, _$rootScope_, $httpBackend, AweSettings, $translate, _$log_) => {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $settings = AweSettings;
        $log = _$log_;
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
        jest.spyOn($translate, "instant").mockImplementation(key => key);
      }]);
    stop = jest.fn();
    effect = jest.fn(() => stop);
  });

  afterEach(() => {
    unregister(EFFECT);
    document.body.innerHTML = "";
  });

  /**
   * Render a login page with the given login settings
   * @param {object} login settings.login
   * @param {string} markup Screen markup
   * @returns {{page: HTMLElement, scope: object}} Page root and its scope
   */
  function render(login, markup = SIGNIN) {
    $settings.update({login: {poweredBy: true, panelTitle: "BRAND_PANEL", ...login}});
    const scope = $rootScope.$new();
    const element = $compile(angular.element("<div>").html(markup).contents())(scope);
    const page = element.filter((_index, node) => node.nodeType === 1)[0];
    document.body.appendChild(page);
    scope.$digest();
    return {page, scope};
  }

  it.each(["split", "card"])("draws the registered effect inside the panel of the %s layout", layout => {
    register(EFFECT, effect);
    const {page} = render({layout, backgroundEffect: EFFECT});
    const wrapper = page.querySelector("#page-signin-bg > .signin-effect");

    expect(page.getAttribute("data-awe-login-effect")).toBe(EFFECT);
    expect(page.getAttribute("data-awe-login-effect-running")).toBe("true");
    expect(wrapper.getAttribute("aria-hidden")).toBe("true");
    expect(wrapper.children).toHaveLength(1);
    const canvas = wrapper.querySelector(":scope > canvas");
    expect(effect).toHaveBeenCalledTimes(1);
    expect(effect).toHaveBeenCalledWith(canvas);
    // The caption is drawn after the effect, over it
    const caption = page.querySelector("#page-signin-bg > .signin-caption");
    expect(wrapper.compareDocumentPosition(caption) & window.Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("starts the effect when it is registered after the page is drawn", () => {
    const {page, scope} = render({layout: "split", backgroundEffect: EFFECT});
    expect(page.hasAttribute("data-awe-login-effect-running")).toBe(false);
    expect(page.querySelector(".signin-effect")).toBeNull();
    expect(effect).not.toHaveBeenCalled();

    register(EFFECT, effect);
    scope.$digest();

    expect(page.getAttribute("data-awe-login-effect-running")).toBe("true");
    expect(effect).toHaveBeenCalledWith(page.querySelector("#page-signin-bg > .signin-effect > canvas"));
  });

  it("stops the effect and stops waiting for it when the page is destroyed", () => {
    register(EFFECT, effect);
    const {page, scope} = render({layout: "split", backgroundEffect: EFFECT});
    expect(stop).not.toHaveBeenCalled();

    scope.$destroy();
    unregister(EFFECT);
    register(EFFECT, effect);

    expect(stop).toHaveBeenCalledTimes(1);
    expect(effect).toHaveBeenCalledTimes(1);
    expect(page.querySelector(".signin-effect")).toBeNull();
  });

  it("draws no effect in the form-only layout, which has no background", () => {
    register(EFFECT, effect);
    const {page} = render({layout: "form-only", backgroundEffect: EFFECT});

    expect(page.querySelector(".signin-effect")).toBeNull();
    expect(page.hasAttribute("data-awe-login-effect")).toBe(false);
    expect(effect).not.toHaveBeenCalled();
  });

  it.each([["no effect", undefined], ["an empty name", "  "]])("keeps the background image with %s", (_name, name) => {
    register(EFFECT, effect);
    const {page} = render({layout: "split", backgroundEffect: name});

    expect(page.querySelector(".signin-effect")).toBeNull();
    expect(page.hasAttribute("data-awe-login-effect")).toBe(false);
    expect(effect).not.toHaveBeenCalled();
  });

  it("keeps the background image with an unknown name: no wrapper covers the panel", () => {
    const {page} = render({layout: "split", backgroundEffect: "plasma-wave"});

    expect(page.getAttribute("data-awe-login-effect")).toBe("plasma-wave");
    expect(page.hasAttribute("data-awe-login-effect-running")).toBe(false);
    expect(page.querySelector(".signin-effect")).toBeNull();
  });

  it("keeps the page and the image when the effect throws", () => {
    const error = jest.spyOn($log, "error").mockImplementation(() => undefined);
    register(EFFECT, () => {
      throw new Error("no 2d context");
    });
    const {page} = render({layout: "split", backgroundEffect: EFFECT});

    expect(page.querySelector("#cod_usr")).not.toBeNull();
    expect(page.hasAttribute("data-awe-login-effect-running")).toBe(false);
    expect(page.querySelector(".signin-effect")).toBeNull();
    expect(error).toHaveBeenCalled();
    error.mockRestore();
  });

  it("starts an effect the application registers through the global aweLoginEffects, as an AngularJS app does", () => {
    window.aweLoginEffects.register(EFFECT, effect);
    const {page} = render({layout: "split", backgroundEffect: EFFECT});

    expect(page.getAttribute("data-awe-login-effect-running")).toBe("true");
    expect(effect).toHaveBeenCalledTimes(1);
  });

  it.each([
    ["change password", CHANGE_PASSWORD, "page-change-password-bg"],
    ["two-factor", TWO_FACTOR, "page-totp-bg"]
  ])("draws the effect inside the panel of the %s screens too (S19)", (_name, markup, panelId) => {
    register(EFFECT, effect);
    const {page} = render({layout: "card", backgroundEffect: EFFECT}, markup);

    expect(page.getAttribute("data-awe-login-effect")).toBe(EFFECT);
    expect(page.querySelector(`#${panelId} > .signin-effect > canvas`)).not.toBeNull();
    expect(effect).toHaveBeenCalledTimes(1);
  });
});
