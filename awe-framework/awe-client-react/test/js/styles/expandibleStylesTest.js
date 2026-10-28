import fs from "fs";
import path from "path";
import lessCompiler from "less";

const SRC = path.join(__dirname, "../../../src");
const less = fs.readFileSync(path.join(SRC, "components", "expandible.less"), "utf8");

// Body of the first rule whose selector is exactly the given one
const ruleBody = (selector) => {
  const escaped = selector.replace(/[>*.]/g, "\\$&");
  const match = less.match(new RegExp(`${escaped}\\s*\\{([^}]*)\\}`));
  return match ? match[1] : null;
};

// Body of the compiled rule of the expanding children with the given class
const compiledRule = async (name) => {
  const {css} = await lessCompiler.render(less);
  const match = css.match(new RegExp(`\\.expandible-vertical > \\.${name},[^{]*\\{([^}]*)\\}`));
  return match ? match[1] : null;
};

const EXPAND_CLASSES = ["expand", ...Array.from({length: 11}, (_, i) => `expand-${i + 2}x`)];

describe("awe-react-client/test/js/styles/expandibleStylesTest.js", () => {
  it.each(EXPAND_CLASSES)("lets the %s children shrink below their content width", async (name) => {
    const body = await compiledRule(name);

    expect(body).not.toBeNull();
    expect(body).toMatch(/min-width:\s*0;/);
  });

  it("does not squeeze the fixed children of an expandible", () => {
    const body = ruleBody("> *");

    expect(body).not.toBeNull();
    expect(body).not.toMatch(/min-width/);
  });

  // A percentage basis is read as the content height when the height of the expandible is not definite (the screens
  // grow from min-height: 100vh), so the children took their content height instead of sharing the space: the charts of
  // a dashboard shrank to their title under a row of grids. A length basis shares it whatever the height of the parent
  it.each(EXPAND_CLASSES)("makes the %s children share the space from a zero length basis", async (name) => {
    const body = await compiledRule(name);

    expect(body).not.toBeNull();
    const flex = body.match(/flex:\s*([^;]+);/);
    expect(flex).not.toBeNull();
    expect(flex[1].trim().split(/\s+/)[2]).toBe("0px");
    expect(body).not.toMatch(/flex-basis/);
  });

  it("keeps the fixed and the inherited children out of the zero length basis", async () => {
    const {css} = await lessCompiler.render(less);

    expect(css).toMatch(/\.expandible-vertical > \*,[^{]*\{\s*flex: none;/);
  });
});
