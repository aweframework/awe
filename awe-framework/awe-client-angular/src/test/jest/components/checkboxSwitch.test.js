import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// A checkbox criterion with style="switch" (#793 S19) is drawn as a switch, as the React engine does: the same native
// checkbox (Space toggles it, the value is still 1 when on and 0 when off) with role="switch", a track and its label
// beside it. Without the style the checkbox does not change
describe("checkbox criterion as a switch (#793 S19)", () => {
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
        $storage.put("status", {base: "loaded"});
      }]);
  });

  const render = ({style = "col-xs-12 switch", readonly = false, selected = ["1"]} = {}) => {
    const address = {view: "base", component: "Sta"};
    $control.setAddressController(address, {
      component: "checkbox", contextMenu: [], dependencies: [], id: "Sta", label: "Active", optional: false, readonly,
      required: false, size: "md", style, visible: true, validation: ""
    });
    $control.setAddressModel(address, {
      defaultValues: ["1"], page: 1, records: 0, selected, total: 0, values: [{value: "1", label: "1"}]
    });
    const element = $compile("<awe-input-checkbox input-checkbox-id='Sta'></awe-input-checkbox>")($rootScope);
    $rootScope.$digest();
    return {element: element[0], address};
  };

  const input = element => element.querySelector("input#Sta");
  const selected = address => String($control.getAddressModel(address).selected);

  it("draws a switch: the checkbox with role switch, its track and the label beside it", () => {
    const {element} = render();

    expect(input(element).getAttribute("type")).toBe("checkbox");
    expect(input(element).getAttribute("role")).toBe("switch");
    expect(input(element).getAttribute("aria-checked")).toBe("true");
    expect(input(element).checked).toBe(true);
    expect(input(element).closest("label").classList.contains("switch")).toBe(true);
    const track = input(element).nextElementSibling;
    expect(track.classList.contains("switch__track")).toBe(true);
    expect(track.getAttribute("aria-hidden")).toBe("true");
    expect(track.nextElementSibling.classList.contains("lbl")).toBe(true);
    expect(track.nextElementSibling.textContent.trim()).toBe("Active");
  });

  it("sends 1 when on and 0 when off, toggled as a checkbox (Space and click)", () => {
    const {element, address} = render();

    input(element).click();
    $rootScope.$digest();

    expect(selected(address)).toBe("0");
    expect(input(element).checked).toBe(false);
    expect(input(element).getAttribute("aria-checked")).toBe("false");

    input(element).click();
    $rootScope.$digest();

    expect(selected(address)).toBe("1");
    expect(input(element).getAttribute("aria-checked")).toBe("true");
  });

  it("follows the value set from outside: on for 1, off for 0", () => {
    const {element, address} = render({selected: ["0"]});

    expect(input(element).checked).toBe(false);
    expect(input(element).getAttribute("aria-checked")).toBe("false");

    $control.changeModelAttribute(address, {selected: "1"});
    $rootScope.$digest();
    expect(input(element).checked).toBe(true);
    expect(input(element).getAttribute("aria-checked")).toBe("true");

    $control.changeModelAttribute(address, {selected: 0});
    $rootScope.$digest();
    expect(input(element).checked).toBe(false);
    expect(input(element).getAttribute("aria-checked")).toBe("false");
  });

  it("is disabled while the criterion is readonly", () => {
    const {element, address} = render({readonly: true});

    expect(input(element).disabled).toBe(true);

    $control.changeControllerAttribute(address, {readonly: false});
    $rootScope.$digest();

    expect(input(element).disabled).toBe(false);
  });

  it("keeps the plain checkbox without the switch style", () => {
    const {element} = render({style: "col-xs-12 switcher"});

    expect(input(element).getAttribute("role")).toBeNull();
    expect(input(element).getAttribute("aria-checked")).toBeNull();
    expect(element.querySelector(".switch__track")).toBeNull();
    expect(input(element).nextElementSibling.classList.contains("lbl")).toBe(true);
    expect(input(element).closest("label").classList.contains("switch")).toBe(false);
  });
});
