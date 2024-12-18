import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/components/contextOption.js', function () {
  let  $injector, $rootScope, $compile, $httpBackend, $actionController, $screen, $control, $storage, $sce, Utilities;
  let model = {
    page: 1,
    records: 1,
    selected: "23/10/1978",
    total: 1,
    values: [{label: "23/10/1978", value: "23/10/1978"}]
  };
  let emptyModel = {page: 1, records: 0, selected: null, total: 0, values: []};
  let controller = {
    checkInitial: true,
    checkTarget: false,
    checked: false,
    component: "contextOption",
    contextMenu: [],
    dependencies: [],
    icon: "search",
    id: "ContextOption",
    loadAll: false,
    optional: false,
    placeholder: "SCREEN_TEXT_DATE",
    printable: true,
    readonly: false,
    required: true,
    size: "lg",
    strict: true,
    style: "no-label",
    validation: "required",
    visible: true,
    separator: false
  };

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Screen", "Control", "Storage", "$sce", "AweUtilities",
      function (__$injector__, _$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Screen_, _Control_, __Storage__, _$sce_, _Utilities_) {
        $injector = __$injector__;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = _ActionController_;
        $screen = _Screen_;
        $control = _Control_;
        $storage = __Storage__;
        $sce = _$sce_;
        Utilities = _Utilities_;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en"});

      }]);
  });

  it('tries to initialize as component without id', function () {
    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkOnlyComponent').and.returnValue(true);
    spyOn(Utilities, "timeout").and.callFake(fn => fn());
    $rootScope.option = controller;

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-context-option/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element[0].tagName).toBe("LI");
  });

  it('replaces the element with the appropriate content', function () {
    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkOnlyComponent').and.returnValue(true);
    spyOn(Utilities, "timeout").and.callFake(fn => fn());
    $rootScope.option = controller;

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-context-option option-id='ContextOption'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("option-id")).toEqual("ContextOption");
    expect(element[0].tagName).toBe("LI");
  });
});