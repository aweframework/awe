const fs = require("fs");
const path = require("path");

// SonarJS S2187 only counts a file as having tests when the file itself calls it/test (or .each/.only/.skip of them): describe does not count and
// imports are not followed, so a compat test that only calls a shared helper would be flagged as a file without tests
const TEST_CALL = /^\s*(?:it|test)(?:\.(?:each|only|skip|concurrent))*\s*(?:\(|`)/m;

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatTestCalls (every compat test file declares its tests with a literal it/test call, as Sonar S2187 requires)", () => {
  const files = fs.readdirSync(__dirname).filter(name => /^compat.*\.test\.js$/.test(name));

  it("finds the compat test files", () => {
    expect(files.length).toBeGreaterThan(20);
  });

  it("has a literal it/test call in every one", () => {
    expect(files.filter(name => !TEST_CALL.test(fs.readFileSync(path.join(__dirname, name), "utf8")))).toEqual([]);
  });

  it("recognizes the forms Sonar accepts and rejects a file that only calls a helper", () => {
    expect(TEST_CALL.test("  it.each(cases)(\"%s\", () => {});")).toBe(true);
    expect(TEST_CALL.test("test(\"a\", () => {});")).toBe(true);
    expect(TEST_CALL.test("describeCompatFamily(badges);")).toBe(false);
    expect(TEST_CALL.test("// it(\"commented\")")).toBe(false);
  });
});
