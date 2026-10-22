import {
  cleanupHeavyComponentTest,
  createPanelController,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";
import {TestAttributes, TestIds} from "../../../main/resources/js/awe/data/testIds";
import fs from "fs";
import path from "path";

const hook = testId => `[data-testid='${testId}']`;

// Anchor at (200, 100) of 100x30 and a popover of 160x80 in the jsdom 1024x768 window
const ANCHOR_RECT = {top: 100, bottom: 130, left: 200, right: 300, width: 100, height: 30, x: 200, y: 100};
const POPOVER_RECT = {top: 0, bottom: 80, left: 0, right: 160, width: 160, height: 80, x: 0, y: 0};

describe("awe-framework/awe-client-angular/src/test/jest/components/popover.test.js", () => {
  let refs, $actionController, anchor;

  const compile = (extra = {}, parent = document.body) => {
    stubPanelComponent(refs, {values: []}, createPanelController("Pop", "popover", {
      label: "POPOVER_TITLE", anchor: "Anc", accept: false, acceptOnClose: false, openAction: "open", ...extra
    }));
    const element = refs.$compile(`<awe-popover popover-id='Pop'><button class='inside'>inside</button><div class='modal-footer'><button class='footer'>footer</button></div></awe-popover>`)(refs.$rootScope.$new());
    parent.appendChild(element[0]);
    refs.$rootScope.$digest();
    const popover = $(document.body).find(hook(TestIds.popover));
    if (popover[0]) {
      popover[0].getBoundingClientRect = () => POPOVER_RECT;
    }
    return element;
  };
  const getPopover = () => $(document.body).find(hook(TestIds.popover));
  const changeController = (element, controller) => {
    element.isolateScope().$broadcast("controllerChange", {address: element.isolateScope().component.address, controller});
  };
  const open = element => changeController(element, {opened: true});
  const isOpen = () => getPopover().attr(TestAttributes.open) === "true";
  const keyDown = (target, key, shiftKey = false) =>
    target.dispatchEvent(new window.KeyboardEvent("keydown", {key, shiftKey, bubbles: true, cancelable: true}));
  const mouseDown = target => target.dispatchEvent(new window.MouseEvent("mousedown", {bubbles: true, cancelable: true}));

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ActionController", (_ActionController_) => {
      $actionController = _ActionController_;
    }]);
    jest.spyOn($actionController, "addStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "removeStack").mockImplementation(jest.fn());
    jest.spyOn($actionController, "addActionList").mockImplementation(jest.fn());
    jest.spyOn($actionController, "finishAction").mockImplementation(jest.fn());

    // The control the popover is anchored to (a button renders its id on the button element)
    anchor = document.createElement("button");
    anchor.id = "Anc";
    anchor.getBoundingClientRect = () => ANCHOR_RECT;
    document.body.appendChild(anchor);
    anchor.focus();
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  describe("markup", () => {
    it("is a closed dialog marked with its owner, with a header with the title and a close button", () => {
      compile();
      const popover = getPopover();

      expect(popover.length).toBe(1);
      expect(popover.hasClass("awe-popover")).toBe(true);
      expect(popover.attr("role")).toBe("dialog");
      expect(popover.attr(TestAttributes.owner)).toBe("Pop");
      expect(popover.attr(TestAttributes.open)).toBe("false");
      expect(popover[0].hidden).toBe(true);
      expect(popover.find(".awe-popover-header .awe-popover-title").text()).toContain("POPOVER_TITLE");
      expect(popover.find(`.awe-popover-header button${hook(TestIds.popoverClose)}`).length).toBe(1);
    });

    it("holds its children in the body and its footer at the end", () => {
      compile();
      const popover = getPopover();

      expect(popover.find(".awe-popover-body button.inside").text()).toBe("inside");
      expect(popover.find(".awe-popover-body .modal-footer button.footer").text()).toBe("footer");
    });

    it("is placed in the page body once it is open, so no container of the screen clips it", () => {
      const element = compile();

      open(element);

      expect(getPopover().parent()[0]).toBe(document.body);
      expect(element.find(hook(TestIds.popover)).length).toBe(0);
    });

    it("is placed in the modal which holds it, so the modal keeps the focus inside", () => {
      const modal = document.createElement("div");
      modal.className = "modal";
      document.body.appendChild(modal);

      open(compile({}, modal));

      expect(getPopover().parent()[0]).toBe(modal);
    });

    it("keeps a default maximum width of 20rem", () => {
      compile();

      expect(getPopover()[0].style.maxWidth).toBe("20rem");
    });

    it("takes its maximum width from max-width", () => {
      compile({maxWidth: "30rem"});

      expect(getPopover()[0].style.maxWidth).toBe("30rem");
    });

    it("is removed from the page when the screen is destroyed", () => {
      const element = compile();

      element.isolateScope().$destroy();

      expect(getPopover().length).toBe(0);
    });
  });

  describe("opening", () => {
    it("opens as a dialog below its anchor by default", () => {
      const element = compile();

      open(element);

      expect($actionController.addStack).toHaveBeenCalledTimes(1);
      expect(isOpen()).toBe(true);
      expect(getPopover()[0].hidden).toBe(false);
      expect(getPopover().attr("data-position")).toBe("bottom");
      // Below the anchor (130 + 8) and centered on it (250 - 80)
      expect(getPopover()[0].style.top).toBe("138px");
      expect(getPopover()[0].style.left).toBe("170px");
    });

    it("opens above its anchor with position top", () => {
      const element = compile({position: "top"});

      open(element);

      expect(getPopover().attr("data-position")).toBe("top");
      // Above the anchor (100 - 8 - 80) and centered on it
      expect(getPopover()[0].style.top).toBe("12px");
      expect(getPopover()[0].style.left).toBe("170px");
    });

    it("opens on the left and on the right of its anchor", () => {
      const left = compile({position: "left"});
      open(left);
      // Left of the anchor (200 - 8 - 160) and vertically centered on it (115 - 40)
      expect(getPopover()[0].style.left).toBe("32px");
      expect(getPopover()[0].style.top).toBe("75px");
      left.isolateScope().$destroy();

      const right = compile({position: "right"});
      open(right);
      expect(getPopover()[0].style.left).toBe("308px");
      expect(getPopover()[0].style.top).toBe("75px");
    });

    it("finds a criterion anchor by its criterion-id", () => {
      anchor.removeAttribute("id");
      const criterion = document.createElement("div");
      criterion.setAttribute("criterion-id", "Anc");
      criterion.getBoundingClientRect = () => ({...ANCHOR_RECT, top: 300, bottom: 330});
      document.body.appendChild(criterion);
      const element = compile();

      open(element);

      expect(getPopover()[0].style.top).toBe("338px");
    });
  });

  describe("closing", () => {
    it("closes with its close button, finishing the opening action", () => {
      const element = compile();
      open(element);

      getPopover().find(hook(TestIds.popoverClose)).trigger("click");

      expect(isOpen()).toBe(false);
      expect(getPopover()[0].hidden).toBe(true);
      expect($actionController.removeStack).toHaveBeenCalledTimes(1);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
    });

    it("closes with a click outside", () => {
      const element = compile();
      open(element);
      const outside = document.createElement("div");
      document.body.appendChild(outside);

      mouseDown(outside);

      expect(isOpen()).toBe(false);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
    });

    it("stays open with a click inside", () => {
      const element = compile();
      open(element);

      mouseDown(getPopover().find("button.inside")[0]);

      expect(isOpen()).toBe(true);
      expect($actionController.finishAction).not.toHaveBeenCalled();
    });

    it("closes with a click on its anchor without opening it again", () => {
      const element = compile();
      open(element);
      const click = jest.fn();
      anchor.addEventListener("click", click);

      mouseDown(anchor);
      anchor.click();

      expect(isOpen()).toBe(false);
      expect(click).not.toHaveBeenCalled();
    });

    it("closes with Esc", () => {
      const element = compile();
      open(element);

      keyDown(getPopover().find("button.inside")[0], "Escape");

      expect(isOpen()).toBe(false);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
    });

    it("closes with a close action (opened false), finishing the opening action with its accept flag", () => {
      const element = compile();
      open(element);

      changeController(element, {opened: false});

      expect(isOpen()).toBe(false);
      expect($actionController.finishAction).toHaveBeenCalledWith("open", false);
    });

    it("opens again right after it is closed, ending the first opening action and keeping the second one", () => {
      // The timeouts run when they are flushed, as in the browser
      const element = compile({openAction: "first"});
      const pending = [];
      refs.$utilities.timeout.mockImplementation(callback => pending.push(callback));
      const flush = () => pending.splice(0).forEach(callback => callback());
      const controller = element.isolateScope().component.controller;
      open(element);

      keyDown(getPopover().find("button.inside")[0], "Escape");
      // The dialog action of the anchor sets the new opening action before opening it
      controller.openAction = "second";
      open(element);
      flush();

      expect(isOpen()).toBe(true);
      expect($actionController.addStack).toHaveBeenCalledTimes(2);
      expect($actionController.removeStack).toHaveBeenCalledTimes(1);
      expect($actionController.finishAction).toHaveBeenCalledTimes(1);
      expect($actionController.finishAction).toHaveBeenCalledWith("first", false);

      keyDown(getPopover().find("button.inside")[0], "Escape");
      flush();

      expect(isOpen()).toBe(false);
      expect($actionController.removeStack).toHaveBeenCalledTimes(2);
      expect($actionController.finishAction).toHaveBeenLastCalledWith("second", false);
    });

    it("stops listening once closed", () => {
      const element = compile();
      open(element);
      keyDown(document.body, "Escape");
      $actionController.finishAction.mockClear();

      keyDown(document.body, "Escape");
      mouseDown(document.body);

      expect($actionController.finishAction).not.toHaveBeenCalled();
    });
  });

  describe("focus", () => {
    it("moves the focus inside once it is open, and keeps Tab inside", () => {
      const element = compile();

      open(element);
      const popover = getPopover();
      expect(popover[0].contains(document.activeElement)).toBe(true);

      const last = popover.find("button").last()[0];
      last.focus();
      keyDown(last, "Tab");
      expect(document.activeElement).toBe(popover.find("button").first()[0]);
    });

    it("gives the focus back to the control that opened it once it is closed", () => {
      const element = compile();
      open(element);

      keyDown(getPopover().find("button.inside")[0], "Escape");

      expect(document.activeElement).toBe(anchor);
    });
  });

  // The Columns and Maximize of a grid inside the popover (#843 L10)
  describe("content with its own layers", () => {
    // The Columns dialog of a grid toolbar: it is in the popover while it is open
    const addNestedDialog = () => $("<div class='grid-columns-dialog' role='dialog'><button class='first'>first</button><button class='last'>last</button></div>")
      .appendTo(getPopover().find(".awe-popover-body"));
    // The Columns side panel of a grid toolbar: a Bootstrap modal shown in the page body
    const addShownModal = () => $("<div class='modal in' role='dialog'><button class='modal-button'>modal</button></div>").appendTo(document.body);

    it("is placed in the maximize target of the screen, so a grid inside is maximized over the content area", () => {
      const view = $("<div class='maximize-target view'><div class='maximize-target window-center'><div class='window'></div></div></div>").appendTo(document.body);
      const content = view.find(".window-center")[0];
      const element = compile({}, view.find(".window")[0]);

      open(element);

      expect(getPopover().parent()[0]).toBe(content);
      expect(getPopover().find("button.inside").parents(".maximize-target:first")[0]).toBe(content);
    });

    it("restores the content maximized inside when it is closed, so the screen is shown again", () => {
      const element = compile();
      open(element);
      const restore = jest.fn();
      angular.element(getPopover().find("button.inside")).scope().$on("restore", restore);

      keyDown(getPopover().find("button.inside")[0], "Escape");

      expect(restore).toHaveBeenCalledTimes(1);
    });

    it("is not placed again while a grid inside is maximized over the screen", () => {
      const element = compile();
      open(element);
      getPopover().addClass("maximizeParent");
      anchor.getBoundingClientRect = () => ({...ANCHOR_RECT, top: 400, bottom: 430});

      window.dispatchEvent(new window.Event("resize"));

      expect(getPopover()[0].style.top).toBe("138px");
    });

    it("keeps Tab inside a dialog open in its content", () => {
      const element = compile();
      open(element);
      const nested = addNestedDialog();
      const last = nested.find("button.last")[0];
      last.focus();

      keyDown(last, "Tab");

      expect(document.activeElement).toBe(nested.find("button.first")[0]);
    });

    it("brings a lost focus back to the dialog open in its content", () => {
      const element = compile();
      open(element);
      const nested = addNestedDialog();
      document.activeElement.blur();

      keyDown(document.body, "Tab");

      expect(document.activeElement).toBe(nested.find("button.first")[0]);
    });

    it("stays open with Esc in a dialog open in its content, and the modal which holds it does not get it", () => {
      const modal = document.createElement("div");
      modal.className = "modal";
      document.body.appendChild(modal);
      const modalKeyDown = jest.fn();
      modal.addEventListener("keydown", modalKeyDown);
      const element = compile({}, modal);
      open(element);
      const nested = addNestedDialog();

      keyDown(nested.find("button.first")[0], "Escape");

      expect(isOpen()).toBe(true);
      expect(modalKeyDown).not.toHaveBeenCalled();
    });

    it("leaves the focus to a modal opened from it (the Columns side panel)", () => {
      const element = compile();
      open(element);
      const button = addShownModal().find("button.modal-button")[0];
      button.focus();
      const tab = new window.KeyboardEvent("keydown", {key: "Tab", bubbles: true, cancelable: true});

      button.dispatchEvent(tab);

      expect(tab.defaultPrevented).toBe(false);
      expect(document.activeElement).toBe(button);
    });

    it("stays open with Esc on the page body while a modal opened from it is shown", () => {
      const element = compile();
      open(element);
      addShownModal();

      keyDown(document.body, "Escape");

      expect(isOpen()).toBe(true);
    });

    it("closes with Esc again once the dialog of its content is closed", () => {
      const element = compile();
      open(element);
      addNestedDialog().remove();

      keyDown(getPopover().find("button.inside")[0], "Escape");

      expect(isOpen()).toBe(false);
    });
  });

  describe("styles", () => {
    const STYLESHEET = path.join(__dirname, "../../../main/resources/less/awe/directives/popover.less");

    it("writes its text with the text color, not with the one of the panel headers (white in some themes)", () => {
      const rule = fs.readFileSync(STYLESHEET, "utf8").match(/^\.awe-popover\s*\{([^]*?)\n {2}&/m)[1];

      expect(rule).toMatch(/(?:^|\s)color:\s*var\(--awe-color-text\);/);
      expect(rule).not.toMatch(/--awe-color-panel-text/);
    });

    it("stays under the modals opened from it (the Columns side panel, 1050 and later in the page)", () => {
      const rule = fs.readFileSync(STYLESHEET, "utf8").match(/^\.awe-popover\s*\{([^]*?)\n {2}&/m)[1];

      expect(Number(rule.match(/z-index:\s*(\d+);/)[1])).toBeLessThanOrEqual(1050);
    });
  });
});
