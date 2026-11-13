import {createMessagePopover} from "../../../main/resources/js/awe/services/messagePopover";
import {popoverTemplate} from "../../../main/resources/js/awe/services/testIds";

describe("awe-framework/awe-client-angular/src/test/jest/services/messagePopover.test.js", () => {
  let target;

  const rect = (element, box) => {
    const {left, top, width, height} = {left: 0, top: 0, width: 0, height: 0, ...box};
    element.getBoundingClientRect = () => ({left, top, width, height, right: left + width, bottom: top + height});
  };
  const create = (options = {}) => createMessagePopover(target, {
    template: popoverTemplate("danger", "Field"),
    title: "Title",
    content: "Content",
    ...options
  });
  const tip = () => $(document.body).children(".popover");

  // The page is a 1000 x 800 viewport and the popover is 200 x 100
  const layout = (targetBox, popoverSize = [200, 100]) => {
    Object.defineProperty(document.documentElement, "clientWidth", {configurable: true, value: 1000});
    Object.defineProperty(document.documentElement, "clientHeight", {configurable: true, value: 800});
    Object.defineProperty(window, "innerWidth", {configurable: true, value: 1000});
    Object.defineProperty(window, "innerHeight", {configurable: true, value: 800});
    rect(document.body, {left: 0, top: 0, width: 1000, height: 800});
    rect(target[0], targetBox);
    // The popover is where its CSS puts it (jsdom does not lay anything out)
    const boundingRect = window.HTMLElement.prototype.getBoundingClientRect;
    jest.spyOn(window.HTMLElement.prototype, "getBoundingClientRect").mockImplementation(function () {
      if (!this.classList.contains("popover")) {
        return boundingRect.call(this);
      }
      const left = parseFloat(this.style.left) || 0;
      const top = parseFloat(this.style.top) || 0;
      return {left, top, width: popoverSize[0], height: popoverSize[1], right: left + popoverSize[0], bottom: top + popoverSize[1]};
    });
    jest.spyOn(window.HTMLElement.prototype, "offsetWidth", "get").mockImplementation(function () {
      return this.classList.contains("popover") ? popoverSize[0] : 0;
    });
    jest.spyOn(window.HTMLElement.prototype, "offsetHeight", "get").mockImplementation(function () {
      return this.classList.contains("popover") ? popoverSize[1] : 0;
    });
  };

  beforeEach(() => {
    document.body.insertAdjacentHTML("beforeend", "<input id='Field'/>");
    target = $("#Field");
  });

  afterEach(() => {
    jest.restoreAllMocks();
    document.body.innerHTML = "";
  });

  describe("markup", () => {
    it("is appended to the body with the template, the title and the content", () => {
      create().show();

      expect(tip().length).toBe(1);
      expect(tip().attr("role")).toBe("tooltip");
      expect(tip().find(".arrow").length).toBe(1);
      expect(tip().find(".popover-title").text()).toBe("Title");
      expect(tip().find(".popover-content").text()).toBe("Content");
    });

    it("keeps the test hooks of the template", () => {
      create().show();

      expect(tip().attr("data-testid")).toBe("popover");
      expect(tip().attr("data-type")).toBe("danger");
      expect(tip().attr("data-testid-owner")).toBe("Field");
      expect(tip().find(".popover-title").attr("data-testid")).toBe("popover-title");
      expect(tip().find(".popover-content").attr("data-testid")).toBe("popover-content");
    });

    it("is displayed with the fade and in classes and the placement", () => {
      create().show();

      expect(tip().hasClass("fade")).toBe(true);
      expect(tip().hasClass("in")).toBe(true);
      expect(tip().hasClass("bottom")).toBe(true);
      expect(tip()[0].style.display).toBe("block");
    });

    it("links the target to the popover with aria-describedby", () => {
      create().show();

      expect(tip().attr("id")).toMatch(/^popover\d+$/);
      expect(target.attr("aria-describedby")).toBe(tip().attr("id"));
    });

    it("hides the title when it is empty", () => {
      create({title: ""}).show();

      expect(tip().find(".popover-title")[0].style.display).toBe("none");
      expect(tip().find(".popover-content").text()).toBe("Content");
    });

    it("is not shown when it has neither title nor content", () => {
      const onShown = jest.fn();

      create({title: "", content: "", onShown}).show();

      expect(tip().length).toBe(0);
      expect(onShown).not.toHaveBeenCalled();
    });

    it("is a silent no-op for an empty target (no element matches), like the jQuery plugin", () => {
      const onShown = jest.fn();
      const popover = createMessagePopover($("#missing"), {template: popoverTemplate("danger"), title: "Title", content: "Content", onShown});

      expect(() => popover.show()).not.toThrow();
      expect(() => popover.hide()).not.toThrow();
      expect(() => popover.destroy()).not.toThrow();
      expect(tip().length).toBe(0);
      expect(onShown).not.toHaveBeenCalled();
    });

    it("is not shown for a target which is not in the page", () => {
      target.remove();

      create().show();

      expect(tip().length).toBe(0);
    });

    it("shows a popover with only a title or only a content", () => {
      create({content: ""}).show();
      expect(tip().find(".popover-title").text()).toBe("Title");
    });
  });

  describe("sanitizing", () => {
    const MALICIOUS = "<img src=x onerror=\"window.hacked = true\"><script>window.hacked = true</script><b>bold</b>";

    it("shows the title as text, never as HTML", () => {
      create({title: MALICIOUS}).show();

      expect(tip().find(".popover-title").text()).toBe(MALICIOUS);
      expect(tip().find(".popover-title").children().length).toBe(0);
      expect(window.hacked).toBeUndefined();
    });

    it("shows the content as text, never as HTML", () => {
      create({content: MALICIOUS}).show();

      expect(tip().find(".popover-content").text()).toBe(MALICIOUS);
      expect(tip().find(".popover-content").children().length).toBe(0);
      expect(window.hacked).toBeUndefined();
    });

    it("does not let a malicious owner or type break out of the attributes of the template", () => {
      create({template: popoverTemplate("danger\" onmouseover=\"window.hacked=true", "x\"><script>window.hacked=true</script>")}).show();

      expect(tip().find("script").length).toBe(0);
      expect(tip()[0].hasAttribute("onmouseover")).toBe(false);
      expect(tip().attr("data-testid-owner")).toBe("x\"><script>window.hacked=true</script>");
    });

    it("removes the elements and attributes a template cannot have (Bootstrap allow list)", () => {
      create({
        template: "<div class=\"popover\" role=\"tooltip\" onclick=\"window.hacked=true\"><script>window.hacked=true</script>" +
          "<div class=\"arrow\"></div><h3 class=\"popover-title\"></h3><div class=\"popover-content\"></div></div>"
      }).show();

      expect(tip().find("script").length).toBe(0);
      expect(tip()[0].hasAttribute("onclick")).toBe(false);
    });

    it("drops unsafe URLs of a template link", () => {
      create({
        template: "<div class=\"popover\"><div class=\"arrow\"></div><h3 class=\"popover-title\"></h3>" +
          "<div class=\"popover-content\"></div><a href=\"javascript:window.hacked=true\" class=\"x\">x</a></div>"
      }).show();

      expect(tip().find("a")[0].hasAttribute("href")).toBe(false);
    });
  });

  describe("placement", () => {
    it("opens below the target, centered on it", () => {
      layout({left: 400, top: 100, width: 100, height: 30});

      create({placement: "auto bottom"}).show();

      expect(tip().hasClass("bottom")).toBe(true);
      expect(tip().hasClass("top")).toBe(false);
      expect(tip().css("top")).toBe("130px");
      expect(tip().css("left")).toBe("350px");
    });

    it("flips above the target when there is no room below", () => {
      layout({left: 400, top: 740, width: 100, height: 30});

      create({placement: "auto bottom"}).show();

      expect(tip().hasClass("top")).toBe(true);
      expect(tip().hasClass("bottom")).toBe(false);
      expect(tip().css("top")).toBe("640px");
    });

    it("does not flip when the placement is not auto", () => {
      layout({left: 400, top: 740, width: 100, height: 30});

      create({placement: "bottom"}).show();

      expect(tip().hasClass("bottom")).toBe(true);
    });

    it("opens above with the top placement, and flips below when there is no room above", () => {
      layout({left: 400, top: 50, width: 100, height: 30});

      create({placement: "auto top"}).show();

      expect(tip().hasClass("bottom")).toBe(true);
    });

    it("places it on the right and flips to the left at the edge of the viewport", () => {
      layout({left: 100, top: 300, width: 100, height: 30});
      create({placement: "auto right"}).show();
      expect(tip().hasClass("right")).toBe(true);
      expect(tip().css("left")).toBe("200px");
      expect(tip().css("top")).toBe("265px");

      document.body.querySelectorAll(".popover").forEach(node => node.remove());
      layout({left: 850, top: 300, width: 100, height: 30});
      create({placement: "auto right"}).show();
      expect(tip().hasClass("left")).toBe(true);
      expect(tip().css("left")).toBe("650px");
    });

    it("opens above the target when the auto token has no side", () => {
      layout({left: 400, top: 100, width: 100, height: 30});

      create({placement: "auto"}).show();

      expect(tip().hasClass("top")).toBe(true);
    });

    it("keeps it inside the viewport and moves the arrow to keep pointing at the target", () => {
      layout({left: 0, top: 100, width: 40, height: 30});

      create({placement: "auto bottom"}).show();

      expect(tip().css("left")).toBe("0px");
      // The target center (20 px) is at 10 % of the popover (200 px wide)
      expect(parseFloat(tip().find(".arrow")[0].style.left)).toBeCloseTo(10, 5);
    });

    it("centers the arrow when nothing was moved", () => {
      layout({left: 400, top: 100, width: 100, height: 30});

      create({placement: "auto bottom"}).show();

      expect(tip().find(".arrow")[0].style.left).toBe("50%");
    });

    it("uses the whole window as the target when the target is the body", () => {
      layout({});
      target = $("body");

      create({placement: "auto bottom"}).show();

      expect(tip().hasClass("top")).toBe(true);
    });
  });

  describe("transitions", () => {
    beforeEach(() => {
      jest.useFakeTimers();
      const computed = window.getComputedStyle.bind(window);
      jest.spyOn(window, "getComputedStyle").mockImplementation((element, pseudo) => new Proxy(computed(element, pseudo), {
        get: (style, property) => property === "transitionDuration" ? "0.15s" : (typeof style[property] === "function" ? style[property].bind(style) : style[property])
      }));
    });

    afterEach(() => jest.useRealTimers());

    it("raises shown once the fade ends, or after 150 ms", () => {
      const onShown = jest.fn();
      create({onShown}).show();
      expect(onShown).not.toHaveBeenCalled();

      jest.advanceTimersByTime(150);

      expect(onShown).toHaveBeenCalledTimes(1);
    });

    it("raises shown when the transition ends", () => {
      const onShown = jest.fn();
      create({onShown}).show();

      tip().trigger("transitionend");

      expect(onShown).toHaveBeenCalledTimes(1);
      jest.advanceTimersByTime(300);
      expect(onShown).toHaveBeenCalledTimes(1);
    });

    it("fades out and detaches after the transition, then raises hidden", () => {
      const onHidden = jest.fn();
      const popover = create({onHidden});
      popover.show();
      jest.advanceTimersByTime(150);

      popover.hide();
      expect(tip().hasClass("in")).toBe(false);
      expect(tip().length).toBe(1);
      expect(onHidden).not.toHaveBeenCalled();

      jest.advanceTimersByTime(150);
      expect(tip().length).toBe(0);
      expect(target.attr("aria-describedby")).toBeUndefined();
      expect(onHidden).toHaveBeenCalledTimes(1);
    });
  });

  describe("show, hide and destroy", () => {
    it("raises shown after showing", () => {
      const onShown = jest.fn();

      create({onShown}).show();

      expect(onShown).toHaveBeenCalledTimes(1);
    });

    it("hides: detaches the popover, clears aria-describedby and raises hidden", () => {
      const onHidden = jest.fn();
      const popover = create({onHidden});
      popover.show();

      popover.hide();

      expect(tip().length).toBe(0);
      expect(target.attr("aria-describedby")).toBeUndefined();
      expect(onHidden).toHaveBeenCalledTimes(1);
    });

    it("does nothing when it is hidden without being shown", () => {
      const onHidden = jest.fn();

      create({onHidden}).hide();

      expect(onHidden).not.toHaveBeenCalled();
    });

    it("can be shown again after it was hidden", () => {
      const popover = create();
      popover.show();
      popover.hide();

      popover.show();

      expect(tip().length).toBe(1);
      expect(tip().find(".popover-content").text()).toBe("Content");
    });

    it("destroys: hides, detaches and cannot be shown again", () => {
      const onHidden = jest.fn();
      const popover = create({onHidden});
      popover.show();

      popover.destroy();

      expect(tip().length).toBe(0);
      expect(onHidden).toHaveBeenCalledTimes(1);
      popover.show();
      expect(tip().length).toBe(0);
    });

    it("does not raise hidden when it is destroyed without being shown", () => {
      const onHidden = jest.fn();

      create({onHidden}).destroy();

      expect(onHidden).not.toHaveBeenCalled();
    });

    it("keeps one popover per instance, so a new one does not reuse the content of the old one", () => {
      const first = create({content: "First"});
      first.show();
      first.destroy();

      const second = create({content: "Second"});
      second.show();

      expect(tip().length).toBe(1);
      expect(tip().find(".popover-content").text()).toBe("Second");
    });

    it("shows a function title or content", () => {
      create({title: () => "From function", content: () => "Content function"}).show();

      expect(tip().find(".popover-title").text()).toBe("From function");
      expect(tip().find(".popover-content").text()).toBe("Content function");
    });
  });
});
