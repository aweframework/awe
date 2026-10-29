import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// The show / hide button of every password criterion (#793 S17): an icon-only button at the end of the field that
// changes the type of the input between password and text. The value, the model and the validation do not change
describe("password criterion show / hide button (#793 S17)", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $control;
  let $storage;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage",
      (_$rootScope_, _$compile_, _$httpBackend_, _Control_, _Storage_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = _Storage_;
        $rootScope.view = "base";
        $rootScope.context = "screen";
        $rootScope.firstLoad = true;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
        $httpBackend.when("POST", "./settings").respond(DefaultSettings);
        $storage.put("controller", {base: {}});
        $storage.put("model", {base: {}});
        $storage.put("api", {base: {}});
      }]);
  });

  const render = ({readonly = false, values = [{value: "secret", label: "secret", selected: true}]} = {}) => {
    const address = {view: "base", component: "Pas"};
    $control.setAddressController(address, {
      component: "password", contextMenu: [], dependencies: [], id: "Pas", optional: false, readonly, required: true,
      size: "md", visible: true, validation: ""
    });
    $control.setAddressModel(address, {defaultValues: [], page: 1, records: 0, selected: "secret", total: 0, values});
    const element = $compile("<awe-input-password input-password-id='Pas'></awe-input-password>")($rootScope);
    $rootScope.$digest();
    return {element: element[0], address};
  };

  const input = element => element.querySelector("input#Pas");
  const toggle = element => element.querySelector("button.password-toggle");
  const iconName = element => toggle(element).querySelector("use").getAttribute("href").split("#")[1];

  it("draws the eye button right after the input, controlling it", () => {
    const {element} = render();

    expect(element.querySelectorAll("button.password-toggle")).toHaveLength(1);
    expect(input(element).nextElementSibling).toBe(toggle(element));
    expect(toggle(element).getAttribute("type")).toBe("button");
    expect(toggle(element).getAttribute("aria-controls")).toBe("Pas");
    expect(toggle(element).getAttribute("aria-pressed")).toBe("false");
    expect(toggle(element).getAttribute("aria-label")).toBe("SCREEN_TEXT_SHOW_PASSWORD");
    expect(iconName(element)).toBe("eye");
    expect(toggle(element).disabled).toBe(false);
    expect(input(element).getAttribute("type")).toBe("password");
  });

  it("shows and hides the password keeping its value and its model", () => {
    const {element, address} = render();
    input(element).value = "typed";
    const model = JSON.stringify($control.getAddressModel(address));

    toggle(element).click();

    expect(input(element).type).toBe("text");
    expect(input(element).value).toBe("typed");
    expect(toggle(element).getAttribute("aria-pressed")).toBe("true");
    expect(toggle(element).getAttribute("aria-label")).toBe("SCREEN_TEXT_HIDE_PASSWORD");
    expect(iconName(element)).toBe("eye-off");

    toggle(element).click();

    expect(input(element).type).toBe("password");
    expect(input(element).value).toBe("typed");
    expect(toggle(element).getAttribute("aria-pressed")).toBe("false");
    expect(toggle(element).getAttribute("aria-label")).toBe("SCREEN_TEXT_SHOW_PASSWORD");
    expect(iconName(element)).toBe("eye");
    expect(JSON.stringify($control.getAddressModel(address))).toBe(model);
  });

  it("is disabled while the criterion is readonly", () => {
    const {element, address} = render({readonly: true});

    expect(toggle(element).disabled).toBe(true);
    expect(input(element).disabled).toBe(true);

    $control.changeControllerAttribute(address, {readonly: false});
    $rootScope.$digest();

    expect(toggle(element).disabled).toBe(false);
  });

  it("keeps the Caps Lock icon of the criterion", () => {
    const {element} = render();

    expect(element.querySelectorAll(".form-control-feedback")).toHaveLength(1);
  });
});
