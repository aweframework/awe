describe('awe-framework/awe-client-angular/src/test/js/services/checkboxRadio.js', function() {
  let $injector, $utilities, $control, $rootScope, $httpBackend, $criterion, $checkboxRadio, $storage;
  let originalTimeout;
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

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should init as checkbox', function() {
    let $scope = $rootScope.$new();
    let selector = new $checkboxRadio($scope, "tutu", {});
    spyOn(selector, "asCheckbox").and.returnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asCheckbox()).toBe(true);
  });

  it('should init as radio', function() {
    let $scope = $rootScope.$new();
    let selector = new $checkboxRadio($scope, "tutu", {});
    spyOn(selector, "asRadio").and.returnValue(true);
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
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      checkboxSelector.asCheckbox();
    });

    // Reset checkbox
    it('should reset checkbox', function() {
      spyOn($control, "publishModelChanged").and.returnValue(true);
      spyOn($storage, "get").and.returnValue(true);
      spyOn($control, "getAddressModel").and.returnValue({});
      // Reset checkbox component
      checkboxSelector.onReset();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(0);
    });

    // Reset checkbox
    it('should restore checkbox with default value', function() {
      spyOn($control, "publishModelChanged").and.returnValue(true);
      spyOn($storage, "get").and.returnValue(true);
      spyOn($control, "getAddressModel").and.returnValue({defaultValues: "1"});
      checkboxSelector.model.defaultValues = "1";

      // Reset checkbox component
      checkboxSelector.onRestore();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(1);
    });

    // Reset checkbox
    it('should restore checkbox with 0', function() {
      spyOn($control, "publishModelChanged").and.returnValue(true);
      spyOn($storage, "get").and.returnValue(true);
      spyOn($control, "getAddressModel").and.returnValue({});

      // Reset checkbox component
      checkboxSelector.onRestore();

      // Filter date with date list
      expect(checkboxSelector.model.selected).toBe(0);
    });
  });
});