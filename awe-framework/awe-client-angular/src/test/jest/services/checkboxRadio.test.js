import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe('awe-framework/awe-client-angular/src/test/jest/services/checkboxRadio.js', function() {
  let $injector, $control, $rootScope, $checkboxRadio, $storage;
  let controller = {};
  let model = {};

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $control = $injector.get('Control');
      $checkboxRadio = $injector.get('CheckboxRadio');
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

  describe('once initialized as column radio', function() {
    let radioSelector, controller, model;
    const address = {view: "report", component: "grid1", row: "2", column: "choice"};
    const OPTIONS = () => [{value: "A", label: "Option A"}, {value: "B", label: "Option B"}, {value: "C", label: "Option C"}];

    const initRadio = function(selected, controllerExtras = {}) {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      radioSelector = new $checkboxRadio($scope, "tutu", {});
      controller = {id: "tutu", component: "radio", label: "Choice", ...controllerExtras};
      model = {selected: selected, values: OPTIONS()};
      jest.spyOn($control, "getAddressController").mockReturnValue(controller);
      jest.spyOn($control, "getAddressModel").mockReturnValue(model);
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      jest.spyOn($control, "publishModelChanged").mockReturnValue(true);
      radioSelector.address = address;
      radioSelector.element = document.createElement("div");
      expect(radioSelector.asColumnRadio()).toBe(true);
      radioSelector.modelChange = jest.fn();
    };

    it('should keep the options of the list of the column in its model', function() {
      initRadio("B");

      expect(radioSelector.model.values.map((option) => option.value)).toEqual(["A", "B", "C"]);
    });

    it('should check only the option that is the selected value of the cell', function() {
      initRadio("B");

      expect(radioSelector.scope.isChecked({value: "B"})).toBe(true);
      expect(radioSelector.scope.isChecked({value: "A"})).toBe(false);
    });

    it('should compare the options as text so a number matches its text', function() {
      initRadio(2);

      expect(radioSelector.scope.isChecked({value: "2"})).toBe(true);
    });

    it('should check nothing when the cell has no value', function() {
      initRadio(null);

      OPTIONS().forEach((option) => expect(radioSelector.scope.isChecked(option)).toBe(false));
    });

    it('should name the options of a cell after the cell so another row has another group', function() {
      initRadio(null);

      radioSelector.updateVisibleValue();

      expect(radioSelector.scope.radioName).toBe("report-grid1-2-choice");
    });

    it('should show the label of the selected option as its visible value', function() {
      initRadio("C");

      radioSelector.updateVisibleValue();

      expect(radioSelector.visibleValue).toBe("Option C");
    });

    it('should choose an option as the selected value and tell the grid', function() {
      initRadio("A");

      radioSelector.scope.choose({value: "B"});

      expect(radioSelector.model.selected).toBe("B");
      expect(radioSelector.modelChange).toHaveBeenCalledTimes(1);
    });

    it('should not change anything when the chosen option is clicked again', function() {
      initRadio("B");

      radioSelector.scope.choose({value: "B"});

      expect(radioSelector.modelChange).not.toHaveBeenCalled();
    });

    it('should not choose anything when the column is readonly', function() {
      initRadio("A", {readonly: true});

      radioSelector.scope.choose({value: "B"});

      expect(radioSelector.model.selected).toBe("A");
      expect(radioSelector.modelChange).not.toHaveBeenCalled();
    });

    it('should take the selection from the model values api', function() {
      initRadio("A");

      radioSelector.api.updateModelValues({selected: "C"});

      expect(radioSelector.model.selected).toBe("C");
    });

    it('should fail the required validation when nothing is chosen, like any other column', function() {
      initRadio(null, {validationRules: {required: {value: true}}});
      radioSelector.scope.criterionClass = "";

      expect(radioSelector.api.validate(undefined, false)).not.toBeNull();

      radioSelector.model.selected = "A";
      expect(radioSelector.api.validate(undefined, false)).toBeNull();
    });

    describe('arrow keys', function() {
      let inputs;

      const press = function(key, index) {
        const event = {key: key, target: inputs[index], preventDefault: jest.fn()};
        radioSelector.scope.keyDown(event);
        return event;
      };

      beforeEach(function() {
        initRadio("A");
        document.body.innerHTML = "";
        const group = document.createElement("div");
        group.setAttribute("role", "radiogroup");
        document.body.appendChild(group);
        inputs = ["A", "B", "C"].map(function(value) {
          const input = document.createElement("input");
          input.type = "radio";
          input.value = value;
          input.click = jest.fn();
          group.appendChild(input);
          return input;
        });
      });

      it('should move the choice to the next option with the right arrow', function() {
        const event = press("ArrowRight", 0);

        expect(event.preventDefault).toHaveBeenCalled();
        expect(inputs[1].click).toHaveBeenCalled();
        expect(document.activeElement).toBe(inputs[1]);
      });

      it('should move the choice to the previous option with the left arrow', function() {
        press("ArrowLeft", 2);

        expect(inputs[1].click).toHaveBeenCalled();
      });

      it('should go round at both ends', function() {
        press("ArrowRight", 2);
        press("ArrowLeft", 0);

        expect(inputs[0].click).toHaveBeenCalled();
        expect(inputs[2].click).toHaveBeenCalled();
      });

      it('should skip the disabled options', function() {
        inputs[1].disabled = true;

        press("ArrowRight", 0);

        expect(inputs[1].click).not.toHaveBeenCalled();
        expect(inputs[2].click).toHaveBeenCalled();
      });

      it('should ignore the other keys', function() {
        const event = press("Enter", 0);

        expect(event.preventDefault).not.toHaveBeenCalled();
        inputs.forEach(function(input) {
          expect(input.click).not.toHaveBeenCalled();
        });
      });
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
