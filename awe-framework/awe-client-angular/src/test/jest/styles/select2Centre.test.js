const fs = require("fs");
const path = require("path");

const CRITERION = path.resolve(__dirname, "../../../main/resources/less/awe/directives/criterion.less");

// A select (select2) centres its value in the field whatever line height it inherits (#885): inside the navbar
// dropdowns pixel-admin gives every link the height of the header as line height, and the value fell out of the field
describe("Select value centring (#885)", () => {
  const less = fs.readFileSync(CRITERION, "utf8").replace(/\/\/.*$/gm, "");

  it("lays the select field out as a flex row centred on its height", () => {
    const rule = less.match(/\.form-group \.select2-container \.select2-choice \{([\s\S]*?)\n\}/);
    expect(rule).not.toBeNull();
    // Own declarations only: the text before the first nested block
    const own = rule[1].slice(0, rule[1].indexOf("{"));
    expect(own).toMatch(/display: flex;/);
    expect(own).toMatch(/align-items: center;/);
  });

  it("gives the value its own line height instead of inheriting the one of its container", () => {
    const rule = less.match(/\.form-group \.select2-container \.select2-choice \{([\s\S]*?)\n\}/);
    expect(rule[1]).toMatch(/\.select2-chosen \{\s*flex: 1;\s*line-height: var\(--awe-line-height-snug\);/);
  });
});
