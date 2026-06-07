import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

describe('awe-framework/awe-client-angular/src/test/jest/services/text.js', function() {
  let $injector, $control, $rootScope, $utilities, Text;

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $rootScope = $injector.get('$rootScope');
      $control = $injector.get('Control');
      $utilities = $injector.get('AweUtilities');
      Text = $injector.get('Text');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  it('should generate a text', function() {
    let $scope = $rootScope.$new();
    $scope.view = "report";
    $scope.context = "contexto";
    let text = new Text($scope, "tutu", {});
    jest.spyOn($control, "getAddressModel").mockReturnValue({values: [], selected: ""});
    jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    jest.spyOn($utilities, "defineModelChangeListeners").mockReturnValue(true);
    text.asText();

    // Assert
    expect(text.asText()).toBe(true);
  });

  describe('once initialized', function() {
    let text;

    // Mock module
    beforeEach(function() {
      let $scope = $rootScope.$new();
      $scope.view = "report";
      $scope.context = "contexto";
      text = new Text($scope, "tutu", {});
      jest.spyOn($control, "getAddressModel").mockReturnValue({values: [], selected: ""});
      jest.spyOn($control, "getAddressController").mockReturnValue({id: "tutu"});
      jest.spyOn($control, "checkComponent").mockReturnValue(true);
      text.asText();
    });

    it('should get visible value \'\'', function() {
      // Change model
      text.model = {selected: null, values: []};
      text.onModelChanged({selected: true});

      // Assert
      expect(text.getVisibleValue()).toBe("");
    });

    it('should change visible value to \'text\'', function() {
      // Change model
      text.model = {selected: "text", values: []};
      text.onModelChanged({selected: true});

      // Assert
      expect(text.getVisibleValue()).toBe("text");
    });

    it('should change visible value to model value text', function() {
      // Change model
      text.model = {selected: "text", values: [{value:"text", label: "This is the right text", icon: "icon", title: "title"}]};
      text.onModelChanged({values: true});

      // Assert
      expect(text.getVisibleValue()).toBe("This is the right text");
    });

    it('should clear selected when values change to an empty list', function() {
      // Change model
      text.model = {selected: "text", values: []};

      // Run
      text.onModelChanged({values: true});

      // Assert
      expect(text.model.selected).toBeNull();
      expect(text.getVisibleValue()).toBe("");
    });
  });
});
