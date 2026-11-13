import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/components/avatar.js', function () {
  let $rootScope, $compile, $httpBackend, $actionController, $control, $storage, $utilities, $component;
  let controller = {
    id: "testAvatar",
    component: "avatar",
    title: "Test Avatar",
    text: "Avatar Text",
    label: "Avatar Label",
    image: "/images/default-avatar.png",
    icon: "user-circle",
    unit: "User",
    style: "avatar-style",
    dropdownStyle: "dropdown-style",
    showLabel: true,
    visible: true,
    children: 0,
    actions: [{
      type: "server",
      target: "profile",
      name: "viewProfile"
    }]
  };

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$compile", "$httpBackend", "ActionController", "Control", "Storage", "AweUtilities", "Component",
      function (_$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Control_, __Storage__, _AweUtilities_, _Component_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = _ActionController_;
        $control = _Control_;
        $storage = __Storage__;
        $utilities = _AweUtilities_;
        $component = _Component_;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en"});
      }]);
  });

  /**
   * Define avatar element for tests
   * @param {String} avatarId Avatar identifier
   * @returns {Object} Compiled element
   */
  function defineAvatar(avatarId = 'testAvatar') {
    // Spy on storage
    jest.spyOn($storage, "get").mockReturnValue({'base': {}});
    jest.spyOn($control, 'checkOnlyComponent').mockReturnValue(true);
    jest.spyOn($utilities, "timeout").mockImplementation(fn => fn());
    jest.spyOn($utilities, "getContextPath").mockReturnValue("/context");

    // Compile a piece of HTML containing the directive
    let element = $compile(`<awe-avatar avatar-id='${avatarId}'></awe-avatar>`)($rootScope);

    // fire all the watches
    $rootScope.$digest();

    return element;
  }

  it('replaces the element with the appropriate content', function () {
    // Define avatar
    let element = defineAvatar();

    // Expect element is defined and directive is active
    expect(element[0].tagName).toBe("LI");
    expect(element.attr("avatar-id")).toEqual("testAvatar");
  });

  it('tries to initialize as component without id', function () {
    // Spy on storage
    jest.spyOn($storage, "get").mockReturnValue({'base': {}});
    jest.spyOn($control, 'checkOnlyComponent').mockReturnValue(true);
    jest.spyOn($utilities, "timeout").mockImplementation(fn => fn());

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-avatar/>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

    expect(element[0].tagName).toBe("LI");
  });

  it('handles onClick without actions', function () {
    // Mock component initialization
    jest.spyOn($component.prototype, "asComponent").mockReturnValue(true);
    jest.spyOn($actionController, "addActionList").mockReturnValue(null);

    // Set up controller data without actions
    let controllerNoActions = {...controller, actions: null};
    $rootScope.controller = controllerNoActions;

    // Define avatar
    let element = defineAvatar();

    // Simulate a click on the avatar
    element.find('a').triggerHandler('click');

    // Check that action was not triggered
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it('fails component initialization', function () {
    // Mock component initialization failure
    jest.spyOn($component.prototype, "asComponent").mockReturnValue(false);

    // Define avatar
    let element = defineAvatar();

    // Check that the component was not initialized properly
    let scope = element.isolateScope();
    expect(scope.controller).toBeUndefined();
  });

  it('exposes getContextPath to the scope', function () {
    // Mock component initialization
    jest.spyOn($component.prototype, "asComponent").mockReturnValue(true);

    // Define avatar
    let element = defineAvatar();

    // Check that getContextPath is exposed to the scope
    let scope = element.isolateScope();
    expect(scope.getContextPath).toBe($utilities.getContextPath);
  });
});
