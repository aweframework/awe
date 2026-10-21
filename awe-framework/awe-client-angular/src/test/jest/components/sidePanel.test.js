import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";

const hook = testId => `[data-testid='${testId}']`;

describe("awe-framework/awe-client-angular/src/test/jest/components/sidePanel.test.js", () => {
  let refs, $actionController;

  const compile = (tag, extra = {}) => {
    const component = tag === "awe-side-panel" ? "side-panel" : "dialog";
    stubPanelComponent(refs, {values: []}, createPanelController("Pnl", component, {label: "PANEL_TITLE", ...extra}));
    const attribute = tag === "awe-side-panel" ? "side-panel-id" : "dialog-id";
    const element = refs.$compile(`<${tag} ${attribute}='Pnl'><button>inside</button></${tag}>`)(refs.$rootScope.$new());
    document.body.appendChild(element[0]);
    refs.$rootScope.$digest();
    return element;
  };
  const changeController = (element, controller) => {
    element.isolateScope().$broadcast("controllerChange", {address: element.isolateScope().component.address, controller});
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", (_ActionController_) => {
      $actionController = _ActionController_;
    }]);
    jest.spyOn($actionController, "addStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "removeStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "addActionList").mockImplementation(jest.fn());
    jest.spyOn($actionController, "finishAction").mockImplementation(jest.fn());
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  describe("side panel", () => {
    it("is a modal anchored to the right edge by default, marked with its owner", () => {
      const panel = compile("awe-side-panel").find(hook(TestIds.sidePanel));

      expect(panel.length).toBe(1);
      expect(panel.hasClass("modal")).toBe(true);
      expect(panel.hasClass("side-panel")).toBe(true);
      expect(panel.hasClass("side-panel-right")).toBe(true);
      expect(panel.attr("data-testid-owner")).toBe("Pnl");
    });

    it("is anchored to the left edge with position left", () => {
      const panel = compile("awe-side-panel", {position: "left"}).find(hook(TestIds.sidePanel));

      expect(panel.hasClass("side-panel-left")).toBe(true);
      expect(panel.hasClass("side-panel-right")).toBe(false);
    });

    it("has a header with the title and a close button, and its children in the body", () => {
      const element = compile("awe-side-panel");

      expect(element.find(".modal-header .modal-title").text()).toContain("PANEL_TITLE");
      expect(element.find(`.modal-header button.close${hook(TestIds.sidePanelClose)}`).length).toBe(1);
      expect(element.find(".side-panel-body button").text()).toBe("inside");
    });

    it("is as wide as the screen up to its maximum width", () => {
      const dialog = compile("awe-side-panel", {maxWidth: "30rem"}).find(".modal-dialog");

      expect(dialog[0].style.maxWidth).toBe("30rem");
    });

    it("keeps a default maximum width", () => {
      const dialog = compile("awe-side-panel").find(".modal-dialog");

      expect(dialog[0].style.maxWidth).toBe("40rem");
    });

    it("opens as a dialog: a new action stack and the modal shown", () => {
      const element = compile("awe-side-panel");

      changeController(element, {opened: true});

      expect($actionController.addStack).toHaveBeenCalledTimes(1);
      expect(refs.$utilities.publishDelayedFromScope).toHaveBeenCalledWith("modalChange", true, expect.anything());
    });

    it("closes as a dialog: the stack removed and the opening action finished with the accept flag", () => {
      const element = compile("awe-side-panel", {accept: false, acceptOnClose: true, openAction: "open"});
      changeController(element, {opened: true});

      changeController(element, {opened: false});

      expect(refs.$utilities.publishDelayedFromScope).toHaveBeenCalledWith("modalChange", false, expect.anything());
      expect($actionController.removeStack).toHaveBeenCalledTimes(1);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
    });

    describe("focus", () => {
      let opener;

      beforeEach(() => {
        opener = document.createElement("button");
        document.body.appendChild(opener);
        opener.focus();
      });

      it("moves the focus inside once it is shown, and keeps Tab inside", () => {
        const panel = compile("awe-side-panel").find(hook(TestIds.sidePanel));

        panel.trigger("show.bs.modal");
        panel.trigger("shown.bs.modal");
        expect(panel[0].contains(document.activeElement)).toBe(true);

        const last = panel.find("button").last()[0];
        last.focus();
        last.dispatchEvent(new window.KeyboardEvent("keydown", {key: "Tab", bubbles: true, cancelable: true}));
        expect(document.activeElement).toBe(panel.find("button").first()[0]);
      });

      it("gives the focus back to the control that opened it once it is hidden", () => {
        const panel = compile("awe-side-panel").find(hook(TestIds.sidePanel));
        panel.trigger("show.bs.modal");
        panel.trigger("shown.bs.modal");

        panel.trigger("hidden.bs.modal");

        expect(document.activeElement).toBe(opener);
      });
    });
  });

  describe("dialog (shares the opening and closing)", () => {
    it("opens with a new action stack and the modal shown", () => {
      const element = compile("awe-dialog");

      changeController(element, {opened: true});

      expect($actionController.addStack).toHaveBeenCalledTimes(1);
      expect(refs.$utilities.publishDelayedFromScope).toHaveBeenCalledWith("modalChange", true, expect.anything());
    });

    it("closes removing the stack and finishing the opening action with the accept flag", () => {
      const element = compile("awe-dialog", {accept: true, acceptOnClose: true, openAction: "open"});
      changeController(element, {opened: true});

      changeController(element, {opened: false});

      expect($actionController.removeStack).toHaveBeenCalledTimes(1);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", true);
    });

    it("keeps its modal markup", () => {
      const dialog = compile("awe-dialog").find(hook(TestIds.dialog));

      expect(dialog.hasClass("modal")).toBe(true);
      expect(dialog.hasClass("side-panel")).toBe(false);
    });
  });
});
