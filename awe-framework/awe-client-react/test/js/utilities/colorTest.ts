import {fromColor, toColor} from "../../../src/utilities/color";

describe("awe-react-client/test/js/utilities/colorTest.ts", () => {
  it.each([
    ["a bare hexadecimal value", "ff0000", "#ff0000"],
    ["a value that already has the hash", "#00ff00", "#00ff00"],
    ["an empty value", "", ""],
    ["null", null, ""],
    ["undefined", undefined, ""]
  ])("converts %s to a color", (_name, value, expected) => {
    expect(toColor(value)).toBe(expected);
  });

  it.each([
    ["a color with hash", "#ff0000", "ff0000"],
    ["a bare value", "ff0000", "ff0000"],
    ["several hashes", "#ff#00", "ff00"],
    ["an empty value", "", ""],
    ["null", null, ""],
    ["undefined", undefined, ""]
  ])("strips the hash from %s", (_name, value, expected) => {
    expect(fromColor(value)).toBe(expected);
  });
});
