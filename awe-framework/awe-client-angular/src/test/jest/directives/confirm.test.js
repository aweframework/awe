import fs from "fs";
import path from "path";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

const CONFIRM_TEMPLATE_URL = "test-confirm-template";
const CONFIRM_TEMPLATE = fs.readFileSync(path.join(__dirname, "../../../main/resources/templates/angular/confirm.html"), "utf8");
const hook = testId => `[data-testid='${testId}']`;

// The confirm dialog closes with Escape as with its cancel button (#885): the focus moves into it when it is shown
// and goes back to the control which opened it when it is hidden
describe("aweConfirm keyboard and focus", () => {
  let $rootScope, $compile, $timeout, actionController, opener;

  beforeEach(() => {
    actionController = {finishAction: jest.fn()};
    angular.mock.module("aweApplication", {
      ServerData: {getAngularTemplateUrl: jest.fn(() => CONFIRM_TEMPLATE_URL)},
      ActionController: actionController
    });
    inject(["$rootScope", "$compile", "$httpBackend", "$templateCache", "$timeout",
      (_$rootScope_, _$compile_, $httpBackend, $templateCache, _$timeout_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
        $templateCache.put(CONFIRM_TEMPLATE_URL, CONFIRM_TEMPLATE);
      }]);
    opener = document.createElement("button");
    document.body.appendChild(opener);
    opener.focus();
  });

  afterEach(() => {
    $(".modal").each((index, modal) => disposeModal(modal));
    document.body.innerHTML = "";
    $(document.body).removeClass("modal-open");
  });

  const openConfirm = () => {
    const scope = $rootScope.$new();
    const element = $compile("<div><awe-confirm></awe-confirm></div>")(scope);
    document.body.appendChild(element[0]);
    $rootScope.$digest();
    scope.confirmAction = {id: "confirm-action"};
    $rootScope.$digest();
    return element.find(hook(TestIds.confirmDialog));
  };
  const pressEscape = () => document.activeElement.dispatchEvent(new window.KeyboardEvent("keydown",
    {key: "Escape", keyCode: 27, which: 27, bubbles: true, cancelable: true}));

  it("moves the focus into the confirm once it is shown", () => {
    const dialog = openConfirm();

    expect(dialog.attr("data-open")).toBe("true");
    expect(document.activeElement).toBe(dialog.find(hook(TestIds.confirmCancel))[0]);
  });

  it("rejects the confirm when Escape is pressed, as the cancel button does", () => {
    const dialog = openConfirm();

    pressEscape();
    $timeout.flush();

    expect(dialog.attr("data-open")).toBe("false");
    expect(actionController.finishAction).toHaveBeenCalledWith({id: "confirm-action"}, false);
  });

  it("gives the focus back to the control that opened it once it is closed", () => {
    const dialog = openConfirm();
    expect(dialog[0].contains(document.activeElement)).toBe(true);

    pressEscape();

    expect(document.activeElement).toBe(opener);
  });

  it("still accepts the confirm with the accept button", () => {
    const dialog = openConfirm();

    dialog.find(hook(TestIds.confirmAccept)).trigger("click");
    $timeout.flush();

    expect(actionController.finishAction).toHaveBeenCalledWith({id: "confirm-action"}, true);
  });
});
