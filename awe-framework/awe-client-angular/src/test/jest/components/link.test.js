import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/components/link.js', function () {
  let  $rootScope, $compile, $httpBackend, $control, $storage;

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage",
      function (_$rootScope_, _$compile_, _$httpBackend_, _Control_, __Storage__) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = __Storage__;

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

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-link/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element[0].tagName).toBe("A");
  });

  it('replaces the element with the appropriate content', function () {
    // Spy on storage
    jest.spyOn($storage, "get").mockReturnValue({'base': {}});
    jest.spyOn($control, 'checkOnlyComponent').mockReturnValue(true);

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-link link-id='Link'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("link-id")).toEqual("Link");
    expect(element[0].tagName).toBe("A");
  });
});
