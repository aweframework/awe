describe('awe-framework/awe-client-angular/src/test/js/services/validator.js', function() {
  let $control, $utilities, $settings, $validator, $window;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');
    inject(["$injector", function($injector) {
      $utilities = $injector.get("AweUtilities");
      $control = $injector.get('Control');
      $settings = $injector.get("AweSettings");
      $window = $injector.get('$window');
      $validator = $injector.get('Validator');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  // A simple test to verify the controller exists
  it('should exist', function() {
    expect($validator).toBeDefined();
  });

  // Show validation error
  it('should show a validation error without timeout', function(done) {
    jest.spyOn($.fn, "offset").mockReturnValue({top:0});
    jest.spyOn($settings, "get").mockReturnValue({validate: 0});
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => {
      fn();
      expect(scope.showValidation).toBe(true);
      done();
    });
    let scope = {};
    $validator.showValidationError(scope, {element: $window});
  });

  // Show validation error
  it('should show a validation error with timeout', function(done) {
    jest.spyOn($.fn, "offset").mockReturnValue({top:0});
    jest.spyOn($settings, "get").mockReturnValue({validate: 2000});
    jest.spyOn($utilities, "timeout").mockImplementation((fn) => {
      fn();
      if (!scope.showValidation) {
        done();
      }
    });
    let scope = {};
    $validator.showValidationError(scope, {element: $window});
  });
});
