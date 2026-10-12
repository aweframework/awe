import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {uploaderInputTemplate} from "../../../main/resources/js/awe/services/uploader";

describe('awe-framework/awe-client-angular/src/test/jest/services/uploader.js', function () {
  let $injector, $rootScope, $settings, $control, $uploader, $upload, $utilities;
  let scope, uploader, controller;

  const makeFile = (name, type, size = 10) => {
    const file = new File(["x"], name, {type});
    Object.defineProperty(file, "size", {value: size});
    return file;
  };

  const init = (controllerAttributes = {}) => {
    controller = {id: "tutu", ...controllerAttributes};
    scope = $rootScope.$new();
    scope.view = "report";
    scope.context = "contexto";
    uploader = new $uploader(scope, "tutu", {});
    uploader.updateClasses = () => null;
    uploader.controller = controller;
    uploader.model = {};
    jest.spyOn($control, "getAddressModel").mockReturnValue({report: {values: [], selected: []}});
    jest.spyOn($control, "getAddressController").mockReturnValue(controller);
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    uploader.asUploader();
  };

  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $settings = $injector.get('AweSettings');
      $control = $injector.get('Control');
      $uploader = $injector.get('Uploader');
      $upload = $injector.get('Upload');
      $utilities = $injector.get('AweUtilities');
    }]);

    jest.spyOn($settings, "get").mockReturnValue(1);
    jest.spyOn($upload, "upload").mockReturnValue({then: jest.fn()});
    global.URL.createObjectURL = jest.fn((file) => `blob:${file.name}`);
    global.URL.revokeObjectURL = jest.fn();
  });

  afterEach(function () {
    jest.restoreAllMocks();
  });

  describe('template', function () {
    const root = () => $("<div></div>").html(uploaderInputTemplate);

    it('has a drop area that is only enabled through the dropZone attribute', function () {
      const zone = root().find("[ngf-drop]");
      expect(zone.length).toBe(1);
      expect(zone.attr("ngf-drop-disabled")).toBe("!dropZoneEnabled()");
      expect(zone.attr("ngf-change")).toBe("chooseFile($files)");
    });

    it('exposes the drop zone, the preview and the capture button to the browser tests', function () {
      const html = root();
      expect(html.find("[ng-attr-data-testid]").attr("ng-attr-data-testid")).toContain("upload-drop-zone");
      expect(html.find("img[data-testid='upload-preview']").length).toBe(1);
      expect(html.find("button[data-testid='upload-capture']").length).toBe(1);
    });

    it('captures with the camera through its own file input, not the choose button', function () {
      const button = root().find("button[data-testid='upload-capture']");
      expect(button.attr("ngf-select")).toBeDefined();
      expect(button.attr("ngf-accept")).toBe("'image/*'");
      expect(button.attr("ngf-capture")).toBe("captureMode()");
      expect(button.parents("[ngf-select]").length).toBe(0);
    });

    it('never injects the file name as html', function () {
      const preview = root().find("img[data-testid='upload-preview']");
      expect(preview.attr("ng-src")).toBe("{{component.previewUrl}}");
      expect(preview.attr("ng-attr-alt")).toBe("{{component.previewName}}");
    });
  });

  describe('drop zone and capture options', function () {
    it('does not enable the drop zone by default', function () {
      init();
      expect(scope.dropZoneEnabled()).toBe(false);
      expect(scope.captureMode()).toBeNull();
    });

    it('enables the drop zone while the criterion is empty and editable', function () {
      init({dropZone: true});
      uploader.model.selected = null;
      expect(scope.dropZoneEnabled()).toBe(true);

      uploader.uploading = true;
      expect(scope.dropZoneEnabled()).toBe(false);
      uploader.uploading = false;

      uploader.model.selected = "file.png";
      expect(scope.dropZoneEnabled()).toBe(false);

      uploader.model.selected = null;
      controller.readonly = true;
      expect(scope.dropZoneEnabled()).toBe(false);
    });

    it('only accepts the user and environment capture modes', function () {
      init({capture: "environment"});
      expect(scope.captureMode()).toBe("environment");
      init({capture: "user"});
      expect(scope.captureMode()).toBe("user");
      init({capture: "bogus"});
      expect(scope.captureMode()).toBeNull();
    });

    it('hides the capture mode when the criterion is read only', function () {
      init({capture: "environment", readonly: true});
      expect(scope.captureMode()).toBeNull();
    });
  });

  describe('preview', function () {
    beforeEach(function () {
      init({dropZone: true});
    });

    it('previews a raster image and uploads it', function () {
      scope.chooseFile([makeFile("<b>a</b>.png", "image/png")]);

      expect($upload.upload).toHaveBeenCalledTimes(1);
      expect(uploader.previewUrl).toBe("blob:<b>a</b>.png");
      expect(uploader.previewName).toBe("<b>a</b>.png");
    });

    it('does not preview documents or svg images', function () {
      scope.chooseFile([makeFile("a.pdf", "application/pdf")]);
      scope.chooseFile([makeFile("a.svg", "image/svg+xml")]);

      expect($upload.upload).toHaveBeenCalledTimes(2);
      expect(uploader.previewUrl).toBeNull();
      expect(global.URL.createObjectURL).not.toHaveBeenCalled();
    });

    it('revokes the previous preview when another file is chosen', function () {
      scope.chooseFile([makeFile("a.png", "image/png")]);
      scope.chooseFile([makeFile("b.png", "image/png")]);

      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith("blob:a.png");
      expect(uploader.previewUrl).toBe("blob:b.png");
    });

    it('revokes the preview when the uploader is cleared', function () {
      scope.chooseFile([makeFile("a.png", "image/png")]);
      uploader.clearUploader();

      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith("blob:a.png");
      expect(uploader.previewUrl).toBeNull();
    });

    it('revokes the preview when the scope is destroyed', function () {
      scope.chooseFile([makeFile("a.png", "image/png")]);
      scope.$destroy();

      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith("blob:a.png");
    });

    it('keeps no preview when the upload fails', function () {
      scope.chooseFile([makeFile("a.png", "image/png")]);
      jest.spyOn($utilities, "manageRestError").mockReturnValue([]);
      uploader.modelChange = jest.fn();
      const onError = $upload.upload.mock.results[0].value.then.mock.calls[0][1];
      onError({status: 500, statusText: "error"});

      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith("blob:a.png");
      expect(uploader.previewUrl).toBeNull();
    });
  });
});
