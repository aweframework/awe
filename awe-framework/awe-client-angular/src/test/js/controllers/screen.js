describe('awe-framework/awe-client-angular/src/test/js/controllers/screen.js', function() {
  let scope, controller, $settings, $actionController, $dependencyController, $screen;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$controller", "AweSettings", "ActionController", "DependencyController", "Screen",
      function($scope, $controller, _AweSettings_, _ActionController_, _DependencyController_, _Screen_){
      scope = $scope.$new();
      $screen = _Screen_;
      $settings = _AweSettings_;
      $actionController = _ActionController_;
      $dependencyController = _DependencyController_;
      controller = $controller('ScreenController', {
        '$scope': scope,
        'AweSettings': $settings,
        'ActionController': $actionController,
        'DependencyController': $dependencyController,
        'Screen': $screen
      });
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  // A simple test to verify the controller exists
  it('should exist', function() {
    expect(controller).toBeDefined();
  });

  // Once initialized, launch tests
  describe('once initialized', function() {
    // Check show actions
    it('checks whether show actions or not', function() {
      expect(controller.showActions()).toBe($settings.get("actionsStack") > 0);
    });

    // Check action information
    it('should log action information', function() {
      let action = {action: "miAccion", parameters: ["tutu", {lala: 1}]};
      jest.spyOn(console, 'info');
      controller.showInfo(action);
      expect(console.info).toHaveBeenCalled();
    });

    // Trigger scope events
    it('should trigger scope events', function() {
      jest.spyOn($dependencyController, "checkAndLaunch");
      jest.spyOn($dependencyController, "start");
      jest.spyOn($dependencyController, "restart");
      jest.spyOn($dependencyController, "unregisterView");
      jest.spyOn($screen, "screen");
      scope.$emit("modelChanged");
      scope.$emit("compiled");
      scope.$emit("initialize-cell");
      scope.$emit("unload");
      scope.$emit("/action/screen");
    });
  });
});