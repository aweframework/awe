import React, {Profiler} from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweButton from "../../../src/components/AweButton";
import AweCriterion from "../../../src/criteria/AweCriterion";
import {DEFAULT_SETTINGS, updateSettings} from "../../../src/redux/actions/settings";

/**
 * A component only renders again when a setting it reads changes, not on any change of the settings (#962)
 */
describe("awe-react-client/test/js/performance/SettingsSelectorTest.jsx", () => {
  const buttonState = {
    settings: DEFAULT_SETTINGS,
    actions: {running: false},
    components: {
      btn1: {
        address: {view: "v", component: "btn1"},
        attributes: {label: "Click me", visible: true},
        actions: []
      }
    }
  };

  const renderCounting = (element, preloadedState) => {
    const renders = {count: 0};
    const result = renderWithProviders(
      <Profiler id="subject" onRender={() => renders.count++}>{element}</Profiler>, {preloadedState});
    renders.count = 0;
    return {...result, renders};
  };

  const changeSettings = (store, settings) => act(() => {
    store.dispatch(updateSettings(settings));
  });

  describe("AweButton", () => {
    it("does not render again when a setting it does not read changes", () => {
      const {store, renders} = renderCounting(<AweButton id="btn1"/>, buttonState);

      changeSettings(store, {actionsStack: 1000});

      expect(renders.count).toBe(0);
    });

    it("renders again with the new value when the help timeout changes", () => {
      const {store, renders, container} = renderCounting(<AweButton id="btn1"/>, buttonState);

      changeSettings(store, {helpTimeout: 4321});

      expect(renders.count).toBeGreaterThan(0);
      expect(container.querySelector("button#btn1").getAttribute("data-pr-showdelay")).toBe("4321");
    });
  });

  describe("AweCriterion", () => {
    const renderCriterion = () => renderCounting(
      <AweCriterion address={{view: "v", component: "crit"}} attributes={{label: "Name", leftLabel: "10", visible: true}}>
        <input aria-label="field"/>
      </AweCriterion>,
      {settings: DEFAULT_SETTINGS});

    it("does not render again when a setting it does not read changes", () => {
      const {store, renders} = renderCriterion();

      changeSettings(store, {actionsStack: 1000});

      expect(renders.count).toBe(0);
    });

    it("renders again with the new size when the pixels per character change", () => {
      const {store, renders, container} = renderCriterion();
      const labelWidth = () => container.querySelector("[style*='width']")?.style.width;
      const before = labelWidth();

      changeSettings(store, {pixelsPerCharacter: 20});

      expect(renders.count).toBeGreaterThan(0);
      expect(labelWidth()).toBe("200px");
      expect(before).not.toBe("200px");
    });
  });
});
