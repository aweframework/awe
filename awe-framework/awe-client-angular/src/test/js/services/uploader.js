describe('awe-framework/awe-client-angular/src/test/js/services/uploader.js', function () {
  let $injector, $settings, $control, $rootScope, $uploader;
  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $settings = $injector.get('AweSettings');
      $control = $injector.get('Control');
      $uploader = $injector.get('Uploader');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function () {  });

  it('should init as uploader', function () {
    let $scope = $rootScope.$new();
    let uploader = new $uploader($scope, "tutu", {});
    uploader.updateClasses = () => null;
    uploader.controller = {};
    uploader.model = {};
    jest.spyOn(uploader, "asCriterion").mockReturnValue(true);
    jest.spyOn($control, "getAddressModel").mockReturnValue({report: {values: [], selected: []}});
    jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    expect(uploader.asUploader()).toBe(true);
  });

  describe('once initialized as uploader', function () {
    let uploader;
    let $scope;

    // Mock module
    beforeEach(function () {
      $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      uploader = new $uploader($scope, "tutu", {});
      uploader.updateClasses = () => null;
      uploader.controller = {};
      uploader.model = {};
      jest.spyOn($control, "getAddressModel").mockReturnValue({report: {values: [], selected: []}});
      jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      uploader.asUploader();
    });

    it('should validate a file', function () {
      jest.spyOn($settings, "get").mockReturnValue(1);
      expect($scope.validate({size: 100})).toBe(true);
      expect($scope.validate({size: 1000000000})).toBe(false);
    });
  });
});