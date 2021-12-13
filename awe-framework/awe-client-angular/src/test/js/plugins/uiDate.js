import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/plugins/uiDate.js', function() {
  let $rootScope, $compile, $httpBackend, $settings, $control, $utilities;
  let datepickerOptions = {
    autoclose: true,
    container: "body",
    enableOnReadonly: false,
    format: "dd/mm/yyyy",
    language: "en",
    maxViewMode: 2,
    todayBtn: "linked",
    todayHighlight: true
  };
  let controller = {
    checkInitial: true,
    checkTarget: false,
    checked: false,
    component: "date",
    contextMenu: [],
    dependencies: [],
    icon: "search",
    id: "dateId",
    loadAll: false,
    optional: false,
    placeholder: "SCREEN_TEXT_USER",
    printable: true,
    readonly: false,
    required: true,
    size: "lg",
    strict: true,
    style: "no-label",
    validation: "required",
    visible: true
  };
  let model = {page:1, records:1, selected: "23/10/1978", total:1, values:[]};
  let component = {controller: controller, model: model, api: {}, modelChange: jasmine.createSpy("modelChange")};

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$compile", "$httpBackend", "AweSettings", "AweUtilities", "Control",
      function (_$rootScope_, _$compile_, _$httpBackend_, __$settings__, __$utilities__, __$control__) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $settings = __$settings__;
        $control = __$control__;
        $utilities = __$utilities__;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond(DefaultSettings);
      }]);
  });

  it('replaces the element with the appropriate content', function() {
    // Compile a piece of HTML containing the directive
    $rootScope.component = component;
    let element = $compile("<input ui-date='" + datepickerOptions + "'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
  });

  it('initializes a date plugin', function() {
    $rootScope.firstLoad = true;
    $rootScope.component = component;

    // Spy on storage
    spyOn($control, "checkComponent").and.returnValue(true);

    // Compile a piece of HTML containing the directive
    let element = $compile("<input ui-date='" + datepickerOptions + "'/>")($rootScope);

    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
  });

  it('initializes a date plugin and change its value', function (done) {
    $rootScope.firstLoad = true;
    $rootScope.component = component;

    // Spy on storage
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressModel").and.returnValue(component.model);
    spyOn($utilities, "timeout").and.callFake(fn => {
      fn();
      done();
    });

    $rootScope.initialized = false;
    // Compile a piece of HTML containing the directive
    let element = $compile("<input ui-date='dateOptions' initialized='initialized'/>")($rootScope);
    $rootScope.$digest();
    $rootScope.initialized = true;
    $rootScope.dateOptions = datepickerOptions;
    $rootScope.$digest();

    component.model.selected = "15/11/2020";
    component.updateModelValues();
    component.updateModelSelected();

    $rootScope.$digest();

    expect(component.model).toEqual({selected: '15/11/2020', values: [], page: 1, records: 1, total: 1});
  });

  it('initializes a date plugin and empty it', function (done) {
    $rootScope.firstLoad = true;
    $rootScope.component = component;

    // Spy on storage
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressModel").and.returnValue(component.model);
    spyOn($utilities, "timeout").and.callFake(fn => {
      fn();
      done();
    });

    $rootScope.initialized = false;
    // Compile a piece of HTML containing the directive
    let element = $compile("<input ui-date='dateOptions' initialized='initialized'/>")($rootScope);
    $rootScope.$digest();
    $rootScope.initialized = true;
    $rootScope.dateOptions = datepickerOptions;
    $rootScope.$digest();

    component.model.selected = null;
    component.updateModelValues();
    component.updateModelSelected();

    $rootScope.$digest();

    expect(component.model).toEqual({selected: null, values: [], page: 1, records: 1, total: 1});
  });
});