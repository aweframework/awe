import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/services/criterion.js', function () {
  let $injector, $control, Criterion, $httpBackend;
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
      $control = $injector.get('Control');
      Criterion = $injector.get('Criterion');
      $httpBackend = $injector.get("$httpBackend");

      controller = {visible: true};
      model = {selected: "text", records: 14, model: [{value:"text", label:"Visible text"}]};

      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "getAddressModel").mockReturnValue(model);
      jest.spyOn($control, "getAddressController").mockReturnValue(controller);

      $httpBackend.when('POST', 'settings').respond(DefaultSettings);
    }]);


    jest.setTimeout(10000);
  });

  afterEach(function () {
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

  describe('validation reporting', function () {
    const VALIDITY = "criterion-validity";
    let emitted;
    let comp;

    beforeEach(function () {
      emitted = [];
      scope.$emit = jest.fn((event, data) => emitted.push({event, data}));
      controller.validationRules = {required: true};
      model.selected = "";
      comp = new Criterion(scope, "comp1", document.createElement("div"));
      comp.asCriterion();
    });

    function validityEvents() {
      return emitted.filter(item => item.event === VALIDITY).map(item => item.data);
    }

    it('reports that the criterion is invalid when its validation fails', function () {
      comp.api.validate(null, false);

      expect(validityEvents()).toEqual([{id: "comp1", invalid: true}]);
    });

    it('selects the container of the first invalid criterion only', function () {
      comp.api.validate(null, false);
      comp.api.validate(null, true);

      expect(emitted.filter(item => item.event === "select-current-tab")).toHaveLength(1);
    });

    it('reports an invalid criterion once, however many times it is validated', function () {
      comp.api.validate(null, false);
      comp.api.validate(null, true);

      expect(validityEvents()).toEqual([{id: "comp1", invalid: true}]);
    });

    it('reports that the criterion is valid again when its classes are updated', function () {
      comp.api.validate(null, false);

      comp.updateClasses();

      expect(validityEvents()).toEqual([{id: "comp1", invalid: true}, {id: "comp1", invalid: false}]);
    });

    it('reports that the criterion is valid again when a new validation passes', function () {
      comp.api.validate(null, false);
      model.selected = "filled";

      expect(comp.api.validate(null, false)).toBeNull();

      expect(validityEvents()).toEqual([{id: "comp1", invalid: true}, {id: "comp1", invalid: false}]);
    });

    it('does not report anything for a criterion that was always valid', function () {
      model.selected = "filled";

      comp.api.validate(null, false);
      comp.updateClasses();

      expect(validityEvents()).toEqual([]);
    });
  });
});
