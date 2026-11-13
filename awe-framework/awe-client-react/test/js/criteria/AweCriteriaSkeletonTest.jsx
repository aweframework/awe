import React from "react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";
import AweInputTextarea from "../../../src/criteria/AweInputTextarea";
import AweInputCheckbox from "../../../src/criteria/AweInputCheckbox";
import AweInputRadio from "../../../src/criteria/AweInputRadio";
import AweSelect from "../../../src/criteria/AweSelect";
import AweSuggest from "../../../src/criteria/AweSuggest";
import AwePicklist from "../../../src/criteria/AwePicklist";

const hook = "[data-testid='loader']";

const registered = (component, attributes = {}) => ({
  settings: DEFAULT_SETTINGS,
  components: {
    [component]: {
      address: {component, view: "report"},
      model: {values: []},
      attributes: {visible: true, ...attributes},
      validationRules: {required: false},
      specificAttributes: {sort: []}
    }
  }
});
const unregistered = () => ({settings: DEFAULT_SETTINGS, components: {}});

describe("awe-react-client/test/js/criteria/AweCriteriaSkeletonTest.jsx", () => {
  describe.each([
    ["text", AweInputText, "field"],
    ["textarea", AweInputTextarea, "textarea"],
    ["checkbox", AweInputCheckbox, "check"],
    ["radio", AweInputRadio, "check"],
    ["select", AweSelect, "field"],
    ["suggest", AweSuggest, "field"],
    ["picklist", AwePicklist, "list"]
  ])("%s", (name, Component, kind) => {
    it("shows a hidden skeleton with the loader hook until the criterion registers", () => {
      const {container} = renderWithProviders(<Component id="crt"/>, {preloadedState: unregistered()});

      const skeleton = container.querySelector(hook);
      expect(skeleton).not.toBeNull();
      expect(skeleton.getAttribute("aria-hidden")).toBe("true");
      expect(skeleton.classList.contains("awe-skeleton-criterion")).toBe(true);
      expect(skeleton.classList.contains(`awe-skeleton-criterion-${kind}`)).toBe(true);
      expect(skeleton.querySelectorAll(".p-skeleton").length).toBeGreaterThan(0);
      expect(container.querySelector("input, textarea, .p-dropdown, .p-picklist")).toBeNull();
    });

    it("paints no skeleton once registered", () => {
      const {container} = renderWithProviders(<Component id="crt"/>, {preloadedState: registered("crt", {label: "Name"})});

      expect(container.querySelector(hook)).toBeNull();
      expect(container.querySelector(".p-skeleton")).toBeNull();
    });
  });

  it("sizes the label placeholder as the label and the field as the size of the criterion", () => {
    const {container} = renderWithProviders(<AweInputText id="crt"/>, {
      preloadedState: {settings: DEFAULT_SETTINGS, components: {crt: {attributes: {label: "Surname", size: "lg"}}}}
    });

    expect(container.querySelector(".awe-skeleton-label").style.width).toBe("7ch");
    expect(container.querySelector(".awe-skeleton-field").classList.contains("awe-skeleton-lg")).toBe(true);
  });

  it("takes the layout cell of the criterion so the page does not jump", () => {
    const {container} = renderWithProviders(<AweInputText id="crt"/>, {
      preloadedState: {settings: DEFAULT_SETTINGS, components: {crt: {attributes: {style: "col-6"}}}}
    });

    const skeleton = container.querySelector(hook);
    expect(skeleton.classList.contains("col-6")).toBe(true);
    expect(skeleton.classList.contains("col-12")).toBe(false);
  });

  it("omits the label placeholder of a criterion without label", () => {
    const {container} = renderWithProviders(<AweInputText id="crt"/>, {
      preloadedState: {settings: DEFAULT_SETTINGS, components: {crt: {attributes: {style: "no-label"}}}}
    });

    expect(container.querySelector(".awe-skeleton-label")).toBeNull();
  });

  it("gives the textarea skeleton the height of its rows", () => {
    const {container} = renderWithProviders(<AweInputTextarea id="crt"/>, {
      preloadedState: {settings: DEFAULT_SETTINGS, components: {crt: {attributes: {areaRows: 5}}}}
    });

    expect(container.querySelector(".awe-skeleton-field").style.height).toBe("8.5rem");
  });

  it("draws option lines for a picklist", () => {
    const {container} = renderWithProviders(<AwePicklist id="crt"/>, {preloadedState: unregistered()});

    expect(container.querySelectorAll(".awe-skeleton-line").length).toBeGreaterThanOrEqual(3);
  });

  it("draws a box and a label for a checkbox", () => {
    const {container} = renderWithProviders(<AweInputCheckbox id="crt"/>, {preloadedState: unregistered()});

    expect(container.querySelector(".awe-skeleton-check")).not.toBeNull();
    expect(container.querySelector(".awe-skeleton-label")).not.toBeNull();
  });
});
