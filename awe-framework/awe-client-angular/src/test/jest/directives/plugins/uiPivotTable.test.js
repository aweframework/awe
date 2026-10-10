import "../../components/lightComponentTestUtils";
import "../../../../main/resources/js/awe/directives/plugins/uiPivotTable";
import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

describe("uiPivotTable directive", () => {
  let $rootScope, $compile, $httpBackend, pivotUI;

  beforeAll(() => {
    require("../../../../main/resources/js/lib/pivotTable/pivot.js");
  });

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", function (_$rootScope_, _$compile_, _$httpBackend_) {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
    pivotUI = jest.spyOn($.fn, "pivotUI").mockImplementation(function () {
      return this;
    });
  });

  afterEach(() => {
    pivotUI.mockRestore();
  });

  function compilePivot(controller) {
    const scope = $rootScope.$new();
    scope.controller = controller;
    scope.datasource = [{a: 1, b: 2}];
    const element = $compile("<div ui-pivot-table></div>")(scope);
    scope.$digest();
    return element;
  }

  it("offers the custom aggregators, formatted with the configured separators, when an aggregator is set", () => {
    compilePivot({
      aggregator: "Custom 80% Upper Bound",
      aggregationField: "n",
      decimalNumbers: 2,
      thousandSeparator: ".",
      decimalSeparator: ","
    });

    expect(pivotUI).toHaveBeenCalled();
    const config = pivotUI.mock.calls[0][1];
    expect(config.aggregators["Custom 80% Upper Bound"]).toBeDefined();
    expect(config.aggregators["Custom 80% Lower Bound"]).toBeDefined();

    const instance = config.aggregators["Custom 80% Upper Bound"].fn(["n", "d"])({}, [], []);
    [{n: 10, d: 100}, {n: 20, d: 100}, {n: 30, d: 100}].forEach((record) => instance.push(record));
    expect(instance.format(1234.5)).toBe("1.234,50");
  });
});
