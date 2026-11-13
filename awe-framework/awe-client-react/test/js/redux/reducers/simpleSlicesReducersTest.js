import {navigation} from "../../../../src/redux/reducers/navigationReducer";
import {runtime} from "../../../../src/redux/reducers/runtimeReducer";
import {screen} from "../../../../src/redux/reducers/screenReducer";
import {settings} from "../../../../src/redux/reducers/settingsReducer";
import {clearRuntimeEvent, setRuntimeEvent} from "../../../../src/redux/actions/runtime";
import {updateScreen} from "../../../../src/redux/actions/screen";
import {DEFAULT_SETTINGS, updateSettings} from "../../../../src/redux/actions/settings";

describe("awe-react-client/test/js/redux/reducers/simpleSlicesReducersTest.js", () => {
  describe("navigation", () => {
    it("starts without a current screen", () => {
      expect(navigation(undefined, {})).toEqual({currentScreen: null});
    });

    it("keeps the screen it navigates to", () => {
      expect(navigation(undefined, {type: "NAVIGATE_TO", payload: "home"})).toEqual({currentScreen: "home"});
    });

    it("returns the same state for unrelated actions", () => {
      const state = {currentScreen: "home"};
      expect(navigation(state, {type: "OTHER"})).toBe(state);
    });
  });

  describe("runtime", () => {
    it("starts without a last event", () => {
      expect(runtime(undefined, {})).toEqual({lastEvent: null});
    });

    it("keeps the last event with its address and timestamp", () => {
      const address = {view: "base", component: "name"};
      const action = setRuntimeEvent(address, "change");
      expect(runtime(undefined, action)).toEqual({
        lastEvent: {event: "change", address, timestamp: action.timestamp}
      });
    });

    it("clears the last event", () => {
      expect(runtime({lastEvent: {event: "blur"}}, clearRuntimeEvent())).toEqual({lastEvent: null});
    });

    it("returns the same state for unrelated actions", () => {
      const state = {lastEvent: null};
      expect(runtime(state, {type: "OTHER"})).toBe(state);
    });
  });

  describe("screen", () => {
    it("starts empty and merges the screen data", () => {
      expect(screen(undefined, {})).toEqual({});
      expect(screen({a: 1, b: 2}, updateScreen({b: 3}))).toEqual({a: 1, b: 3});
    });

    it("returns the same state for unrelated actions", () => {
      const state = {a: 1};
      expect(screen(state, {type: "OTHER"})).toBe(state);
    });
  });

  describe("settings", () => {
    it("starts with the default settings", () => {
      expect(settings(undefined, {})).toBe(DEFAULT_SETTINGS);
    });

    it("merges the new settings over the current ones", () => {
      const state = settings(undefined, updateSettings({language: "es"}));
      expect(state).toEqual({...DEFAULT_SETTINGS, language: "es"});
    });

    it("returns the same state for unrelated actions", () => {
      const state = {language: "en"};
      expect(settings(state, {type: "OTHER"})).toBe(state);
    });
  });
});
