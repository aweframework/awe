import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/plugins/uiModal.js', function () {
  let $rootScope, $compile;

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$rootScope", "$compile", "$httpBackend", function (_$rootScope_, _$compile_, $httpBackend) {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend.when('POST', 'settings').respond(DefaultSettings);
    }]);
  });

  const compileModal = () => {
    const element = $compile("<div class='modal' ui-modal></div>")($rootScope.$new());
    $rootScope.$digest();
    return element;
  };

  it("exposes the dialog as closed as soon as it is linked", () => {
    expect(compileModal().attr("data-open")).toBe("false");
  });

  it("exposes the dialog as open when Bootstrap has shown it", () => {
    const element = compileModal();

    element.trigger("shown.bs.modal");

    expect(element.attr("data-open")).toBe("true");
  });

  it("exposes the dialog as closed when Bootstrap has hidden it (backdrop already removed)", () => {
    const element = compileModal();
    element.trigger("shown.bs.modal");

    element.trigger("hidden.bs.modal");

    expect(element.attr("data-open")).toBe("false");
  });
});
