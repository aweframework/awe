import {getModal, hideModal, isModalShown, showModal, disposeModal} from "../../../main/resources/js/awe/services/modal";

describe("awe-framework/awe-client-angular/src/test/jest/services/modal.test.js", () => {
  let element, opener, events;

  const BOOTSTRAP_EVENTS = ["show", "shown", "hide", "hidden"];
  const build = (classes = "modal", attributes = "") => {
    document.body.insertAdjacentHTML("beforeend",
      `<div class="${classes}" ${attributes}><div class="modal-dialog"><div class="modal-content">` +
      `<button class="inside">inside</button><button class="dismiss" data-dismiss="modal">&times;</button>` +
      `</div></div></div>`);
    const modal = $(document.body).children(".modal").last();
    BOOTSTRAP_EVENTS.forEach(name => modal.on(`${name}.bs.modal`, () => events.push({
      name,
      open: modal.hasClass("in"),
      display: modal[0].style.display,
      backdrops: document.querySelectorAll(".modal-backdrop").length,
      bodyOpen: document.body.classList.contains("modal-open")
    })));
    return modal;
  };
  const names = () => events.map(event => event.name);
  const key = (target, code) => target.dispatchEvent(new window.KeyboardEvent("keydown", {keyCode: code, which: code, bubbles: true, cancelable: true}));
  // jsdom loads no stylesheet: give the modal, its dialog and its backdrop the CSS transition of the fade classes
  const enableTransitions = () => {
    const computed = window.getComputedStyle.bind(window);
    jest.spyOn(window, "getComputedStyle").mockImplementation((target, pseudo) => new Proxy(computed(target, pseudo), {
      get: (style, property) => {
        if (property === "transitionDuration") {
          return "0.15s";
        }
        return typeof style[property] === "function" ? style[property].bind(style) : style[property];
      }
    }));
  };
  const endTransition = target => $(target).trigger("transitionend");

  beforeEach(() => {
    events = [];
    opener = document.createElement("button");
    document.body.appendChild(opener);
    opener.focus();
    element = build();
  });

  afterEach(() => {
    $(document).off("focusin.bs.modal");
    $(window).off("resize.bs.modal");
    jest.restoreAllMocks();
    document.body.innerHTML = "";
    document.body.removeAttribute("style");
    document.body.className = "";
    jest.useRealTimers();
  });

  describe("show", () => {
    it("displays the modal with the in class, a backdrop and the body flagged as modal-open", () => {
      showModal(element);

      expect(element[0].style.display).toBe("block");
      expect(element.hasClass("in")).toBe(true);
      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);
      expect(document.querySelector(".modal-backdrop").classList.contains("in")).toBe(true);
      expect(document.querySelector(".modal-backdrop").parentNode).toBe(document.body);
      expect(document.body.classList.contains("modal-open")).toBe(true);
      expect(isModalShown(element)).toBe(true);
    });

    it("triggers show before anything changes and shown once it is displayed", () => {
      showModal(element);

      expect(names()).toEqual(["show", "shown"]);
      expect(events[0]).toMatchObject({open: false, backdrops: 0, bodyOpen: false});
      expect(events[1]).toMatchObject({open: true, display: "block", backdrops: 1, bodyOpen: true});
    });

    it("cancels the show when a show.bs.modal listener prevents the default", () => {
      element.on("show.bs.modal", event => event.preventDefault());

      showModal(element);

      expect(names()).toEqual(["show"]);
      expect(element.hasClass("in")).toBe(false);
      expect(document.querySelector(".modal-backdrop")).toBeNull();
      expect(isModalShown(element)).toBe(false);
    });

    it("ignores a second show while it is shown (one backdrop only)", () => {
      showModal(element);
      showModal(element);

      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);
      expect(names().filter(name => name === "shown").length).toBe(1);
    });

    it("gives each open modal its own backdrop", () => {
      const second = build();

      showModal(element);
      showModal(second);

      expect(document.querySelectorAll(".modal-backdrop").length).toBe(2);
    });

    it("gives the backdrop the fade class only when the modal fades", () => {
      showModal(element);
      expect(document.querySelector(".modal-backdrop").classList.contains("fade")).toBe(false);

      hideModal(element);
      const fading = build("modal fade");
      showModal(fading);
      expect(document.querySelector(".modal-backdrop").classList.contains("fade")).toBe(true);
    });

    it("moves a detached modal to the body", () => {
      const detached = $("<div class='modal'><div class='modal-dialog'></div></div>");

      showModal(detached);

      expect(detached.parent()[0]).toBe(document.body);
    });

    it("reads backdrop and keyboard from the data attributes of the modal", () => {
      const statik = build("modal", "data-backdrop='static' data-keyboard='false'");

      expect(getModal(statik).options).toMatchObject({backdrop: "static", keyboard: false});
      expect(getModal(element).options).toMatchObject({backdrop: true, keyboard: true});
    });

    it("keeps the same instance of an element", () => {
      expect(getModal(element)).toBe(getModal(element));
    });

    it("moves the focus to the modal", () => {
      element.attr("tabindex", "-1");

      showModal(element);

      expect(document.activeElement).toBe(element[0]);
    });

    it("sends the focus back into the modal when it leaves", () => {
      element.attr("tabindex", "-1");
      showModal(element);

      opener.focus();

      expect(document.activeElement).toBe(element[0]);
    });
  });

  describe("transitions", () => {
    beforeEach(() => {
      enableTransitions();
      jest.useFakeTimers();
      element = build("modal fade");
    });

    it("shows the modal first and raises shown when the dialog finishes sliding in", () => {
      showModal(element);

      expect(element.hasClass("in")).toBe(false);
      endTransition(document.querySelector(".modal-backdrop"));
      expect(element.hasClass("in")).toBe(true);
      expect(names()).toEqual(["show"]);

      endTransition(element.find(".modal-dialog"));
      expect(names()).toEqual(["show", "shown"]);
    });

    it("does not wait for a transition which never ends (backdrop 150 ms, dialog 300 ms)", () => {
      showModal(element);

      jest.advanceTimersByTime(150);
      expect(element.hasClass("in")).toBe(true);
      expect(names()).toEqual(["show"]);

      jest.advanceTimersByTime(300);
      expect(names()).toEqual(["show", "shown"]);
    });

    it("raises hidden after the modal and the backdrop faded, with the backdrop already removed", () => {
      showModal(element);
      jest.advanceTimersByTime(450);
      events.length = 0;

      hideModal(element);

      expect(element.hasClass("in")).toBe(false);
      expect(names()).toEqual(["hide"]);
      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);

      endTransition(element);
      expect(element[0].style.display).toBe("none");
      expect(names()).toEqual(["hide"]);

      endTransition(document.querySelector(".modal-backdrop"));
      expect(names()).toEqual(["hide", "hidden"]);
      expect(events[1]).toMatchObject({backdrops: 0, bodyOpen: false, display: "none"});
    });

    it("hides after 300 ms plus 150 ms when no transition ends", () => {
      showModal(element);
      jest.advanceTimersByTime(450);
      events.length = 0;

      hideModal(element);
      jest.advanceTimersByTime(300);
      expect(names()).toEqual(["hide"]);
      jest.advanceTimersByTime(150);

      expect(names()).toEqual(["hide", "hidden"]);
      expect(document.querySelector(".modal-backdrop")).toBeNull();
    });

    it("disposed while it waits for its backdrop transition, it never shows (no callback on a detached element)", () => {
      showModal(element);

      disposeModal(element);
      element.remove();
      jest.advanceTimersByTime(1000);

      expect(element.hasClass("in")).toBe(false);
      expect(element[0].style.display).not.toBe("block");
      expect(names()).toEqual(["show"]);
      expect(document.body.contains(element[0])).toBe(false);
    });

    it("disposed while it waits for its dialog transition, it never raises shown", () => {
      showModal(element);
      jest.advanceTimersByTime(150);

      disposeModal(element);
      jest.advanceTimersByTime(1000);

      expect(names()).toEqual(["show"]);
    });

    it("shown again during its fade out: one backdrop, no stale hide, and it stays open", () => {
      showModal(element);
      jest.advanceTimersByTime(450);
      events.length = 0;
      hideModal(element);

      showModal(element);
      jest.advanceTimersByTime(1000);

      expect(isModalShown(element)).toBe(true);
      expect(element.hasClass("in")).toBe(true);
      expect(element[0].style.display).toBe("block");
      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);
      expect(document.body.classList.contains("modal-open")).toBe(true);
      expect(names()).not.toContain("hidden");
      expect(names().filter(name => name === "shown").length).toBe(1);
    });

    it("shown again during the fade out of its backdrop: the old backdrop is not left behind", () => {
      showModal(element);
      jest.advanceTimersByTime(450);
      hideModal(element);
      jest.advanceTimersByTime(300);
      expect(element[0].style.display).toBe("none");

      showModal(element);
      jest.advanceTimersByTime(1000);

      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);
      expect(element.hasClass("in")).toBe(true);
      hideModal(element);
      jest.advanceTimersByTime(1000);
      expect(document.querySelectorAll(".modal-backdrop").length).toBe(0);
    });

    it("keeps the body padding it had when it is shown again during its fade out", () => {
      document.body.style.paddingRight = "4px";
      showModal(element);
      jest.advanceTimersByTime(450);
      hideModal(element);

      showModal(element);
      jest.advanceTimersByTime(1000);
      hideModal(element);
      jest.advanceTimersByTime(1000);

      expect(document.body.style.paddingRight).toBe("4px");
    });

    it("ignores the transitions which bubble up from the content of the dialog", () => {
      showModal(element);
      jest.advanceTimersByTime(150);

      endTransition(element.find(".inside"));

      expect(names()).toEqual(["show"]);
    });
  });

  describe("a fading modal without CSS transition", () => {
    it("does not make the user wait for the timeout", () => {
      jest.useFakeTimers();
      const fading = build("modal fade");

      showModal(fading);
      hideModal(fading);

      expect(names()).toEqual(["show", "shown", "hide", "hidden"]);
      expect(document.querySelector(".modal-backdrop")).toBeNull();
    });
  });

  describe("hide", () => {
    beforeEach(() => showModal(element));

    it("hides the modal, removes the backdrop and the modal-open flag", () => {
      hideModal(element);

      expect(element.hasClass("in")).toBe(false);
      expect(element[0].style.display).toBe("none");
      expect(document.querySelector(".modal-backdrop")).toBeNull();
      expect(document.body.classList.contains("modal-open")).toBe(false);
      expect(isModalShown(element)).toBe(false);
    });

    it("triggers hide first and hidden last, when the backdrop is already gone", () => {
      events.length = 0;

      hideModal(element);

      expect(names()).toEqual(["hide", "hidden"]);
      expect(events[0]).toMatchObject({open: true, backdrops: 1, bodyOpen: true});
      expect(events[1]).toMatchObject({open: false, backdrops: 0, bodyOpen: false});
    });

    it("cancels the hide when a hide.bs.modal listener prevents the default", () => {
      element.on("hide.bs.modal", event => event.preventDefault());

      hideModal(element);

      expect(element.hasClass("in")).toBe(true);
      expect(isModalShown(element)).toBe(true);
    });

    it("does nothing when it is not shown, but still raises hide", () => {
      hideModal(element);
      events.length = 0;

      hideModal(element);

      expect(names()).toEqual(["hide"]);
    });

    it("can be shown again", () => {
      hideModal(element);
      showModal(element);

      expect(element.hasClass("in")).toBe(true);
      expect(document.querySelectorAll(".modal-backdrop").length).toBe(1);
    });

    it("gives the focus back to the control which opened it", () => {
      hideModal(element);

      expect(document.activeElement).toBe(opener);
    });

    it("leaves the focus where the user moved it outside the modal", () => {
      const other = document.createElement("input");
      document.body.appendChild(other);
      $(document).off("focusin.bs.modal");
      other.focus();

      hideModal(element);

      expect(document.activeElement).toBe(other);
    });
  });

  describe("dismissing", () => {
    it("closes with Esc", () => {
      showModal(element);

      key(element[0], 27);

      expect(isModalShown(element)).toBe(false);
    });

    it("closes with Esc raised from an element inside", () => {
      showModal(element);

      key(element.find(".inside")[0], 27);

      expect(isModalShown(element)).toBe(false);
    });

    it("ignores other keys", () => {
      showModal(element);

      key(element[0], 13);

      expect(isModalShown(element)).toBe(true);
    });

    it("does not close with Esc when data-keyboard is false", () => {
      const modal = build("modal", "data-keyboard='false'");
      showModal(modal);

      key(modal[0], 27);

      expect(isModalShown(modal)).toBe(true);
    });

    it("stops listening to Esc once hidden", () => {
      showModal(element);
      hideModal(element);
      const listener = jest.fn();
      element.on("hide.bs.modal", listener);

      key(element[0], 27);

      expect(listener).not.toHaveBeenCalled();
    });

    it("closes with a click on the backdrop area (the modal itself)", () => {
      showModal(element);

      element.trigger("click");

      expect(isModalShown(element)).toBe(false);
    });

    it("does not close with a click inside the dialog", () => {
      showModal(element);

      element.find(".inside").trigger("click");

      expect(isModalShown(element)).toBe(true);
    });

    it("does not close with a click on the backdrop when it is static, and focuses the modal instead", () => {
      const modal = build("modal", "data-backdrop='static' tabindex='-1'");
      showModal(modal);
      opener.focus();
      $(document).off("focusin.bs.modal");

      modal.trigger("click");

      expect(isModalShown(modal)).toBe(true);
      expect(document.activeElement).toBe(modal[0]);
    });

    it("does not close with a click when the mouse was pressed inside the dialog and released outside", () => {
      showModal(element);

      element.find(".modal-dialog").trigger("mousedown");
      element.trigger("mouseup");
      element.trigger("click");

      expect(isModalShown(element)).toBe(true);
      element.trigger("click");
      expect(isModalShown(element)).toBe(false);
    });

    it("closes with a data-dismiss=modal button", () => {
      showModal(element);

      element.find(".dismiss").trigger("click");

      expect(isModalShown(element)).toBe(false);
    });

    it("prevents the default action of the data-dismiss button", () => {
      showModal(element);
      const click = $.Event("click");

      element.find(".dismiss").trigger(click);

      expect(click.isDefaultPrevented()).toBe(true);
    });

    it("closes with a data-dismiss=modal element added after it was shown", () => {
      showModal(element);
      element.find(".modal-content").append("<span class='late' data-dismiss='modal'></span>");

      element.find(".late").trigger("click");

      expect(isModalShown(element)).toBe(false);
    });

    it("stops listening to data-dismiss buttons once hidden", () => {
      showModal(element);
      hideModal(element);
      events.length = 0;

      element.find(".dismiss").trigger("click");

      expect(names()).toEqual([]);
    });
  });

  describe("scrollbar", () => {
    const measure = width => Object.defineProperty(window.HTMLElement.prototype, "offsetWidth", {
      configurable: true,
      get() {
        return this.classList && this.classList.contains("modal-scrollbar-measure") ? width : 0;
      }
    });

    const originalInnerWidth = window.innerWidth;

    afterEach(() => {
      delete window.HTMLElement.prototype.offsetWidth;
      delete document.body.clientWidth;
      Object.defineProperty(window, "innerWidth", {configurable: true, writable: true, value: originalInnerWidth});
    });

    it("compensates the scrollbar width in the body padding while open and restores it", () => {
      measure(15);
      document.body.style.paddingRight = "4px";
      Object.defineProperty(document.body, "clientWidth", {configurable: true, value: 985});
      Object.defineProperty(window, "innerWidth", {configurable: true, value: 1000});
      const {paddingRight} = window.getComputedStyle(document.body);

      showModal(element);
      expect(document.body.style.paddingRight).toBe(`${parseInt(paddingRight, 10) + 15}px`);
      expect(document.querySelector(".modal-scrollbar-measure")).toBeNull();

      hideModal(element);
      expect(document.body.style.paddingRight).toBe("4px");
      delete document.body.clientWidth;
    });

    it("leaves the body padding alone when the page has no scrollbar", () => {
      measure(15);
      Object.defineProperty(document.body, "clientWidth", {configurable: true, value: 1000});
      Object.defineProperty(window, "innerWidth", {configurable: true, value: 1000});

      showModal(element);

      expect(document.body.style.paddingRight).toBe("");
      delete document.body.clientWidth;
    });

    it("pads the fixed navbars like the body", () => {
      measure(15);
      document.body.insertAdjacentHTML("beforeend", "<nav class='navbar-fixed-top' style='padding-right: 3px'></nav>");
      Object.defineProperty(document.body, "clientWidth", {configurable: true, value: 985});
      Object.defineProperty(window, "innerWidth", {configurable: true, value: 1000});

      showModal(element);
      expect(document.querySelector(".navbar-fixed-top").style.paddingRight).toBe("18px");

      hideModal(element);
      expect(document.querySelector(".navbar-fixed-top").style.paddingRight).toBe("3px");
      delete document.body.clientWidth;
    });
  });

  describe("dispose", () => {
    it("removes the backdrop and the modal-open flag of a shown modal at once, without events", () => {
      showModal(element);
      events.length = 0;

      disposeModal(element);

      expect(document.querySelector(".modal-backdrop")).toBeNull();
      expect(document.body.classList.contains("modal-open")).toBe(false);
      expect(isModalShown(element)).toBe(false);
      expect(names()).toEqual([]);
    });

    it("removes the listeners of the document and the window", () => {
      showModal(element);

      disposeModal(element);
      opener.focus();

      expect(document.activeElement).toBe(opener);
    });

    it("does nothing with a modal which was never shown", () => {
      expect(() => disposeModal(element)).not.toThrow();
      expect(isModalShown(element)).toBe(false);
    });
  });
});
