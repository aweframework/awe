import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/plugins/uiModal.js', function () {
  let $rootScope, $compile, $timeout;

  beforeEach(function () {
    angular.mock.module('aweApplication');
    inject(["$rootScope", "$compile", "$httpBackend", "$timeout", function (_$rootScope_, _$compile_, $httpBackend, _$timeout_) {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $timeout = _$timeout_;
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

  // Bootstrap closes a modal with Escape only when the keyboard focus is inside it
  describe("keyboard and focus", () => {
    let opener;

    beforeEach(() => {
      opener = document.createElement("button");
      document.body.appendChild(opener);
      opener.focus();
    });

    afterEach(() => {
      $(".modal").modal("hide");
      document.body.innerHTML = "";
      $(document.body).removeClass("modal-open");
    });

    const compileDialog = (content, attributes = "") => {
      const scope = $rootScope.$new();
      scope.closed = jest.fn();
      const element = $compile(`<div class='modal' ui-modal on-close='closed()' ${attributes}>${content}</div>`)(scope);
      document.body.appendChild(element[0]);
      $rootScope.$digest();
      return {element, closed: scope.closed};
    };
    const CLOSABLE = "<div class='modal-header'><button type='button' class='close' data-dismiss='modal'>x</button></div>"
      + "<div class='modal-body'><input type='text'></div>";
    const open = () => {
      $rootScope.$broadcast("modalChange", true);
      $rootScope.$digest();
    };
    const pressEscape = () => document.activeElement.dispatchEvent(new window.KeyboardEvent("keydown",
      {key: "Escape", keyCode: 27, which: 27, bubbles: true, cancelable: true}));

    it("moves the focus to the first control of the modal once it is shown", () => {
      const {element} = compileDialog(CLOSABLE);

      open();

      expect(element.attr("data-open")).toBe("true");
      expect(document.activeElement).toBe(element.find("button.close")[0]);
    });

    it("focuses the modal itself when it has no control", () => {
      const {element} = compileDialog("<div class='modal-body'>text</div>");

      open();

      expect(document.activeElement).toBe(element[0]);
      expect(element.attr("tabindex")).toBe("-1");
    });

    it("closes a modal with a close button when Escape is pressed, as its close button does", () => {
      const {element, closed} = compileDialog(CLOSABLE);
      open();

      pressEscape();
      $timeout.flush();

      expect(element.attr("data-open")).toBe("false");
      expect(closed).toHaveBeenCalledTimes(1);
    });

    it("gives the focus back to the control that opened it once it is closed", () => {
      const {element} = compileDialog(CLOSABLE);
      open();
      expect(element[0].contains(document.activeElement)).toBe(true);

      pressEscape();

      expect(document.activeElement).toBe(opener);
    });

    it("does not close a modal without a close button when Escape is pressed", () => {
      const {element, closed} = compileDialog("<div class='modal-body'><input type='text'></div>");
      open();
      expect(document.activeElement).toBe(element.find("input")[0]);

      pressEscape();
      $timeout.flush();

      expect(element.attr("data-open")).toBe("true");
      expect(closed).not.toHaveBeenCalled();
    });

    it("does not take the close button of a nested modal as its own", () => {
      const {element} = compileDialog(`<div class='modal-body'><input type='text'>
        <div class='modal'><button type='button' data-dismiss='modal'>x</button></div></div>`);
      open();
      expect(document.activeElement).toBe(element.find("input")[0]);

      pressEscape();

      expect(element.attr("data-open")).toBe("true");
    });

    it("does not trap Tab unless asked to", () => {
      const {element} = compileDialog(CLOSABLE);
      open();
      const last = element.find("input")[0];
      last.focus();

      const tab = new window.KeyboardEvent("keydown", {key: "Tab", bubbles: true, cancelable: true});
      last.dispatchEvent(tab);

      expect(tab.defaultPrevented).toBe(false);
      expect(document.activeElement).toBe(last);
    });
  });
});
