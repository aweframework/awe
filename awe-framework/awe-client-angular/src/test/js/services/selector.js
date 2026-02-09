describe('awe-framework/awe-client-angular/src/test/js/services/selector.js', function() {
  let $injector, $utilities, $settings, $control, $rootScope, $translate, $httpBackend, $log, $actionController, $criterion, $selector;
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
      $settings = $injector.get('AweSettings');
      $criterion = $injector.get('Criterion');
      $translate = $injector.get('$translate');
      $httpBackend = $injector.get('$httpBackend');
      $log = $injector.get('$log');
      $actionController = $injector.get('ActionController');
      $control = $injector.get('Control');
      $selector = $injector.get('Selector');
    }]);

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should init as select', function() {
    let $scope = $rootScope.$new();
    let selector = new $selector($scope, "tutu", {});
    spyOn(selector, "asCriterion").and.returnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asSelect()).toBe(true);
  });

  it('should init as suggest', function() {
    let currentModel = {values:[], selected: "lala"};
    let $scope = $rootScope.$new();
    let selector = new $selector($scope, "tutu", {});
    spyOn($control, "getAddressModel").and.returnValue(currentModel);
    spyOn($actionController, "generateAction").and.returnValue({});
    spyOn($actionController, "addActionList").and.returnValue(null);
    spyOn(selector, "asCriterion").and.returnValue(true);
    spyOn(selector, "reload").and.returnValue(true);
    selector.api = {};
    selector.model = currentModel;
    selector.controller = controller;
    expect(selector.asSuggest("lala")).toBe(true);
  });

  it('should init as select multiple', function() {
    let $scope = $rootScope.$new();
    let selector = new $selector($scope, "tutu", {});
    spyOn(selector, "asCriterion").and.returnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asSelectMultiple()).toBe(true);
  });

  it('should init as suggest multiple', function() {
    let $scope = $rootScope.$new();
    let selector = new $selector($scope, "tutu", {});
    spyOn(selector, "asCriterion").and.returnValue(true);
    selector.api = {};
    selector.model = model;
    selector.controller = controller;
    expect(selector.asSuggestMultiple()).toBe(true);
  });

  describe('once initialized as select', function() {
    let select;

    // Mock module
    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      select = new $selector($scope, "tutu", {});
      spyOn($control, "getAddressModel").and.returnValue({values: [], selected: []});
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      select.asSelect();
    });

    // As select
    it('should update model values', function() {
      // Define values to update
      let data = {values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};
      let data2 = {values: [{value: "A", label: "tutu"}, {value: "B", label: "lala"}, {value: "C", label: "lolo"}], selected: ["B"]};
      let data3 = {selected: [0]};

      // Update model values
      select.api.updateModelValues(data);

      // Check values updated
      expect(select.model.values).toEqual(data.values);

      // Update model values
      select.api.updateModelValues(data2);

      // Check values updated
      expect(select.model.values).toEqual(data2.values);

      // Update model values
      select.api.updateModelValues(data);

      // Check values updated
      expect(select.model.values).toEqual(data.values);

      // Update selected values
      select.api.updateModelValues(data3);

      // Check selected updated
      expect(select.model.selected).toEqual("0");
    });
  });

  describe('once initialized as select check change process', function() {
    let select;

    // Mock module
    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      select = new $selector($scope, "tutu", {});
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      select.asSelect();
    });

    it('should process change event setting selected to null when value is empty', function() {
      // Arrange a specific model reference so we can observe changes
      let modelRef = {values: [], selected: 'initial'};
      spyOn($control, "getAddressModel").and.returnValue(modelRef);
      // Ensure modelChange is called
      select.modelChange = jasmine.createSpy('modelChange');
      // Act: trigger plugin change with empty value
      select.onPluginChange({val: ''});
      // Assert
      expect(modelRef.selected).toBeNull();
      expect(select.modelChange).toHaveBeenCalled();
    });

    it('should process change event setting selected to provided value when not empty', function() {
      // Arrange
      let modelRef = {values: [], selected: null};
      spyOn($control, "getAddressModel").and.returnValue(modelRef);
      select.modelChange = jasmine.createSpy('modelChange');
      // Act
      select.onPluginChange({val: 'ABC'});
      // Assert
      expect(modelRef.selected).toBe('ABC');
      expect(select.modelChange).toHaveBeenCalled();
    });
  });

  describe('once initialized as select for value helpers', function() {
    let select;

    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      select = new $selector($scope, "tutu", {});
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      spyOn($control, "getAddressModel").and.returnValue({values: [], selected: null});
      select.asSelect();
    });

    it('should fallback to first value when selected is not in values', function() {
      let modelRef = {
        values: [{value: "A", label: "Alpha"}, {value: "B", label: "Beta"}],
        selected: "C"
      };
      $control.getAddressModel.and.returnValue(modelRef);
      select.onStart();

      expect(modelRef.selected).toBe("A");
      expect(modelRef["initial-selected"]).toBe("A");
    });

    it('should keep selected when it is in values', function() {
      let modelRef = {
        values: [{value: 1, label: "One"}, {value: 2, label: "Two"}],
        selected: "2"
      };
      $control.getAddressModel.and.returnValue(modelRef);

      select.onStart();

      expect(modelRef.selected).toBe("2");
      expect(modelRef["initial-selected"]).toBeUndefined();
    });

    it('should translate and join selected labels for visible value', function() {
      let modelRef = {
        values: [{value: 1, label: "One"}, {value: "2", label: "Two"}],
        selected: [1, "2"]
      };
      spyOn($translate, "instant").and.callFake(text => `T-${text}`);
      $control.getAddressModel.and.returnValue(modelRef);

      expect(select.getVisibleValue()).toBe("T-One, T-Two");
    });

    it('should filter select data for initSelection', function() {
      let modelRef = {
        values: [{value: 1, label: "One"}, {value: "2", label: "Two"}, {value: 3, label: "Three"}],
        selected: [2, 4]
      };
      spyOn($translate, "instant").and.callFake(text => `T-${text}`);
      $control.getAddressModel.and.returnValue(modelRef);
      let callback = jasmine.createSpy('callback');

      select.scope.aweSelectOptions.initSelection(null, callback);

      expect(callback).toHaveBeenCalledWith({id: "2", text: "T-Two"});
    });
  });

  describe('once initialized as suggest', function () {
    let suggest;

    // Mock module
    beforeEach(function () {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      suggest = new $selector($scope, "tutu", {});
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      suggest.asSuggest();
    });

    it('should update model values', function () {
      // Define values to update
      suggest.model = {storedValues: [], values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};

      // Update model values
      spyOn($control, "getAddressModel").and.returnValue(suggest.model);
      suggest.onModelChangedValues();

      // Check values updated
      expect(suggest.model.values).toEqual([{value: 1, label: "Yes"}]);
    });

    it('should update model values with stored values', function () {
      // Define values to update
      suggest.model = {storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], values: [], selected: []};

      // Update model values
      spyOn($control, "getAddressModel").and.returnValue(suggest.model);
      suggest.onModelChangedValues();

      // Check values updated
      expect(suggest.model.values).toEqual([]);
    });

    it('should update model values with stored values and duplicates', function () {
      // Define values to update
      suggest.model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };

      // Update model values
      spyOn($control, "getAddressModel").and.returnValue(suggest.model);
      suggest.onModelChangedValues();

      // Check values updated
      expect(suggest.model.values).toEqual([{value: 2, label: "Other"}]);
    });

    it('should filter on model changed selected with existing value', function () {
      // Define values to update
      suggest.model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };

      // Update model values
      spyOn($control, "getAddressModel").and.returnValue(suggest.model);
      suggest.onModelChangedSelected();

      // Check values updated
      expect(suggest.model.values).toEqual([{value: 2, label: "Other"}]);
    });

    it('should filter on model changed selected with another value', function () {
      // Define values to update
      suggest.model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [3]
      };

      // Update model values
      spyOn($control, "getAddressModel").and.returnValue(suggest.model);
      spyOn(suggest, "reload").and.callFake(() => suggest.model.values = [{value: 3, label: "Another one bites the dust"}]);
      suggest.onModelChangedSelected();

      // Check values updated
      expect(suggest.model.values).toEqual([{value: 3, label: "Another one bites the dust"}]);
    });

    it('should update model values with api - test 1', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};

      // Update model values
      // Define values to update
      suggest.api.updateModelValues(data);

      // Check values updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}]);
    });

    it('should update model values with api - test 2', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data2 = {
        values: [{value: "A", label: "tutu"}, {value: "B", label: "lala"}, {value: "C", label: "lolo"}],
        selected: ["B"]
      };

      // Update model values
      suggest.api.updateModelValues(data2);

      // Check values updated
      expect(model.values).toEqual([{value: "A", label: "tutu"}, {value: "B", label: "lala"}, {
        value: "C",
        label: "lolo"
      }]);
    });

    it('should update model values with api - test 3', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: "B", label: "lala"}, {value: 2, label: "Other"}],
        selected: ["B"]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};

      // Update model values
      suggest.api.updateModelValues(data);

      // Check values updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}]);
    });

    it('should update model values with api - test 4', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [1]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data3 = {selected: [0]};

      // Update selected values
      suggest.api.updateModelValues(data3);

      // Check selected updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}]);
      expect(model.selected).toEqual(0);
    });

    it('should update model values with api - test 5', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: null
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: []};

      // Update selected values
      suggest.api.updateModelValues(data);

      // Check selected updated
      expect(model.values).toEqual([]);
      expect(model.selected).toEqual(null);
    });
  });

  describe('once initialized as multiple suggest', function () {
    let suggest;

    // Mock module
    beforeEach(function () {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      suggest = new $selector($scope, "tutu", {});
      spyOn($control, "getAddressController").and.returnValue({id: "tutu"});
      spyOn($control, "checkComponent").and.returnValue(true);
      suggest.asSuggestMultiple();
    });

    it('should update model values with api - test 1', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};

      // Update model values
      // Define values to update
      suggest.api.updateModelValues(data);

      // Check values updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}]);
      expect(model.selected).toEqual([1]);
    });

    it('should update model values with api - test 2', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [2]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data2 = {
        values: [{value: "A", label: "tutu"}, {value: "B", label: "lala"}, {value: "C", label: "lolo"}],
        selected: ["B", "C"]
      };

      // Update model values
      suggest.api.updateModelValues(data2);

      // Check values updated
      expect(model.values).toEqual([
        {value: "A", label: "tutu"},
        {value: "B", label: "lala"},
        {value: "C", label: "lolo"}]);
      expect(model.selected).toEqual(["B", "C"]);
    });

    it('should update model values with api - test 3', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: "B", label: "lala"}, {value: 2, label: "Other"}],
        selected: ["B"]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}], selected: [1]};

      // Update model values
      suggest.api.updateModelValues(data);

      // Check values updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}]);
    });

    it('should update model values with api - test 4', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: [1]
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data3 = {selected: [0, 2]};

      // Update selected values
      suggest.api.updateModelValues(data3);

      // Check selected updated
      expect(model.values).toEqual([{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}]);
      expect(model.selected).toEqual([0, 2]);
    });

    it('should update model values with api - test 5', function () {
      let model = {
        storedValues: [{value: 0, label: "No"}, {value: 1, label: "Yes"}],
        values: [{value: 0, label: "No"}, {value: 1, label: "Yes"}, {value: 2, label: "Other"}],
        selected: null
      };
      spyOn($control, "getAddressModel").and.returnValue(model);

      // Define values to update
      let data = {values: []};

      // Update selected values
      suggest.api.updateModelValues(data);

      // Check selected updated
      expect(model.values).toEqual([]);
      expect(model.selected).toEqual(null);
    });
  });
});
