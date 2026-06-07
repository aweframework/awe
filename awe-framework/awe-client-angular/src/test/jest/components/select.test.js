import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/components/select.js', function() {
  let  $rootScope, $compile, $httpBackend, $control, $storage;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage",
      function(_$rootScope_, _$compile_, _$httpBackend_, _Control_, __Storage__){
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $control = _Control_;
      $storage = __Storage__;

      $rootScope.view = 'base';
      $rootScope.context = 'screen';

      // backend definition common for all tests
      $httpBackend.when('POST', 'settings').respond(DefaultSettings);
      $httpBackend.when('POST', './settings').respond(DefaultSettings);

      $storage.put("controller", {base: {}});
      $storage.put("model", {base: {}});
      $storage.put("api", {base: {}});
    }]);
  });

  function registerSelectorData() {
    $control.setAddressController({view: "base", component: "RefreshTime"}, {
      numberFormat: "{min: 0}", checkInitial: true, checkTarget: false, checked: false, component: "select", contextMenu: [],
      dependencies: [], icon: "search", id: "RefreshTime", loadAll: false, optional: false, placeholder: "SCREEN_TEXT_USER",
      printable: true, readonly: false, required: true, size: "lg", strict: true, style: "no-label", validation: "required",
      visible: true
    });
    $control.setAddressModel({view: "base", component: "RefreshTime"}, {
      defaultValues: [], page: 1, records: 0, selected: [], total: 0, values: []
    });
  }

  function getComponentScope(element) {
    return element.isolateScope() || element.scope();
  }

  function expectSelectorInput(element, id) {
    let input = element.find(".validator.input > input[type='hidden'][ui-select2]");

    expect(element.find("awe-context-menu").length).toBe(0);
    expect(element.attr("criterion-id")).toBe(id);
    expect(input.length).toBe(1);
    expect(input.attr("id")).toBe(id);
    expect(input.attr("name")).toBe(id);
    expect(input.attr("autocomplete")).toBe("off");
    return input;
  }

  it('replaces the element with the appropriate content', function() {
    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-input-select input-select-id='RefreshTime'></awe-input-select>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();

    expect(element.find("awe-context-menu").length).toBe(0);
    expect(element.find(".criterion").attr("criterion-id")).toBeUndefined();
    expect(element.find(".validator.input > select").length).toBe(0);
    expect(element.find(".validator.input > input[type='hidden'][ui-select2]").length).toBe(1);
    expect(element.find(".validator.input > input[type='hidden'][ui-select2]").attr("id")).toBeUndefined();
  });

  it('initializes a select', function(done) {
    $rootScope.firstLoad = true;
    registerSelectorData();

    // Compile a piece of HTML containing the directive after screen data is available
    let  element = $compile("<awe-input-select input-select-id='RefreshTime'></awe-input-select>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

      let input = expectSelectorInput(element, "RefreshTime");
      expect(element.find(".form-group.group-lg.w-icon").length).toBe(1);
      expect(element.find(".validator.input > span.criterion-icon-lg.form-icon.fa.fa-search").length).toBe(1);
      expect(input.attr("multiple")).toBeUndefined();
      expect(input.attr("on-refresh")).toBeUndefined();
      expect(getComponentScope(element).aweSelectOptions).toEqual(expect.objectContaining({
        minimumResultsForSearch: 5,
        multiple: false,
        placeholder: "SCREEN_TEXT_USER"
      }));
    done();
  });

  it('initializes a suggest', function(done) {
    $rootScope.firstLoad = true;
    registerSelectorData();

    // Compile a piece of HTML containing the directive after screen data is available
    let  element = $compile("<awe-input-suggest input-suggest-id='RefreshTime'></awe-input-suggest>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

      let input = expectSelectorInput(element, "RefreshTime");
      expect(element.find(".form-group.group-lg.w-icon").length).toBe(1);
      expect(element.find(".validator.input > span.criterion-icon-lg.form-icon.fa.fa-search").length).toBe(1);
      expect(input.attr("multiple")).toBeUndefined();
      expect(input.attr("on-refresh")).toBeUndefined();
      expect(getComponentScope(element).aweSelectOptions).toEqual(expect.objectContaining({
        minimumInputLength: 1,
        multiple: false,
        placeholder: "SCREEN_TEXT_USER"
      }));
    done();
  });

  it('initializes a select multiple', function(done) {
    $rootScope.firstLoad = true;
    registerSelectorData();

    // Compile a piece of HTML containing the directive after screen data is available
    let  element = $compile("<awe-input-select-multiple input-select-multiple-id='RefreshTime'></awe-input-select-multiple>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

      let input = expectSelectorInput(element, "RefreshTime");
      expect(element.find(".form-group.group-lg.w-icon").length).toBe(1);
      expect(element.find(".validator.input > span.criterion-icon-lg.form-icon.fa.fa-search").length).toBe(1);
      expect(input.attr("multiple")).toBeUndefined();
      expect(input.attr("on-refresh")).toBeUndefined();
      expect(getComponentScope(element).aweSelectOptions).toEqual(expect.objectContaining({
        minimumResultsForSearch: 5,
        multiple: true,
        placeholder: "SCREEN_TEXT_USER"
      }));
    done();
  });

  it('initializes a suggest multiple', function(done) {
    $rootScope.firstLoad = true;
    registerSelectorData();

    // Compile a piece of HTML containing the directive after screen data is available
    let  element = $compile("<awe-input-suggest-multiple input-suggest-multiple-id='RefreshTime'></awe-input-suggest-multiple>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

      let input = expectSelectorInput(element, "RefreshTime");
      expect(element.find(".form-group.group-lg.w-icon").length).toBe(1);
      expect(element.find(".validator.input > span.criterion-icon-lg.form-icon.fa.fa-search").length).toBe(1);
      expect(input.attr("multiple")).toBeUndefined();
      expect(input.attr("on-refresh")).toBeUndefined();
      expect(getComponentScope(element).aweSelectOptions).toEqual(expect.objectContaining({
        minimumInputLength: 1,
        multiple: true,
        placeholder: "SCREEN_TEXT_USER"
      }));
    done();
  });
});
