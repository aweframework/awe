import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe('awe-framework/awe-client-angular/src/test/jest/controllers/download.js', function() {
  let scope, controller, $utilities, $actionController, $dependencyController, $screen;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$rootScope", "$controller", "AweUtilities", "ActionController",
      function($scope, $controller, _AweUtilities_, _ActionController_){
      scope = $scope.$new();
      $utilities = _AweUtilities_;
      $actionController = _ActionController_;
      controller = $controller('DownloadController', {
        '$scope': scope,
        'AweUtilities': $utilities,
        'ActionController': $actionController
      });
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  // A simple test to verify the controller exists
  it('should exist', function() {
    expect(controller).toBeDefined();
  });

  // Once initialized, launch tests
  describe('once initialized', function() {
    // Start a download
    it('should start a download', function() {
      controller.startDownload({});
      expect(controller.downloads.length).toBe(1);
    });

    // Finish a download
    it('should finish a download with an action', function() {
      jest.spyOn($utilities, "timeout").mockImplementation((fn) => fn());
      $utilities.timeout.cancel = jest.fn();
      let file = {action: {accept: () => null}};
      controller.startDownload(file);
      expect(controller.downloads.length).toBe(1);
      controller.finishDownload(file);
      expect(controller.downloads.length).toBe(0);
    });

    // Fail a download
    it('should fail a download', function() {
      jest.spyOn($utilities, "timeout").mockImplementation((fn) => fn());
      $utilities.timeout.cancel = jest.fn();
      jest.spyOn($actionController, 'sendMessage');
      let file = {};
      controller.startDownload(file);
      expect(controller.downloads.length).toBe(1);
      controller.failDownload(file);
      expect(controller.downloads.length).toBe(0);
      expect($actionController.sendMessage).toHaveBeenCalled();
    });

    // Trigger scope events
    it('should trigger scope events', function() {
      const file = {};

      scope.$emit("download-file", file);

      expect(controller.downloads).toEqual([file]);
      expect(file.index).toBe(0);
    });
  });
});
