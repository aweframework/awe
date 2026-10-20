import fs from "fs";
import path from "path";

const SRC = path.join(__dirname, "../../../src");
const less = fs.readFileSync(path.join(SRC, "components", "expandible.less"), "utf8");

// Body of the first rule whose selector is exactly the given one
const ruleBody = (selector) => {
  const escaped = selector.replace(/[>*.]/g, "\\$&");
  const match = less.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`));
  return match ? match[1] : null;
};

describe("awe-react-client/test/js/styles/expandibleStylesTest.js", () => {
  it.each(["expand", ...Array.from({length: 11}, (_, i) => `expand-${i + 2}x`)])(
    "lets the %s children shrink below their content width",
    (name) => {
      const body = ruleBody(`> .${name}`);

      expect(body).not.toBeNull();
      expect(body).toMatch(/min-width:\s*0;/);
    });

  it("does not squeeze the fixed children of an expandible", () => {
    const body = ruleBody("> *");

    expect(body).not.toBeNull();
    expect(body).not.toMatch(/min-width/);
  });
});
