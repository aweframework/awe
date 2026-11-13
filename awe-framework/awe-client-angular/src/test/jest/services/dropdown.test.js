import "../../../main/resources/js/awe/services/dropdown";
import "../../../main/resources/js/lib/bootstrap-tabdrop/src/js/bootstrap-tabdrop";

describe("awe-framework/awe-client-angular/src/test/jest/services/dropdown.test.js", () => {
  let events;

  const MENU = id => `<li class="dropdown" id="${id}"><a class="dropdown-toggle" data-toggle="dropdown" href="#">${id}</a>` +
    `<ul class="dropdown-menu"><li><a class="item" href="#">one</a></li><li class="disabled"><a class="item skipped" href="#">off</a></li>` +
    `<li><a class="item" href="#">two</a></li><li><input class="field"/></li></ul></li>`;
  const dropdown = id => $(`#${id}`);
  const toggle = id => dropdown(id).children("[data-toggle='dropdown']");
  const key = (target, which) => {
    const event = $.Event("keydown", {which});
    $(target).trigger(event);
    return event;
  };
  const isOpen = id => dropdown(id).hasClass("open");

  beforeEach(() => {
    events = [];
    document.body.insertAdjacentHTML("beforeend", `<ul class="nav">${MENU("first")}${MENU("second")}</ul><button id="outside">outside</button>`);
    ["show", "shown", "hide", "hidden"].forEach(name =>
      $(".dropdown").on(`${name}.bs.dropdown`, event => events.push(`${name}:${event.currentTarget.id}:${event.relatedTarget && event.relatedTarget.tagName}`)));
    // jsdom lays nothing out: every item takes room
    jest.spyOn(window.HTMLElement.prototype, "offsetWidth", "get").mockReturnValue(10);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    document.body.innerHTML = "";
    delete document.documentElement.ontouchstart;
  });

  describe("toggle", () => {
    it("opens the menu: the parent gets the open class and the toggle aria-expanded", () => {
      toggle("first").trigger("click");

      expect(isOpen("first")).toBe(true);
      expect(toggle("first").attr("aria-expanded")).toBe("true");
    });

    it("raises show and shown (with the toggle as related target) around the opening", () => {
      toggle("first").trigger("click");

      expect(events).toEqual(["show:first:A", "shown:first:A"]);
    });

    it("focuses the toggle when it opens", () => {
      toggle("first").trigger("click");

      expect(document.activeElement).toBe(toggle("first")[0]);
    });

    it("closes the menu with a second click on the toggle", () => {
      toggle("first").trigger("click");
      events.length = 0;

      toggle("first").trigger("click");

      expect(isOpen("first")).toBe(false);
      expect(toggle("first").attr("aria-expanded")).toBe("false");
      expect(events).toEqual(["hide:first:A", "hidden:first:A"]);
    });

    it("closes the other menus when it opens one", () => {
      toggle("first").trigger("click");

      toggle("second").trigger("click");

      expect(isOpen("first")).toBe(false);
      expect(isOpen("second")).toBe(true);
    });

    it("prevents the default action of the toggle (a link to #)", () => {
      const click = $.Event("click");

      toggle("first").trigger(click);

      expect(click.isDefaultPrevented()).toBe(true);
    });

    it("does not open a disabled toggle", () => {
      toggle("first").addClass("disabled").trigger("click");

      expect(isOpen("first")).toBe(false);
    });

    it("does not open when show is prevented", () => {
      dropdown("first").on("show.bs.dropdown", event => event.preventDefault());

      toggle("first").trigger("click");

      expect(isOpen("first")).toBe(false);
    });

    it("prevents the default action of the toggle even when show is prevented", () => {
      dropdown("first").on("show.bs.dropdown", event => event.preventDefault());
      const click = $.Event("click");

      toggle("first").trigger(click);

      expect(click.isDefaultPrevented()).toBe(true);
    });

    it("works with a toggle which is not a link and has no href", () => {
      document.body.insertAdjacentHTML("beforeend",
        "<li class='dropdown' id='avatar'><a class='dropdown-toggle' data-toggle='dropdown'>x</a><ul class='dropdown-menu'></ul></li>");

      $("#avatar > a").trigger("click");

      expect(isOpen("avatar")).toBe(true);
    });

    it("opens the element of data-target instead of the parent", () => {
      document.body.insertAdjacentHTML("beforeend",
        "<div id='holder'></div><button id='targeting' data-toggle='dropdown' data-target='#holder'></button>");

      $("#targeting").trigger("click");

      expect($("#holder").hasClass("open")).toBe(true);
      expect($("#targeting").parent().hasClass("open")).toBe(false);
    });

    it.each([
      ["/page.html#holder", "#holder"],
      ["#holder", "#holder"],
      ["/a#b#holder", "#holder"]
    ])("opens the element of the anchor of the href %s", href => {
      document.body.insertAdjacentHTML("beforeend", `<div id='holder'></div><a id='linking' data-toggle='dropdown' href='${href}'></a>`);

      $("#linking").trigger("click");

      expect($("#holder").hasClass("open")).toBe(true);
    });

    it("opens the parent when the href is only '#' or has no anchor", () => {
      toggle("first").attr("href", "#");
      toggle("second").attr("href", "/page.html");

      toggle("first").trigger("click");
      expect(isOpen("first")).toBe(true);
      toggle("second").trigger("click");
      expect(isOpen("second")).toBe(true);
    });

    it("does not need the toggle to be there when the script runs (delegated)", () => {
      document.body.insertAdjacentHTML("beforeend", MENU("late"));

      toggle("late").trigger("click");

      expect(isOpen("late")).toBe(true);
    });
  });

  describe("closing", () => {
    beforeEach(() => toggle("first").trigger("click"));

    it("closes with a click outside", () => {
      $("#outside").trigger("click");

      expect(isOpen("first")).toBe(false);
      expect(toggle("first").attr("aria-expanded")).toBe("false");
    });

    it("closes with a click on an item of the menu", () => {
      dropdown("first").find(".item").first().trigger("click");

      expect(isOpen("first")).toBe(false);
    });

    it("stays open with a click in a field of the menu", () => {
      dropdown("first").find(".field").trigger("click");

      expect(isOpen("first")).toBe(true);
    });

    it("stays open with a click in a form of a dropdown", () => {
      document.body.insertAdjacentHTML("beforeend", "<form id='form'><button id='inForm' type='button'>x</button></form>");
      $("#form").appendTo(dropdown("first"));

      $("#inForm").trigger("click");

      expect(isOpen("first")).toBe(true);
    });

    it("ignores a click with the right button", () => {
      $("#outside").trigger($.Event("click", {which: 3}));

      expect(isOpen("first")).toBe(true);
    });

    it("does not close when hide is prevented", () => {
      dropdown("first").on("hide.bs.dropdown", event => event.preventDefault());

      $("#outside").trigger("click");

      expect(isOpen("first")).toBe(true);
    });

    it("closes with Esc and gives the focus to the toggle", () => {
      const item = dropdown("first").find(".item").first();
      item.trigger("focus");

      key(item, 27);

      expect(isOpen("first")).toBe(false);
      expect(document.activeElement).toBe(toggle("first")[0]);
    });

    it("closes with Esc raised from the toggle", () => {
      key(toggle("first"), 27);

      expect(isOpen("first")).toBe(false);
    });

    it("keeps Esc inside a field of the menu", () => {
      key(dropdown("first").find(".field"), 27);

      expect(isOpen("first")).toBe(true);
    });
  });

  describe("keyboard", () => {
    it("opens a closed menu with the down arrow, the up arrow or the space bar on the toggle", () => {
      [40, 38, 32].forEach(which => {
        key(toggle("first"), which);
        expect(isOpen("first")).toBe(true);
        toggle("first").trigger("click");
        expect(isOpen("first")).toBe(false);
      });
    });

    it("moves the focus between the items with the arrows, skipping the disabled ones", () => {
      toggle("first").trigger("click");
      const items = dropdown("first").find(".dropdown-menu li:not(.disabled) a");

      key(toggle("first"), 40);
      expect(document.activeElement).toBe(items[0]);

      key(items[0], 40);
      expect(document.activeElement).toBe(items[1]);

      key(items[1], 40);
      expect(document.activeElement).toBe(items[1]);

      key(items[1], 38);
      expect(document.activeElement).toBe(items[0]);
    });

    it("prevents the default action and the propagation of the keys it handles", () => {
      const event = key(toggle("first"), 40);

      expect(event.isDefaultPrevented()).toBe(true);
      expect(event.isPropagationStopped()).toBe(true);
    });

    it("ignores the other keys and the keys typed in a field", () => {
      expect(key(toggle("first"), 13).isDefaultPrevented()).toBe(false);
      toggle("first").trigger("click");
      expect(key(dropdown("first").find(".field"), 40).isDefaultPrevented()).toBe(false);
    });
  });

  describe("touch screens", () => {
    beforeEach(() => {
      document.documentElement.ontouchstart = null;
    });

    it("puts a backdrop behind the menu, which closes it", () => {
      toggle("first").trigger("click");
      expect($(".dropdown-backdrop").length).toBe(1);

      $(".dropdown-backdrop").trigger("click");

      expect(isOpen("first")).toBe(false);
      expect($(".dropdown-backdrop").length).toBe(0);
    });

    it("leaves no backdrop when show is prevented", () => {
      dropdown("first").on("show.bs.dropdown", event => event.preventDefault());

      toggle("first").trigger("click");

      expect(isOpen("first")).toBe(false);
      expect($(".dropdown-backdrop").length).toBe(0);
    });

    it("does not put a backdrop in a navbar", () => {
      $(".nav").addClass("navbar-nav");

      toggle("first").trigger("click");

      expect($(".dropdown-backdrop").length).toBe(0);
    });
  });

  describe("tab strip overflow (bootstrap-tabdrop)", () => {
    it("opens the overflow menu of the tabs with its toggle", () => {
      document.body.insertAdjacentHTML("beforeend", "<ul id='tabs' class='nav nav-tabs'><li><a>One</a></li></ul>");
      $("#tabs").tabdrop();
      const overflow = $("#tabs > li.tabdrop");

      overflow.children("a.dropdown-toggle").trigger("click");

      expect(overflow.hasClass("open")).toBe(true);
      expect(overflow.children("a").attr("aria-expanded")).toBe("true");

      $("#outside").trigger("click");
      expect(overflow.hasClass("open")).toBe(false);
    });
  });
});
