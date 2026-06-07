// Source-traceable Jest owner for the Karma validationRules service spec.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

require("../../js/services/validationRules.js");

describe("ValidationRules Jest logging contract", () => {
  let $rules;
  let $control;

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      $rules = $injector.get("ValidationRules");
      $control = $injector.get("Control");
    }]);
  });

  it("keeps maxRepeat validation deterministic without emitting empty console.info output", () => {
    jest.spyOn($control, "getAddressViewController").mockReturnValue({Grid: {}});
    jest.spyOn($control, "getAddressModel").mockImplementation(() => ({
      values: [
        {Col1: {value: 1.0}, Col2: {value: 10}},
        {Col1: 1.0, Col2: {value: 11}}
      ],
      selected: 1.0
    }));
    const infoSpy = jest.spyOn(console, "info").mockImplementation(() => {});

    try {
      const result = $rules.validate("maxRepeat", 1.0, 1, {view: "base", component: "Grid", column: "Col1", row: "1"});

      expect(result).toEqual({message: "VALIDATOR_MESSAGE_MAX_REPEAT"});
      expect(infoSpy).not.toHaveBeenCalled();
    } finally {
      infoSpy.mockRestore();
    }
  });
});
