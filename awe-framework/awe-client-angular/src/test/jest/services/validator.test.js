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
});
