describe('awe-framework/awe-client-angular/src/test/js/services/maximize.js', function() {
  let $injector, Maximize, Utilities, Position;
  let originalTimeout;

  const controller = {
    maximize: true
  };

  const getNode = (node, resizing, parent) => ({
    val: () => node,
    offset: () => ({top: 100, left: 100}),
    scrollTop: () => 0,
    scrollLeft: () => 0,
    outerWidth: () => 100,
    outerHeight: () => 100,
    offsetParent: () => parent,
    clone: () => resizing,
    parents: () => parent,
    css: () => resizing,
    addClass: () => resizing,
    removeClass: () => resizing,
    find: () => resizing,
    empty: () => null,
    parentsUntil: () => [parent, resizing, parent]
  });

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      Maximize = $injector.get('Maximize');
      Position = $injector.get('Position');
      Utilities = $injector.get('AweUtilities');
    }]);

    originalTimeout = jasmine.DEFAULT_TIMEOUT_INTERVAL;
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 10000;
  });

  afterEach(function() {
    jasmine.DEFAULT_TIMEOUT_INTERVAL = originalTimeout;
  });

  it('should init maximize', function() {
    const element = {};
    const $scope = {
      controller,
      $root: {},
      $on: jasmine.createSpy("$on")
    };
    // Init
    Maximize.initMaximize($scope, element);
  });

  it('should init maximize without controller', function() {
    const element = {};
    const $scope = {
      $root: {},
      $on: jasmine.createSpy("$on")
    };
    // Init
    Maximize.initMaximize($scope, element);
  });

  it('should maximize panel', function() {
    const element = $(document.createElement("div"));
    const parent = $(document.createElement("div"));
    const resizing = $(document.createElement("div"));

    const $scope = {
      controller,
      $root: {},
      $on: jasmine.createSpy("$on"),
      $emit: jasmine.createSpy("$emit"),
      $broadcast: jasmine.createSpy("$broadcast")
    };
    // Passthrough timeout
    spyOn(Utilities, "timeout").and.callFake((fn) => fn());
    spyOn(element, "val").and.returnValue(getNode(element, resizing, parent));
    spyOn(resizing, "val").and.returnValue(getNode(resizing, resizing, resizing));
    spyOn(parent, "val").and.returnValue(getNode(parent, parent, parent));
    spyOn($.fn, "offset").and.returnValue({top: 0, left: 0});
    spyOn(Utilities, "animateCSS").and.callFake((a, b, c, fn) => fn());

    // Init
    Maximize.initMaximize($scope, element);

    // Maximize panel
    $scope.maximizePanel();

    // Expect
    expect($scope.$broadcast).toHaveBeenCalledWith("resize");
  });

  it('should restore panel', function() {
    const element = $(document.createElement("div"));
    const parent = $(document.createElement("div"));
    const resizing = $(document.createElement("div"));
    const $scope = {
      controller,
      $root: {},
      $on: jasmine.createSpy("$on"),
      $emit: jasmine.createSpy("$emit"),
      $broadcast: jasmine.createSpy("$broadcast")
    };
    // Passthrough timeout
    spyOn(Utilities, "timeout").and.callFake((fn) => fn());
    //spyOn($.fn, "val").and.callFake((e) => e);
    spyOn(element, "val").and.returnValue(getNode(element, resizing, parent));
    spyOn(resizing, "val").and.returnValue(getNode(resizing, resizing, resizing));
    spyOn(parent, "val").and.returnValue(getNode(parent, parent, parent));
    spyOn(Utilities, "animateCSS").and.callFake((a, b, c, fn) => fn());

    // Init
    Maximize.initMaximize($scope, element);

    // Restore panel
    $scope.restorePanel();

    // Expect
    expect($scope.$broadcast).toHaveBeenCalledWith("resize");
  });

});