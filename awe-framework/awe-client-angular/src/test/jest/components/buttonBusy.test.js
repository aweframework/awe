// Busy state of the button that launched actions (#771): the spinner instead of its icon only when the actions are
// still running BUSY_THRESHOLD after the click
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {BUSY_THRESHOLD} from "../../../main/resources/js/awe/services/busyState";

describe("busy state of the button that launched actions", () => {
  let $compile, $rootScope, $timeout, Storage, controllers;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    controllers = {};
    inject(["$compile", "$rootScope", "$timeout", "$httpBackend", "Storage", "Control", "ActionController",
      (_$compile_, _$rootScope_, _$timeout_, $httpBackend, _Storage_, Control, ActionController) => {
        jest.spyOn(Control, "checkComponent").mockReturnValue(true);
        jest.spyOn(Control, "getAddressModel").mockReturnValue({selected: null, values: []});
        jest.spyOn(Control, "getAddressController").mockImplementation(address => controllers[address.component]);
        // Launching the actions of a button marks the actions as running, as the action controller does
        jest.spyOn(ActionController, "addActionList").mockImplementation(() => _Storage_.put("actions-running", true));
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $timeout = _$timeout_;
        Storage = _Storage_;
        $rootScope.status = {loading: false};
        $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
      }]);
    Storage.put("actions-running", false);
  });

  afterEach(() => {
    $("body").empty();
    jest.restoreAllMocks();
  });

  const compileButton = (id, extra = {}) => {
    controllers[id] = {id, component: "button", buttonType: "button", label: "BUTTON_" + id, visible: true,
      actions: [{type: "server"}], dependencies: [], contextMenu: [], ...extra};
    const scope = $rootScope.$new();
    const element = $compile(`<awe-button button-id='${id}'></awe-button>`)(scope);
    $("body").append(element);
    $rootScope.$digest();
    return {element, scope, button: element.find("button")};
  };

  const isBusy = button => button.hasClass("is-loading");
  const click = button => button.triggerHandler("click");
  const endActions = () => {
    Storage.put("actions-running", false);
    $rootScope.$digest();
  };

  it("shows the spinner instead of the icon only after the threshold while the actions run", () => {
    const {button} = compileButton("ButSave", {icon: "save"});
    click(button);

    $timeout.flush(BUSY_THRESHOLD - 1);
    expect(isBusy(button)).toBe(false);
    expect(button.attr("aria-busy")).toBeUndefined();
    expect(button.find(".button-busy").length).toBe(0);

    $timeout.flush(1);
    expect(isBusy(button)).toBe(true);
    expect(button.attr("aria-busy")).toBe("true");
    expect(button.prop("disabled")).toBe(true);
    const children = button.children(".button-icon");
    expect(children.first().hasClass("button-busy")).toBe(true);
    expect(children.first().find("svg.awe-icon.awe-icon-spin use").attr("href")).toMatch(/#loader-circle$/);
    expect(button.find(".button-text").length).toBe(1);
  });

  it("puts the spinner before the label when the button has no icon", () => {
    const {button} = compileButton("ButAccept");
    click(button);
    $timeout.flush(BUSY_THRESHOLD);

    expect(isBusy(button)).toBe(true);
    expect(button.children().first().hasClass("button-busy")).toBe(true);
    expect(button.children().first().next().hasClass("button-text")).toBe(true);
  });

  it("clears the busy state when the actions end", () => {
    const {button} = compileButton("ButSave", {icon: "save"});
    click(button);
    $timeout.flush(BUSY_THRESHOLD);
    expect(isBusy(button)).toBe(true);

    endActions();

    expect(isBusy(button)).toBe(false);
    expect(button.attr("aria-busy")).toBeUndefined();
    expect(button.find(".button-busy").length).toBe(0);
    expect(button.prop("disabled")).toBe(false);
  });

  it("changes nothing when the actions end before the threshold", () => {
    const {button} = compileButton("ButSave", {icon: "save"});
    click(button);
    endActions();

    $timeout.flush(BUSY_THRESHOLD);
    expect(isBusy(button)).toBe(false);
    expect(button.find(".button-busy").length).toBe(0);
  });

  it("does not show it when the button sets show-loading to false", () => {
    const {button} = compileButton("ButQuiet", {showLoading: false});
    click(button);
    $timeout.flush(BUSY_THRESHOLD);

    expect(isBusy(button)).toBe(false);
    expect(button.prop("disabled")).toBe(true);
  });

  it("shows it by default when the button has no show-loading value", () => {
    const {button} = compileButton("ButNull", {showLoading: null});
    click(button);
    $timeout.flush(BUSY_THRESHOLD);

    expect(isBusy(button)).toBe(true);
  });

  it("shows it only on the launching button; the others keep being disabled", () => {
    const launching = compileButton("ButSave", {icon: "save"}).button;
    const other = compileButton("ButOther", {icon: "trash"}).button;
    click(launching);
    $timeout.flush(BUSY_THRESHOLD);

    expect(isBusy(launching)).toBe(true);
    expect(isBusy(other)).toBe(false);
    expect(other.find(".button-busy").length).toBe(0);
    expect(other.prop("disabled")).toBe(true);
  });

  it("cancels the threshold when the button is destroyed", () => {
    const {button, element} = compileButton("ButSave", {icon: "save"});
    const buttonScope = element.isolateScope();
    click(button);
    buttonScope.$destroy();

    $timeout.flush(BUSY_THRESHOLD);
    expect(buttonScope.busy).toBe(false);
  });
});
