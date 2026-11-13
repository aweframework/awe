import {getCollapse} from "../../../main/resources/js/awe/services/collapse";

describe("awe-framework/awe-client-angular/src/test/jest/services/collapse.test.js", () => {
  let panel, trigger;

  const build = (open = false) => {
    document.body.insertAdjacentHTML("beforeend",
      `<a id="trigger" class="accordion-toggle${open ? "" : " collapsed"}">toggle</a><div id="panel" class="panel-collapse collapse${open ? " in" : ""}">content</div>`);
    panel = $("#panel");
    trigger = $("#trigger");
  };
  const enableTransitions = () => {
    const computed = window.getComputedStyle.bind(window);
    jest.spyOn(window, "getComputedStyle").mockImplementation((element, pseudo) => new Proxy(computed(element, pseudo), {
      get: (style, property) => {
        if (property === "transitionDuration") {
          return element.classList.contains("collapsing") ? "0.35s" : "";
        }
        return typeof style[property] === "function" ? style[property].bind(style) : style[property];
      }
    }));
    Object.defineProperty(window.HTMLElement.prototype, "scrollHeight", {configurable: true, get: () => 120});
  };

  afterEach(() => {
    jest.restoreAllMocks();
    jest.useRealTimers();
    delete window.HTMLElement.prototype.scrollHeight;
    document.body.innerHTML = "";
  });

  describe("without transition", () => {
    it("opens: the panel is collapse in and the trigger is no longer collapsed", () => {
      build(false);

      getCollapse(panel, trigger).setOpen(true);

      expect(panel.attr("class")).toBe("panel-collapse collapse in");
      expect(trigger.hasClass("collapsed")).toBe(false);
      expect(panel[0].style.height).toBe("");
    });

    it("closes: the panel is collapse, the trigger is collapsed and no height is left behind", () => {
      build(true);

      getCollapse(panel, trigger).setOpen(false);

      expect(panel.attr("class")).toBe("panel-collapse collapse");
      expect(trigger.hasClass("collapsed")).toBe(true);
      expect(panel[0].style.height).toBe("");
    });

    it("can be opened again after it was closed (nothing keeps it at height 0)", () => {
      build(true);
      const collapse = getCollapse(panel, trigger);

      collapse.setOpen(false);
      collapse.setOpen(true);

      expect(panel.hasClass("in")).toBe(true);
      expect(panel[0].style.height).toBe("");
    });

    it("does nothing when it already is in the requested state", () => {
      build(true);
      const onDone = jest.fn();

      getCollapse(panel, trigger).setOpen(true, {onDone});

      expect(panel.attr("class")).toBe("panel-collapse collapse in");
      expect(onDone).not.toHaveBeenCalled();
    });

    it("marks the trigger of a panel which already is in the requested state", () => {
      build(false);
      trigger.removeClass("collapsed");

      getCollapse(panel, trigger).setOpen(false);

      expect(trigger.hasClass("collapsed")).toBe(true);
      expect(panel.attr("class")).toBe("panel-collapse collapse");
    });

    it("ignores a missing panel", () => {
      expect(() => getCollapse($(), $()).setOpen(true)).not.toThrow();
    });

    it("calls back when it is done", () => {
      build(false);
      const onDone = jest.fn();

      getCollapse(panel, trigger).setOpen(true, {onDone});

      expect(onDone).toHaveBeenCalledTimes(1);
    });

    it("reuses the instance of a panel", () => {
      build(false);

      expect(getCollapse(panel, trigger)).toBe(getCollapse(panel, trigger));
    });

    it("sets the state at once, without animation, when asked", () => {
      build(false);
      enableTransitions();

      getCollapse(panel, trigger).setOpen(true, {animate: false});

      expect(panel.attr("class")).toBe("panel-collapse collapse in");
    });
  });

  describe("with transition", () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    it("opens growing from 0 to its content height, and ends as collapse in without height", () => {
      build(false);
      enableTransitions();

      getCollapse(panel, trigger).setOpen(true);

      expect(panel.hasClass("collapsing")).toBe(true);
      expect(panel.hasClass("collapse")).toBe(false);
      expect(panel.hasClass("in")).toBe(false);
      expect(panel[0].style.height).toBe("120px");
      expect(trigger.hasClass("collapsed")).toBe(false);

      panel.trigger("transitionend");

      expect(panel.attr("class")).toBe("panel-collapse collapse in");
      expect(panel[0].style.height).toBe("");
    });

    it("ends after 350 ms when the transition never ends", () => {
      build(false);
      enableTransitions();
      const onDone = jest.fn();
      getCollapse(panel, trigger).setOpen(true, {onDone});

      jest.advanceTimersByTime(349);
      expect(panel.hasClass("collapsing")).toBe(true);
      jest.advanceTimersByTime(1);

      expect(panel.attr("class")).toBe("panel-collapse collapse in");
      expect(onDone).toHaveBeenCalledTimes(1);
    });

    it("closes shrinking from its height to 0, and ends as collapse without height", () => {
      build(true);
      enableTransitions();
      jest.spyOn($.fn, "height").mockImplementation(function (value) {
        if (value === undefined) {
          return 120;
        }
        this[0].style.height = typeof value === "number" ? `${value}px` : value;
        return this;
      });

      getCollapse(panel, trigger).setOpen(false);

      expect(panel.hasClass("collapsing")).toBe(true);
      expect(panel.hasClass("in")).toBe(false);
      expect(trigger.hasClass("collapsed")).toBe(true);
      expect(panel[0].style.height).toBe("0px");

      panel.trigger("transitionend");

      expect(panel.attr("class")).toBe("panel-collapse collapse");
      expect(panel[0].style.height).toBe("");
    });

    it("ignores the transitions of its content", () => {
      build(false);
      enableTransitions();
      panel.append("<span id='inner'></span>");
      getCollapse(panel, trigger).setOpen(true);

      $("#inner").trigger("transitionend");

      expect(panel.hasClass("collapsing")).toBe(true);
    });

    it("finishes the running transition at once when the state is asked to change again", () => {
      build(false);
      enableTransitions();
      const collapse = getCollapse(panel, trigger);
      collapse.setOpen(true);

      collapse.setOpen(false);

      // The opening was completed (collapse in) and the closing started from there
      expect(panel.hasClass("in")).toBe(false);
      expect(panel.hasClass("collapsing")).toBe(true);
      expect(trigger.hasClass("collapsed")).toBe(true);

      jest.advanceTimersByTime(350);
      expect(panel.attr("class")).toBe("panel-collapse collapse");
    });

    it("does not end twice when the transition ends and the timer fires", () => {
      build(false);
      enableTransitions();
      const onDone = jest.fn();
      getCollapse(panel, trigger).setOpen(true, {onDone});

      panel.trigger("transitionend");
      jest.advanceTimersByTime(500);

      expect(onDone).toHaveBeenCalledTimes(1);
    });
  });
});
