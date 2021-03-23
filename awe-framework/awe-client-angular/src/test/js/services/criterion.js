import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/services/criterion.js', function () {
  let $rootScope, $injector, $control, $utilities, Criterion, $httpBackend;
  let originalTimeout;
  let address = {"component": "comp1", "view": "report"};
  let scopedFunctions = {};
  let scope = {view: "report", $parent: {$parent: {}}, $on: (k, fn) => scopedFunctions[k] = fn, $emit: () => null};
  let controller;
  let model;
  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $control = $injector.get('Control');
      $utilities = $injector.get('AweUtilities');
      Criterion = $injector.get('Criterion');
      $httpBackend = $injector.get("$httpBackend");

      controller = {visible: true};
      model = {selected: "text", records: 14, model: [{value:"text", label:"Visible text"}]};

      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "getAddressModel").and.returnValue(model);
      spyOn($control, "getAddressController").and.returnValue(controller);

      $httpBackend.when('POST', 'settings').respond(DefaultSettings);
    }]);


    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function () {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should generate a criterion', function () {
    // Prepare
    let comp = new Criterion(scope, "comp1", "element1");

    // Assert
    expect(comp.id).toEqual("comp1");
  });

  it('should initialize a criterion', function () {
    // Prepare
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.address).toEqual(address);
  });

  it('should check criterion is visible', function () {
    // Prepare
    controller["invisible"] = false;
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.attributeMethods.visible(comp)).toBe(true);
  });

  it('should check criterion is not visible', function () {
    // Prepare
    controller["invisible"] = true;
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.attributeMethods.visible(comp)).toBe(false);
  });

  it('should check criterion max elements per page', function () {
    // Prepare
    controller["invisible"] = true;
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.getMax()).toBe(100);
  });

  it('should get controller criterion max elements per page', function () {
    // Prepare
    controller["max"] = 50;
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.getMax()).toBe(50);
  });

  it('should get data from criterion', function () {
    // Prepare
    let comp = new Criterion(scope, "comp1", "element1");
    comp.asCriterion();

    // Assert
    expect(comp.getData()).toEqual({ "comp1": "text" });
  });
});