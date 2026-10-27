import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/components/contextOption.js', function () {
  let  $rootScope, $compile, $httpBackend, $control, $storage, Utilities;
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
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage", "AweUtilities",
      function (_$rootScope_, _$compile_, _$httpBackend_, _Control_, __Storage__, _Utilities_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = __Storage__;
        Utilities = _Utilities_;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en"});

      }]);
  });

  it('tries to initialize as component without id', function () {
    // Spy on storage
    jest.spyOn($storage, "get").mockReturnValue({'base': {}});
    jest.spyOn($control, 'checkOnlyComponent').mockReturnValue(true);
    jest.spyOn(Utilities, "timeout").mockImplementation(fn => fn());
    $rootScope.option = controller;

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-context-option/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element[0].tagName).toBe("LI");
  });

  it('replaces the element with the appropriate content', function () {
    // Spy on storage
    jest.spyOn($storage, "get").mockReturnValue({'base': {}});
    jest.spyOn($control, 'checkOnlyComponent').mockReturnValue(true);
    jest.spyOn(Utilities, "timeout").mockImplementation(fn => fn());
    $rootScope.option = controller;

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-context-option option-id='ContextOption'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("option-id")).toEqual("ContextOption");
    expect(element[0].tagName).toBe("LI");
  });
});
