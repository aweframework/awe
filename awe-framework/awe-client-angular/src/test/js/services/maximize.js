describe('awe-framework/awe-client-angular/src/test/js/services/maximize.js', function() {
  let $injector, Maximize, Utilities, Position;
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

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  it('should init maximize', function() {
    const element = {};
    const $scope = {
      controller,
      $root: {},
      $on: jest.fn().mockName("$on")
    };
    // Init
    Maximize.initMaximize($scope, element);
  });

  it('should init maximize without controller', function() {
    const element = {};
    const $scope = {
      $root: {},
      $on: jest.fn().mockName("$on")
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
      $on: jest.fn().mockName("$on"),
      $emit: jest.fn().mockName("$emit"),
      $broadcast: jest.fn().mockName("$broadcast")
    };
    // Passthrough timeout
    jest.spyOn(Utilities, "timeout").mockImplementation((fn) => fn());
    jest.spyOn(element, "val").mockReturnValue(getNode(element, resizing, parent));
    jest.spyOn(resizing, "val").mockReturnValue(getNode(resizing, resizing, resizing));
    jest.spyOn(parent, "val").mockReturnValue(getNode(parent, parent, parent));
    jest.spyOn($.fn, "offset").mockReturnValue({top: 0, left: 0});
    jest.spyOn(Utilities, "animateCSS").mockImplementation((a, b, c, fn) => fn());

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
      $on: jest.fn().mockName("$on"),
      $emit: jest.fn().mockName("$emit"),
      $broadcast: jest.fn().mockName("$broadcast")
    };
    // Passthrough timeout
    jest.spyOn(Utilities, "timeout").mockImplementation((fn) => fn());
    //jest.spyOn($.fn, "val").mockImplementation((e) => e);
    jest.spyOn(element, "val").mockReturnValue(getNode(element, resizing, parent));
    jest.spyOn(resizing, "val").mockReturnValue(getNode(resizing, resizing, resizing));
    jest.spyOn(parent, "val").mockReturnValue(getNode(parent, parent, parent));
    jest.spyOn(Utilities, "animateCSS").mockImplementation((a, b, c, fn) => fn());

    // Init
    Maximize.initMaximize($scope, element);

    // Restore panel
    $scope.restorePanel();

    // Expect
    expect($scope.$broadcast).toHaveBeenCalledWith("resize");
  });

});