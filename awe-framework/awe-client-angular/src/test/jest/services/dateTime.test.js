import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/services/dateTime.js', function() {
  let $injector, $utilities, $settings, $control, $rootScope, $translate, $httpBackend, $log, $actionController, $criterion, $dateTime;
  let controller = {};
  let model = {};

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $utilities = $injector.get('AweUtilities');
      $settings = $injector.get('AweSettings');
      $criterion = $injector.get('Criterion');
      $translate = $injector.get('$translate');
      $httpBackend = $injector.get('$httpBackend');
      $log = $injector.get('$log');
      $actionController = $injector.get('ActionController');
      $control = $injector.get('Control');
      $dateTime = $injector.get('DateTime');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  it('should init as date', function() {
    let $scope = $rootScope.$new();
    let selector = new $dateTime($scope, "tutu", {});
    jest.spyOn(selector, "asCriterion").mockReturnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asDate()).toBe(true);
  });

  it('should init as filtered date', function() {
    let currentModel = {values:[], selected: "lala"};
    let $scope = $rootScope.$new();
    let selector = new $dateTime($scope, "tutu", {});
    jest.spyOn(selector, "asCriterion").mockReturnValue(true);
    selector.api = {};
    selector.model = currentModel;
    selector.controller = controller;
    expect(selector.asFilteredDate()).toBe(true);
  });

  it('should init as time', function() {
    let $scope = $rootScope.$new();
    let selector = new $dateTime($scope, "tutu", {});
    jest.spyOn(selector, "asCriterion").mockReturnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asTime()).toBe(true);
  });

  describe('once initialized', function() {
    let dateSelector;
    let currentModel;

    // Mock module
    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      dateSelector = new $dateTime($scope, "tutu", {});
      currentModel = {values: [], selected: []};
      jest.spyOn($control, "getAddressModel").mockReturnValue(currentModel);
      jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      dateSelector.asFilteredDate();
    });

    // As select
    it('should filter allowed dates, months and years from values objects', function() {
      currentModel.values = [
        {value: '23/10/2018', label: '23/10/2018'},
        {value: '05/11/2019', label: '05/11/2019'}
      ];

      dateSelector.onModelChanged();

      expect(dateSelector.scope.aweDateOptions.beforeShowDay(new Date(2018, 9, 23))).toBe(true);
      expect(dateSelector.scope.aweDateOptions.beforeShowDay(new Date(2018, 9, 24))).toBe(false);
      expect(dateSelector.scope.aweDateOptions.beforeShowMonth(new Date(2018, 9, 1))).toBe(true);
      expect(dateSelector.scope.aweDateOptions.beforeShowMonth(new Date(2018, 8, 1))).toBe(false);
      expect(dateSelector.scope.aweDateOptions.beforeShowYear(new Date(2018, 0, 1))).toBe(true);
      expect(dateSelector.scope.aweDateOptions.beforeShowYear(new Date(2020, 0, 1))).toBe(false);
    });

    it('should ignore malformed filtered date values without crashing', function() {
      currentModel.values = ['23/10/2018', null, {label: 'missing value'}];

      expect(function() {
        dateSelector.onModelChanged();
      }).not.toThrow();

      expect(dateSelector.scope.aweDateOptions.beforeShowDay(new Date(2018, 9, 23))).toBe(false);
    });
  });
});
