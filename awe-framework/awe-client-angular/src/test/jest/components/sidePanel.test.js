import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";

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

      it("shows with the native modal: backdrop, focus inside, and closes with its close button giving the focus back", () => {
        const element = compile("awe-side-panel");
        const panel = element.find(hook(TestIds.sidePanel));

        element.isolateScope().$broadcast("modalChange", true);
        expect(panel.hasClass("in")).toBe(true);
        expect($(".modal-backdrop").length).toBe(1);
        expect(panel[0].contains(document.activeElement)).toBe(true);

        panel.find(hook(TestIds.sidePanelClose)).trigger("click");
        expect(panel.hasClass("in")).toBe(false);
        expect($(".modal-backdrop").length).toBe(0);
        expect(panel.attr("data-open")).toBe("false");
        expect(document.activeElement).toBe(opener);
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

  // Identity header (#793 S18): while exactly one row of header-grid is selected, the header shows that row
  describe("identity header", () => {
    const HEADER = {headerGrid: "GrdUsr", headerTitleField: "FulNam", headerSubtitleField: "UsrPro", headerAvatarField: "Ini",
      headerAvatarToneField: "IniTon", headerTags: [
        {field: "StaTxt", toneField: "StaTon"},
        {field: "2faHdr", toneField: "2faTon", iconField: "2faIcn"}
      ]};
    const ROWS = () => [
      {id: "1", FulNam: "Javier García", UsrPro: "jgarcia · Operator", Ini: "Javier García", IniTon: "warning",
        StaTxt: "Locked", StaTon: "danger", "2faHdr": "Two-factor on", "2faTon": "neutral", "2faIcn": "lucide:shield-check"},
      {id: "2", FulNam: {value: "ana", label: "Ana Ruiz"}, UsrPro: "ana", Ini: "Ana Ruiz", IniTon: "unknown",
        StaTxt: "Active", StaTon: "success", "2faHdr": "", "2faTon": "neutral", "2faIcn": ""}
    ];
    let grid;

    const compileWith = (selected, extra = HEADER) => {
      grid = {values: ROWS(), selected};
      const element = compile("awe-side-panel", extra);
      refs.$control.getAddressModel.mockImplementation(address => address.component === "GrdUsr" ? grid : {values: []});
      refs.$rootScope.$digest();
      return element;
    };
    const header = element => element.find(".modal-header");
    const dialog = element => element.find(hook(TestIds.sidePanel));

    it("shows the avatar, the title, the subtitle and the tags of the selected row", () => {
      const element = compileWith(["1"]);

      const identity = header(element).find(".panel-identity");
      expect(identity.length).toBe(1);
      expect(header(element).find(".modal-title").length).toBe(0);
      const avatar = identity.find(".panel-identity-avatar");
      expect(avatar.text()).toBe("JG");
      expect(avatar.hasClass("awe-cell-avatar")).toBe(true);
      expect(avatar.hasClass("awe-cell-avatar-warning")).toBe(true);
      expect(avatar.attr("aria-hidden")).toBe("true");
      const title = identity.find(".panel-identity-title");
      expect(title.text()).toBe("Javier García");
      expect(title.attr("id")).toBe("Pnl-title");
      expect(dialog(element).attr("aria-labelledby")).toBe("Pnl-title");
      expect(identity.find(".panel-identity-subtitle").text()).toBe("jgarcia · Operator");
      const tags = identity.find(".panel-identity-tags > .awe-cell-status");
      expect(tags.length).toBe(2);
      expect(tags.eq(0).text().trim()).toBe("Locked");
      expect(tags.eq(0).hasClass("awe-cell-status-danger")).toBe(true);
      expect(tags.eq(0).hasClass("awe-cell-status-plain")).toBe(false);
      expect(tags.eq(1).text().trim()).toBe("Two-factor on");
      expect(tags.eq(1).hasClass("awe-cell-status-plain")).toBe(true);
      expect(tags.eq(1).find(".awe-cell-icon[aria-hidden='true'] use").attr("href")).toContain("shield-check");
    });

    it("marks the title, the subtitle and each tag of the identity header and the title of the label for the tests", () => {
      const element = compileWith(["1"]);

      expect(header(element).find(hook(TestIds.sidePanelTitle)).text()).toBe("Javier García");
      expect(header(element).find(hook(TestIds.sidePanelSubtitle)).text()).toBe("jgarcia · Operator");
      const tags = header(element).find(hook(TestIds.sidePanelTag));
      expect(tags.length).toBe(2);
      expect(tags.eq(0).text().trim()).toBe("Locked");
      expect(tags.eq(1).text().trim()).toBe("Two-factor on");

      const label = compileWith([]);
      expect(header(label).find(hook(TestIds.sidePanelTitle)).text()).toContain("PANEL_TITLE");
      expect(header(label).find(hook(TestIds.sidePanelSubtitle)).length).toBe(0);
      expect(header(label).find(hook(TestIds.sidePanelTag)).length).toBe(0);
    });

    it("shows the label and the icon of the panel with no selected row or several", () => {
      [null, [], ["1", "2"]].forEach(selected => {
        const element = compileWith(selected);

        expect(header(element).find(".panel-identity").length).toBe(0);
        expect(header(element).find(".modal-title").text()).toContain("PANEL_TITLE");
        expect(header(element).find(".modal-title .panel-title-icon").length).toBe(1);
        expect(header(element).find(".modal-title").attr("id")).toBe("Pnl-title");
        expect(dialog(element).attr("aria-labelledby")).toBe("Pnl-title");
      });
    });

    it("follows the selected row while it is open, hiding the tags with an empty value", () => {
      const element = compileWith(["1"]);

      grid.selected = ["2"];
      refs.$rootScope.$digest();

      const identity = header(element).find(".panel-identity");
      expect(identity.find(".panel-identity-title").text()).toBe("Ana Ruiz");
      expect(identity.find(".panel-identity-avatar").text()).toBe("AR");
      expect(identity.find(".panel-identity-avatar").hasClass("awe-cell-avatar-neutral")).toBe(true);
      const tags = identity.find(".panel-identity-tags > .awe-cell-status");
      expect(tags.length).toBe(1);
      expect(tags.eq(0).text().trim()).toBe("Active");
      expect(tags.eq(0).hasClass("awe-cell-status-success")).toBe(true);

      grid.selected = null;
      refs.$rootScope.$digest();
      expect(header(element).find(".panel-identity").length).toBe(0);
      expect(header(element).find(".modal-title").text()).toContain("PANEL_TITLE");
    });

    it("does not change without header-grid, even with a selected row in the screen", () => {
      const element = compileWith(["1"], {});

      expect(header(element).find(".panel-identity").length).toBe(0);
      expect(header(element).find(".modal-title").text()).toContain("PANEL_TITLE");
      expect(header(element).find(".modal-title").attr("id")).toBeUndefined();
      expect(dialog(element).attr("aria-labelledby")).toBeUndefined();
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

    describe("keyboard and focus", () => {
      let opener;

      beforeEach(() => {
        opener = document.createElement("button");
        document.body.appendChild(opener);
        opener.focus();
      });

      afterEach(() => {
        $(".modal").each((index, modal) => disposeModal(modal));
        $(document.body).removeClass("modal-open");
      });

      // The dialog shown by Bootstrap, as the delayed modalChange of the opening does
      const open = (extra = {}) => {
        const element = compile("awe-dialog", {accept: false, acceptOnClose: false, openAction: "open", ...extra});
        changeController(element, {opened: true});
        refs.$rootScope.$broadcast("modalChange", true);
        refs.$rootScope.$digest();
        return element.find(hook(TestIds.dialog));
      };
      const pressEscape = () => document.activeElement.dispatchEvent(new window.KeyboardEvent("keydown",
        {key: "Escape", keyCode: 27, which: 27, bubbles: true, cancelable: true}));

      it("moves the focus into the dialog once it is shown", () => {
        const dialog = open();

        expect(document.activeElement).toBe(dialog.find(hook(TestIds.dialogClose))[0]);
      });

      it("closes with Escape as with its close button, and gives the focus back to the opener", () => {
        open();

        pressEscape();

        expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
        expect(document.activeElement).toBe(opener);
      });

      it("does not close with Escape when it has no close button (no title)", () => {
        const dialog = open({label: null});
        expect(dialog[0].contains(document.activeElement)).toBe(true);

        pressEscape();

        expect(dialog.attr("data-open")).toBe("true");
        expect($actionController.finishAction).not.toHaveBeenCalled();
      });
    });

    it("keeps its modal markup", () => {
      const dialog = compile("awe-dialog").find(hook(TestIds.dialog));

      expect(dialog.hasClass("modal")).toBe(true);
      expect(dialog.hasClass("side-panel")).toBe(false);
    });
  });
});
