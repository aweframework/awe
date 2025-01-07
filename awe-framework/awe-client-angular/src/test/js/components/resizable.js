import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/components/resizable.js', function () {
  let  $injector, $rootScope, $compile, $httpBackend, $actionController, $screen, $control, $storage, $sce, $utilities;

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Screen", "Control", "Storage", "$sce", "AweUtilities",
      function (__$injector__, _$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Screen_, _Control_, __Storage__, _$sce_, _$utilities_) {
        $injector = __$injector__;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = _ActionController_;
        $screen = _Screen_;
        $control = _Control_;
        $storage = __Storage__;
        $sce = _$sce_;
        $utilities = _$utilities_;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en-GB"});

      }]);
  });

  it('replaces the element with the appropriate content', function () {
    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);


    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-resizable resizable-id='Resizable'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("resizable-id")).toEqual("Resizable");
    expect(element[0].tagName).toBe("DIV");
  });

  it('checks drag start up', function (done) {
    // Spy on storage
    $scope = $rootScope.$new();
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);
    spyOn($utilities, "publish");

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-resizable resizable-id='Resizable'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("resizable-id")).toEqual("Resizable");
    expect(element[0].tagName).toBe("DIV");

    // Call drag start method
    let $scope = element.isolateScope() || element.scope();
    $scope.dragStart({}, "top");

    setTimeout(() => {
      document.dispatchEvent(new Event("mousemove"));
      setTimeout(() => {
        document.dispatchEvent(new Event("mouseup"));
        expect($utilities.publish).toHaveBeenCalled();
        done();
      }, 100);
    }, 100);
  });

  it('checks drag start bottom', function (done) {
    // Spy on storage
    $scope = $rootScope.$new();
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);
    spyOn($utilities, "publish");

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-resizable resizable-id='Resizable'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("resizable-id")).toEqual("Resizable");
    expect(element[0].tagName).toBe("DIV");

    // Call drag start method
    let $scope = element.isolateScope() || element.scope();
    $scope.dragStart({}, "bottom");

    setTimeout(() => {
      document.dispatchEvent(new Event("mousemove"));
      setTimeout(() => {
        document.dispatchEvent(new Event("mouseup"));
        expect($utilities.publish).toHaveBeenCalled();
        done();
      }, 100);
    }, 100);
  });

  it('checks drag start left', function (done) {
    // Spy on storage
    $scope = $rootScope.$new();
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);
    spyOn($utilities, "publish");

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-resizable resizable-id='Resizable'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("resizable-id")).toEqual("Resizable");
    expect(element[0].tagName).toBe("DIV");

    // Call drag start method
    let $scope = element.isolateScope() || element.scope();
    $scope.dragStart({}, "left");

    setTimeout(() => {
      document.dispatchEvent(new Event("mousemove"));
      setTimeout(() => {
        document.dispatchEvent(new Event("mouseup"));
        expect($utilities.publish).toHaveBeenCalled();
        done();
      }, 100);
    }, 100);
  });

  it('checks drag start right', function (done) {
    // Spy on storage
    $scope = $rootScope.$new();
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);
    spyOn($utilities, "publish");

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-resizable resizable-id='Resizable'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("resizable-id")).toEqual("Resizable");
    expect(element[0].tagName).toBe("DIV");

    // Call drag start method
    let $scope = element.isolateScope() || element.scope();
    $scope.dragStart({}, "right");

    setTimeout(() => {
      document.dispatchEvent(new Event("mousemove"));
      setTimeout(() => {
        document.dispatchEvent(new Event("mouseup"));
        expect($utilities.publish).toHaveBeenCalled();
        done();
      }, 100);
    }, 100);
  });
});