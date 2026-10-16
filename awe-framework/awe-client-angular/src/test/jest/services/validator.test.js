import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe("Validator", () => {
  let $utilities;
  let $settings;
  let $validator;
  let $window;
  let originalScrollTo;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $utilities = $injector.get("AweUtilities");
      $settings = $injector.get("AweSettings");
      $validator = $injector.get("Validator");
      $window = $injector.get("$window");
      originalScrollTo = $window.scrollTo;
    }]);
  });

  afterEach(() => {
    if ($window) {
      $window.scrollTo = originalScrollTo;
    }
  });

  function showValidationError(validateTimeout) {
    const scope = {};
    $window.scrollTo = jest.fn();
    jest.spyOn($.fn, "offset").mockReturnValue({top: 0, left: 0});
    jest.spyOn($.fn, "height").mockReturnValue(0);
    jest.spyOn($settings, "get").mockReturnValue({validate: validateTimeout});
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => {
      fn();
      return "timer";
    });
    $utilities.timeout.cancel = jest.fn();

    $validator.showValidationError(scope, {id: "Field", message: "Required", element: $window});

    return scope;
  }

  it("shows a validation message and scrolls locally without a global scroll shim", () => {
    const scope = showValidationError(0);

    expect(scope.showValidation).toBe(true);
    expect(scope.validationMessage).toBe("Required");
    expect($window.scrollTo).toHaveBeenCalledWith(0, expect.any(Number));
  });

  it("schedules validation message hiding when a timeout is configured", () => {
    const scope = showValidationError(2000);

    expect(scope.showValidation).toBe(false);
    expect(scope.errorTimer).toBe("timer");
    expect($utilities.timeout).toHaveBeenCalledWith(expect.any(Function), 2000);
  });

  it("cancels an existing validation timer before showing a new validation error", () => {
    const scope = {errorTimer: "old-timer"};
    $window.scrollTo = jest.fn();
    jest.spyOn($.fn, "offset").mockReturnValue({top: 5, left: 0});
    jest.spyOn($.fn, "height").mockReturnValue(10);
    jest.spyOn($settings, "get").mockReturnValue({validate: 0});
    jest.spyOn($utilities, "timeout").mockImplementation(fn => fn());
    $utilities.timeout.cancel = jest.fn();

    $validator.showValidationError(scope, {id: "Field", message: "Invalid", element: $window});

    expect($utilities.timeout.cancel).toHaveBeenCalledWith("old-timer");
    expect(scope.validationMessage).toBe("Invalid");
  });

  describe("revealElement", () => {
    function criterion(html) {
      document.body.innerHTML = html;
      return document.body.firstElementChild;
    }

    afterEach(() => {
      document.body.innerHTML = "";
    });

    it("focuses the control of the criterion and scrolls it into view", () => {
      const element = criterion("<div><label>Name</label><input type='hidden'/><input id='name'/></div>");
      element.scrollIntoView = jest.fn();

      expect($validator.revealElement($(element))).toBe(true);

      expect(document.activeElement.id).toBe("name");
      expect(element.scrollIntoView).toHaveBeenCalledWith({block: "center", behavior: "smooth"});
    });

    it("focuses the control again when it cannot take the focus yet (its container is still opening)", () => {
      const element = criterion("<div><input id='name' disabled/></div>");
      element.scrollIntoView = jest.fn();
      const timeout = jest.spyOn($utilities, "timeout").mockImplementation(() => {});

      $validator.revealElement($(element));

      expect(document.activeElement.id).not.toBe("name");
      expect(timeout).toHaveBeenCalledWith(expect.any(Function), 100);

      element.querySelector("input").disabled = false;
      timeout.mock.calls[0][0]();

      expect(document.activeElement.id).toBe("name");
      timeout.mockRestore();
    });

    it("scrolls to a criterion without a control to focus", () => {
      const element = criterion("<div><span>read only</span></div>");
      element.scrollIntoView = jest.fn();

      expect($validator.revealElement($(element))).toBe(true);

      expect(element.scrollIntoView).toHaveBeenCalled();
    });

    it("does nothing when the element cannot be scrolled into view", () => {
      expect($validator.revealElement($($window))).toBe(false);
    });
  });

  it("reveals the element of the error before showing the message", () => {
    const element = document.createElement("div");
    element.scrollIntoView = jest.fn();
    const scope = {};
    jest.spyOn($.fn, "offset").mockReturnValue({top: 0, left: 0});
    jest.spyOn($.fn, "height").mockReturnValue(0);
    jest.spyOn($settings, "get").mockReturnValue({validate: 0});
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => fn());
    $utilities.timeout.cancel = jest.fn();

    $validator.showValidationError(scope, {id: "Field", message: "Required", element});

    expect(element.scrollIntoView).toHaveBeenCalled();
    expect(scope.showValidation).toBe(true);
  });
  describe("deferred containers that were never opened (#824)", () => {
    let $storage;
    let controllers;

    beforeEach(() => {
      inject(["Storage", (Storage) => {
        $storage = Storage;
      }]);
      controllers = {Dia: {deferred: true, deferredLoaded: false}, Plain: {}};
      $storage.put("controller", {report: controllers});
      document.body.innerHTML = `<div id="Dia"><span id="inDia"></span></div><div id="Plain"><span id="inPlain"></span></div>
        <div id="Page"><div id="Dia2"><span id="inDia2"></span></div></div><span id="outside"></span>`;
    });

    afterEach(() => {
      document.body.innerHTML = "";
    });

    function api(id, error = {message: "required"}) {
      return {
        address: {view: "report", component: id},
        element: document.getElementById(id),
        validate: jest.fn(() => error)
      };
    }

    function validate(components, baseNode) {
      $storage.put("api", {report: Object.fromEntries(components.map(c => [c.address.component, c]))});
      return $validator.validateNode(baseNode || $(document.body));
    }

    it("ignores the criteria of a deferred dialog that was not opened", () => {
      const inDia = api("inDia");
      const outside = api("outside");

      const errors = validate([inDia, outside]);

      expect(inDia.validate).not.toHaveBeenCalled();
      expect(errors).toHaveLength(1);
    });

    it("validates the criteria of a deferred dialog once it was opened", () => {
      controllers.Dia.deferredLoaded = true;
      const inDia = api("inDia");

      expect(validate([inDia])).toHaveLength(1);
      expect(inDia.validate).toHaveBeenCalled();
    });

    it("keeps validating the criteria of a dialog which is not deferred", () => {
      const inPlain = api("inPlain");

      expect(validate([inPlain])).toHaveLength(1);
    });

    it("passes a targeted validation of an unopened deferred container", () => {
      expect(validate([api("inDia")], $("#Dia"))).toEqual([]);
    });

    it("skips the unopened deferred part of a targeted validation of its parent", () => {
      controllers.Dia2 = {deferred: true, deferredLoaded: false};
      const errors = validate([api("inDia2"), api("inPlain")], $(document.body));

      expect(errors).toHaveLength(1);
      expect(validate([api("inDia2")], $("#Page"))).toEqual([]);
    });

    it("validates a targeted container once it was opened", () => {
      controllers.Dia.deferredLoaded = true;

      expect(validate([api("inDia")], $("#Dia"))).toHaveLength(1);
    });
  });
});
