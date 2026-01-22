import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/components/avatar.js', function () {
  let $injector, $rootScope, $compile, $httpBackend, $actionController, $control, $storage, $utilities, $component, $serverData;
  let model = {
    values: [{
      title: "User Avatar",
      label: "John Doe",
      image: "/images/avatar.png",
      icon: "user",
      unit: "Admin"
    }]
  };
  let emptyModel = {values: []};
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
  let controllerWithChildren = {
    ...controller,
    children: 2
  };

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Control", "Storage", "AweUtilities", "Component", "ServerData",
      function (__$injector__, _$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Control_, __Storage__, _AweUtilities_, _Component_, _ServerData_) {
        $injector = __$injector__;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = _ActionController_;
        $control = _Control_;
        $storage = __Storage__;
        $utilities = _AweUtilities_;
        $component = _Component_;
        $serverData = _ServerData_;

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
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkOnlyComponent').and.returnValue(true);
    spyOn($utilities, "timeout").and.callFake(fn => fn());
    spyOn($utilities, "getContextPath").and.returnValue("/context");

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
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkOnlyComponent').and.returnValue(true);
    spyOn($utilities, "timeout").and.callFake(fn => fn());

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-avatar/>")($rootScope);

    // fire all the watches
    $rootScope.$digest();

    expect(element[0].tagName).toBe("LI");
  });

  it('handles onClick without actions', function () {
    // Mock component initialization
    spyOn($component.prototype, "asComponent").and.returnValue(true);
    spyOn($actionController, "addActionList").and.returnValue(null);

    // Set up controller data without actions
    let controllerNoActions = {...controller, actions: null};
    $rootScope.controller = controllerNoActions;

    // Define avatar
    let element = defineAvatar();

    // Get scope and trigger click
    let scope = element.isolateScope();

    // Simulate a click on the avatar
    element.find('a').triggerHandler('click');

    // Check that action was not triggered
    expect($actionController.addActionList).not.toHaveBeenCalled();
  });

  it('fails component initialization', function () {
    // Mock component initialization failure
    spyOn($component.prototype, "asComponent").and.returnValue(false);

    // Define avatar
    let element = defineAvatar();

    // Check that the component was not initialized properly
    let scope = element.isolateScope();
    expect(scope.controller).toBeUndefined();
  });

  it('exposes getContextPath to the scope', function () {
    // Mock component initialization
    spyOn($component.prototype, "asComponent").and.returnValue(true);

    // Define avatar
    let element = defineAvatar();

    // Check that getContextPath is exposed to the scope
    let scope = element.isolateScope();
    expect(scope.getContextPath).toBe($utilities.getContextPath);
  });
});
