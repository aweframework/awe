import {ANY, DIGIT_CODES, SHORTCUTS, createKeyHandler, matchesShortcut} from "../../../src/utilities/keyboard";

const press = (init) => new KeyboardEvent("keydown", init);

describe("utilities/keyboard", () => {
  describe("matchesShortcut", () => {
    it("matches a plain key", () => {
      expect(matchesShortcut(press({key: "Enter"}), {key: "Enter"})).toBe(true);
    });

    it("rejects another key", () => {
      expect(matchesShortcut(press({key: "a"}), {key: "Enter"})).toBe(false);
    });

    it("matches by code and ignores key", () => {
      expect(matchesShortcut(press({key: "º", code: "Digit0"}), {code: "Digit0"})).toBe(true);
      expect(matchesShortcut(press({key: "0", code: "Digit1"}), {code: "Digit0"})).toBe(false);
    });

    it("requires key and code when both are given", () => {
      const shortcut = {key: "Enter", code: "NumpadEnter"};
      expect(matchesShortcut(press({key: "Enter", code: "NumpadEnter"}), shortcut)).toBe(true);
      expect(matchesShortcut(press({key: "Enter", code: "Enter"}), shortcut)).toBe(false);
    });

    it("accepts arrays of keys and codes", () => {
      expect(matchesShortcut(press({key: "ArrowUp"}), {key: ["ArrowUp", "ArrowLeft"]})).toBe(true);
      expect(matchesShortcut(press({key: "ArrowLeft"}), {key: ["ArrowUp", "ArrowLeft"]})).toBe(true);
      expect(matchesShortcut(press({key: "ArrowDown"}), {key: ["ArrowUp", "ArrowLeft"]})).toBe(false);
      expect(matchesShortcut(press({code: "Digit3"}), {code: DIGIT_CODES})).toBe(true);
      expect(matchesShortcut(press({code: "KeyA"}), {code: DIGIT_CODES})).toBe(false);
    });

    it("rejects an event with an extra modifier", () => {
      expect(matchesShortcut(press({key: "Enter", ctrlKey: true}), {key: "Enter"})).toBe(false);
      expect(matchesShortcut(press({key: "Enter", altKey: true}), {key: "Enter"})).toBe(false);
      expect(matchesShortcut(press({key: "Enter", shiftKey: true}), {key: "Enter"})).toBe(false);
      expect(matchesShortcut(press({key: "Enter", metaKey: true}), {key: "Enter"})).toBe(false);
    });

    it("rejects an event with a missing modifier", () => {
      const shortcut = {key: "ArrowUp", alt: true};
      expect(matchesShortcut(press({key: "ArrowUp", altKey: true}), shortcut)).toBe(true);
      expect(matchesShortcut(press({key: "ArrowUp"}), shortcut)).toBe(false);
    });

    it("requires every modifier of a combination", () => {
      const shortcut = {code: "Digit1", alt: true, shift: true};
      expect(matchesShortcut(press({code: "Digit1", altKey: true, shiftKey: true}), shortcut)).toBe(true);
      expect(matchesShortcut(press({code: "Digit1", altKey: true}), shortcut)).toBe(false);
      expect(matchesShortcut(press({code: "Digit1", shiftKey: true}), shortcut)).toBe(false);
    });

    it("ignores a modifier set to ANY", () => {
      const shortcut = {code: "Digit1", alt: true, shift: true, ctrl: ANY, meta: ANY};
      expect(matchesShortcut(press({code: "Digit1", altKey: true, shiftKey: true}), shortcut)).toBe(true);
      expect(matchesShortcut(press({code: "Digit1", altKey: true, shiftKey: true, ctrlKey: true, metaKey: true}), shortcut)).toBe(true);
      expect(matchesShortcut(press({code: "Digit1", altKey: true}), shortcut)).toBe(false);
    });

    it("does not match a shortcut without key nor code", () => {
      expect(matchesShortcut(press({key: "Enter"}), {})).toBe(false);
    });
  });

  describe("createKeyHandler", () => {
    it("runs the first matching binding and returns true", () => {
      const first = jest.fn();
      const second = jest.fn();
      const handler = createKeyHandler([
        {shortcut: {key: "Enter"}, handler: first},
        {shortcut: {key: "Enter"}, handler: second}
      ]);
      const event = press({key: "Enter"});
      expect(handler(event)).toBe(true);
      expect(first).toHaveBeenCalledWith(event, {key: "Enter"});
      expect(second).not.toHaveBeenCalled();
    });

    it("returns false and runs nothing when no binding matches", () => {
      const run = jest.fn();
      const handler = createKeyHandler([{shortcut: {key: "Enter"}, handler: run}]);
      expect(handler(press({key: "a"}))).toBe(false);
      expect(run).not.toHaveBeenCalled();
    });

    it("accepts several shortcuts per binding and reports the matched one", () => {
      const run = jest.fn();
      const handler = createKeyHandler([{shortcuts: [{key: "ArrowUp"}, {key: "ArrowDown"}], handler: run}]);
      const event = press({key: "ArrowDown"});
      expect(handler(event)).toBe(true);
      expect(run).toHaveBeenCalledWith(event, {key: "ArrowDown"});
    });

    it("handles an empty or missing list of bindings", () => {
      expect(createKeyHandler([])(press({key: "a"}))).toBe(false);
      expect(createKeyHandler()(press({key: "a"}))).toBe(false);
    });
  });

  describe("SHORTCUTS", () => {
    it("is a frozen catalogue with id, keys, scope and description", () => {
      expect(Object.isFrozen(SHORTCUTS)).toBe(true);
      expect(SHORTCUTS.length).toBeGreaterThan(0);
      SHORTCUTS.forEach((entry) => {
        expect(entry.id).toEqual(expect.any(String));
        expect(entry.keys).toEqual(expect.any(String));
        expect(entry.scope).toEqual(expect.any(String));
        expect(entry.description).toEqual(expect.any(String));
      });
    });

    it("has unique ids", () => {
      const ids = SHORTCUTS.map((entry) => entry.id);
      expect(new Set(ids).size).toBe(ids.length);
    });

    it("lists the debug actions stack shortcut", () => {
      expect(SHORTCUTS.find((entry) => entry.id === "actions-stack")).toMatchObject({keys: "Alt+Shift+0..9", scope: "global"});
    });
  });

  describe("DIGIT_CODES", () => {
    it("lists Digit0 to Digit9", () => {
      expect(DIGIT_CODES).toHaveLength(10);
      expect(DIGIT_CODES[0]).toBe("Digit0");
      expect(DIGIT_CODES[9]).toBe("Digit9");
    });
  });
});
