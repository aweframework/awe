import fs from "fs";
import path from "path";

const STYLESHEET = path.join(__dirname, "../../../src/components/AweGridThemeClasses.less");
const SCALES = ["seq", "pos", "neg"];
const STEPS = [1, 2, 3, 4, 5];
const WCAG_AA = 4.5;
const HEX_COLOR = /^#[0-9a-f]{6}$/i;

const source = () => fs.readFileSync(STYLESHEET, "utf8");

const getRule = (className) => {
  const match = new RegExp(`\\.${className}\\s*\\{([^}]*)\\}`).exec(source());
  if (!match) return null;
  const property = (name) => new RegExp(`(?:^|[\\s;])${name}:\\s*([^;\\s]+)`).exec(match[1])?.[1];
  return {background: property("background-color"), color: property("color")};
};

const luminance = (hex) => {
  const [r, g, b] = [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map(c => c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const contrast = (first, second) => {
  const [light, dark] = [luminance(first), luminance(second)].sort((a, b) => b - a);
  return (light + 0.05) / (dark + 0.05);
};

describe("awe-react-client/test/js/styles/gridThemeClassesTest.js", () => {
  it("hides the header of a grid with the grid-no-header class without removing its cells", () => {
    const text = source();

    expect(text).toMatch(/\.grid-no-header/);
    expect(text).not.toMatch(/display:\s*none/);
  });

  SCALES.forEach(scale => {
    STEPS.forEach(step => {
      it(`defines scale-${scale}-${step} with a text colour that passes WCAG AA`, () => {
        const rule = getRule(`scale-${scale}-${step}`);

        expect(rule).not.toBeNull();
        expect(rule.background).toMatch(HEX_COLOR);
        expect(rule.color).toMatch(HEX_COLOR);
        expect(contrast(rule.background, rule.color)).toBeGreaterThanOrEqual(WCAG_AA);
      });
    });

    it(`makes every step of the ${scale} scale darker than the previous one`, () => {
      const luminances = STEPS.map(step => luminance(getRule(`scale-${scale}-${step}`).background));

      luminances.slice(1).forEach((value, index) => expect(value).toBeLessThan(luminances[index]));
    });
  });
});
