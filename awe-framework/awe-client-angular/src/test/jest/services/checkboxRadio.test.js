import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe('awe-framework/awe-client-angular/src/test/jest/services/checkboxRadio.js', function() {
  let $injector, $utilities, $control, $rootScope, $httpBackend, $criterion, $checkboxRadio, $storage;
  let controller = {};
  let model = {};

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $utilities = $injector.get('AweUtilities');
      $criterion = $injector.get('Criterion');
      $control = $injector.get('Control');
      $checkboxRadio = $injector.get('CheckboxRadio');
      $httpBackend = $injector.get('$httpBackend');
      $storage = $injector.get('Storage');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  it('should init as checkbox', function() {
    let $scope = $rootScope.$new();
    let selector = new $checkboxRadio($scope, "tutu", {});
    jest.spyOn(selector, "asCheckbox").mockReturnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asCheckbox()).toBe(true);
  });

  it('should init as radio', function() {
    let $scope = $rootScope.$new();
    let selector = new $checkboxRadio($scope, "tutu", {});
    jest.spyOn(selector, "asRadio").mockReturnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asRadio()).toBe(true);
  });

  describe('once initialized as checkbox', function() {
    let checkboxSelector;

    // Mock module
    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      checkboxSelector = new $checkboxRadio($scope, "tutu", {});
      jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      checkboxSelector.asCheckbox();
    });

    // Reset checkbox
    it('should reset checkbox', function() {
      jest.spyOn($control, "publishModelChanged").mockReturnValue(true);
      jest.spyOn($storage, "get").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue({});
      // Reset checkbox component
      checkboxSelector.onReset();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(0);
    });

    // Reset checkbox
    it('should restore checkbox with default value', function() {
      jest.spyOn($control, "publishModelChanged").mockReturnValue(true);
      jest.spyOn($storage, "get").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue({defaultValues: "1"});
      checkboxSelector.model.defaultValues = "1";

      // Reset checkbox component
      checkboxSelector.onRestore();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(1);
    });

    // Reset checkbox
    it('should restore checkbox with 0', function() {
      jest.spyOn($control, "publishModelChanged").mockReturnValue(true);
      jest.spyOn($storage, "get").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue({});

      // Reset checkbox component
      checkboxSelector.onRestore();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(0);
    });
  });

  describe('once initialized as radio', function() {
    it('should initialize safely when values are empty', function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      let radioSelector = new $checkboxRadio($scope, "tutu", {});

      jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu", group: "grp"});
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressViewModel").mockReturnValue({});
      jest.spyOn($control, "getAddressViewApi").mockReturnValue({});
      jest.spyOn($control, "publishModelChanged").mockReturnValue(true);

      radioSelector.model = {selected: null, values: []};
      radioSelector.controller = {group: "grp"};

      expect(function() {
        radioSelector.asRadio();
      }).not.toThrow();

      expect(radioSelector.value).toBeNull();
    });
  });
});
