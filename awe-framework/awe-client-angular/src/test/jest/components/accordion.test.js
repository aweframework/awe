// End to end Jest spec of the accordion: the real awe-accordion and awe-accordion-item directives with their real templates
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

const fs = require("fs");
const path = require("path");

const template = name => fs.readFileSync(
  path.join(__dirname, "..", "..", "..", "main", "resources", "templates", "angular", `${name}.html`), "utf8");

describe("components/accordion.js (accordion and accordion item together)", () => {
  let refs;

  // jsdom loads no stylesheet: the collapsing class gets the transition of the stylesheet
  let transitionChecks;
  const enableTransitions = () => {
    transitionChecks = 0;
    const computed = window.getComputedStyle.bind(window);
    jest.spyOn(window, "getComputedStyle").mockImplementation((element, pseudo) => new Proxy(computed(element, pseudo), {
      get: (style, property) => {
        if (property === "transitionDuration") {
          transitionChecks += 1;
          return element.classList.contains("collapsing") ? "0.35s" : "";
        }
        return typeof style[property] === "function" ? style[property].bind(style) : style[property];
      }
    }));
  };

  const compile = ({selected = [], autocollapse = true} = {}) => {
    const accordion = createPanelController("acc", "accordion", {itemsSelected: selected, autocollapse, selected: null});
    stubPanelComponent(refs, createPanelModel("1"), accordion);
    jest.spyOn(refs.$control, "getAddressController").mockImplementation(address =>
      address.component === "acc" ? accordion : createPanelController(address.component, "accordion-item", {label: address.component}));
    inject(["ServerData", "$templateCache", (serverData, $templateCache) => {
      jest.spyOn(serverData, "getAngularTemplateUrl").mockImplementation(name => `${name}-template`);
      ["accordion", "accordionItem"].forEach(name => $templateCache.put(`${name}-template`, template(name)));
    }]);
    const element = refs.$compile(`<awe-accordion accordion-id="acc">
      <awe-accordion-item accordion-item-id="one"></awe-accordion-item>
      <awe-accordion-item accordion-item-id="two"></awe-accordion-item>
    </awe-accordion>`)(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    document.body.appendChild(element[0]);
    const part = id => ({panel: element.find(`#${id}`), toggle: element.find(`a[href='#${id}']`)});
    return {element, accordion, one: part("one"), two: part("two")};
  };
  const click = (toggle, scope) => {
    toggle.trigger("click");
    refs.$rootScope.$digest();
    return scope;
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  it("opens an item with a click on its toggle", () => {
    const {one, accordion} = compile();

    click(one.toggle);

    expect(accordion.itemsSelected).toEqual(["one"]);
    expect(one.panel.hasClass("in")).toBe(true);
    expect(one.toggle.hasClass("collapsed")).toBe(false);
    expect(one.toggle.attr("aria-expanded")).toBe("true");
  });

  it("closes an open item with a second click", () => {
    const {one, accordion} = compile();
    click(one.toggle);

    click(one.toggle);

    expect(accordion.itemsSelected).toEqual([]);
    expect(one.panel.hasClass("in")).toBe(false);
    expect(one.toggle.hasClass("collapsed")).toBe(true);
    expect(one.toggle.attr("aria-expanded")).toBe("false");
  });

  it("keeps one item open with autocollapse: opening another one closes the first", () => {
    const {one, two} = compile();
    click(one.toggle);

    click(two.toggle);

    expect(one.panel.hasClass("in")).toBe(false);
    expect(two.panel.hasClass("in")).toBe(true);
    expect(one.toggle.hasClass("collapsed")).toBe(true);
  });

  it("keeps several items open without autocollapse", () => {
    const {one, two} = compile({autocollapse: false});
    click(one.toggle);

    click(two.toggle);

    expect(one.panel.hasClass("in")).toBe(true);
    expect(two.panel.hasClass("in")).toBe(true);
  });

  it("does not navigate with a click on the toggle", () => {
    const {one} = compile();
    const event = $.Event("click");

    one.toggle.trigger(event);

    expect(event.isDefaultPrevented()).toBe(true);
  });

  describe("with a CSS transition", () => {
    beforeEach(() => {
      jest.useFakeTimers();
      enableTransitions();
    });

    it("renders the items selected on load open at once, without the opening animation", () => {
      const {one, two} = compile({selected: ["one"]});

      // The animation is never even considered: nothing asked whether the panel has a transition
      expect(transitionChecks).toBe(0);
      expect(one.panel.attr("class")).toContain("collapse in");
      expect(one.panel.hasClass("collapsing")).toBe(false);
      expect(one.panel[0].style.height).toBe("");
      expect(two.panel.hasClass("collapsing")).toBe(false);
      expect(two.panel.hasClass("in")).toBe(false);
    });

    it("animates the opening of an item with a click", () => {
      const {one} = compile();

      click(one.toggle);
      expect(one.panel.hasClass("collapsing")).toBe(true);
      expect(one.panel.hasClass("in")).toBe(false);

      jest.advanceTimersByTime(350);
      expect(one.panel.hasClass("collapsing")).toBe(false);
      expect(one.panel.hasClass("in")).toBe(true);
    });
  });
});
