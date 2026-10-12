import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("Panel", () => {
  let $control;
  let $httpBackend;
  let Panel;
  let scopedFunctions;
  let scope;
  let panelController;

  const VALIDITY = "criterion-validity";
  const SELECT_TAB = "select-current-tab";

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $control = $injector.get("Control");
      $httpBackend = $injector.get("$httpBackend");
      Panel = $injector.get("Panel");

      scopedFunctions = {};
      scope = {
        view: "report",
        $parent: {$parent: {}},
        $on: jest.fn((key, fn) => {
          scopedFunctions[key] = fn;
          return jest.fn();
        }),
        $emit: jest.fn()
      };
      panelController = {isActive: jest.fn(), selectTab: jest.fn(), setInvalid: jest.fn()};

      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue({selected: null, values: []});
      jest.spyOn($control, "getAddressController").mockReturnValue({visible: true});
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  it("selects its tab when a field inside it has to be revealed", () => {
    Panel.init(panelController, scope, "tab1");

    scopedFunctions[SELECT_TAB]();

    expect(panelController.selectTab).toHaveBeenCalledWith("tab1");
  });

  it("marks its tab as invalid while some field inside it is invalid", () => {
    Panel.init(panelController, scope, "tab1");

    scopedFunctions[VALIDITY]({}, {id: "a", invalid: true});
    scopedFunctions[VALIDITY]({}, {id: "b", invalid: true});
    scopedFunctions[VALIDITY]({}, {id: "a", invalid: false});
    expect(panelController.setInvalid).toHaveBeenLastCalledWith("tab1", true);

    scopedFunctions[VALIDITY]({}, {id: "b", invalid: false});
    expect(panelController.setInvalid).toHaveBeenLastCalledWith("tab1", false);
  });

  it("does not notify when the state of its tab does not change", () => {
    Panel.init(panelController, scope, "tab1");

    scopedFunctions[VALIDITY]({}, {id: "a", invalid: false});
    scopedFunctions[VALIDITY]({}, {id: "a", invalid: true});
    scopedFunctions[VALIDITY]({}, {id: "b", invalid: true});

    expect(panelController.setInvalid).toHaveBeenCalledTimes(1);
  });

  it("does not track invalid fields for panel controllers that cannot mark them", () => {
    delete panelController.setInvalid;
    Panel.init(panelController, scope, "tab1");

    expect(scopedFunctions[VALIDITY]).toBeUndefined();
  });

  it("tracks invalid fields for any scope and returns the listener to release", () => {
    const onChange = jest.fn();
    const release = Panel.trackInvalid(scope, onChange);

    scopedFunctions[VALIDITY]({}, {id: "a", invalid: true});

    expect(onChange).toHaveBeenCalledWith(true);
    expect(typeof release).toBe("function");
  });

  it("opens a collapsed item when a field inside it has to be revealed", () => {
    const open = jest.fn();
    Panel.revealOn(scope, () => false, open);

    scopedFunctions[SELECT_TAB]();

    expect(open).toHaveBeenCalledTimes(1);
  });

  it("keeps an open item as it is when a field inside it has to be revealed", () => {
    const open = jest.fn();
    Panel.revealOn(scope, () => true, open);

    scopedFunctions[SELECT_TAB]();

    expect(open).not.toHaveBeenCalled();
  });
});
