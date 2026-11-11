import {scaleStep, tokenColorScaleGenerator} from "../../../../src/components/pivot/colorScale";

describe('awe-react-client/test/js/components/pivot/colorScaleTest.js', () => {

  it('cuts the range of the scale in five steps, the lowest value in the first and the highest in the last', () => {
    expect([0, 19, 20, 50, 79, 80, 100].map(value => scaleStep(value, 0, 100))).toEqual([1, 1, 2, 3, 4, 5, 5]);
  });

  it('keeps the first step when the scale has no range', () => {
    expect(scaleStep(7, 7, 7)).toBe(1);
  });

  it('gives the background and the text of a cell as tokens of the scale', () => {
    const style = tokenColorScaleGenerator([0, 100]);

    expect(style(100)).toEqual({backgroundColor: "var(--awe-color-scale-5)", color: "var(--awe-color-on-scale-5)"});
    expect(style(0)).toEqual({backgroundColor: "var(--awe-color-scale-1)", color: "var(--awe-color-on-scale-1)"});
  });
});
