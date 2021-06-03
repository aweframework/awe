import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/plugins/uiNumeric.js', function() {
  let $rootScope, $compile, $httpBackend, $settings, $control, $utilities;
  let numericOptions = "{}";
  let controller = {
    numberFormat: "{min: 0}",
    checkInitial: true,
    checkTarget: false,
    checked: false,
    component: "wizard",
    contextMenu: [],
    dependencies: [],
    icon: "search",
    id: "wizardId",
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
  let model = {
    page: 1,
    records: 3,
    selected: "3",
    total: 1,
    values: [{label: "Step 1", value: "1"}, {label: "Step 2", value: "2"}, {label: "Step 3", value: "3"}]
  };
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
    let element = $compile("<input ui-numeric='" + numericOptions + "'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
  });

  it('initializes a numeric plugin', function() {
    $rootScope.firstLoad = true;
    $rootScope.component = component;

    // Spy on storage
    spyOn($control, "checkComponent").and.returnValue(true);

    // Compile a piece of HTML containing the directive
    let element = $compile("<input ui-numeric='{min:50, max: 100, precision: 2}'/>")($rootScope);

    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
  });

  it('initializes a numeric plugin and change its value', function (done) {
    $rootScope.firstLoad = true;
    $rootScope.component = component;
    let model = {values: [{}], selected: "1200.21"};

    // Spy on storage
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressModel").and.returnValue(model);
    spyOn($utilities, "timeout").and.callFake(fn => {
      fn();
      done();
    });

    // Compile a piece of HTML containing the directive
    let element = $compile("<input ui-numeric='{min:50, max: 100000, precision: 2, aSep: \".\", aDec: \",\"}'/>")($rootScope);

    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();

    element.trigger('change', {});

    $rootScope.$digest();

    expect(model).toEqual({selected: 1200.21, values: [{value: 1200.21, label: "1.200,21"}]});
  });
});