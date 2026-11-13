import {buildSparkline, parseSparklineSeries, SPARKLINE_HEIGHT, SPARKLINE_WIDTH} from "../../../src/utilities/sparkline";

/*
 * The AngularJS client has the same renderer (js/awe/services/chart/sparklineRenderer.js) with the same tests: the
 * golden shapes below are identical in both so the two engines draw the same SVG.
 */
const tags = model => model.shapes.map(shape => shape.tag);
const bySelector = (model, tag) => model.shapes.filter(shape => shape.tag === tag);
const hitAreas = model => model.shapes.filter(shape => shape.title !== undefined);

describe("awe-react-client/test/js/utilities/sparklineTest.js", () => {

  describe("parseSparklineSeries", () => {
    it("accepts an array of numbers", () => {
      expect(parseSparklineSeries([3, 5, -1, 8])).toEqual([3, 5, -1, 8]);
    });

    it("accepts a JSON string with an array of numbers", () => {
      expect(parseSparklineSeries("[3, 5, -1.5, 8]")).toEqual([3, 5, -1.5, 8]);
      expect(parseSparklineSeries(" [1,2] ")).toEqual([1, 2]);
    });

    it("accepts numeric strings inside the array", () => {
      expect(parseSparklineSeries(["3", "4.5"])).toEqual([3, 4.5]);
    });

    it.each([
      [null], [undefined], [""], ["   "], ["not json"], ["{\"a\":1}"], ["5"], [5], [{}], [true],
      [[]], ["[]"], [[1, "x"]], [[1, null]], [[1, NaN]], [[1, Infinity]], [["[1]"]], [[[1], [2]]], ["[1, 2"]
    ])("returns null for %p", value => {
      expect(parseSparklineSeries(value)).toBeNull();
    });
  });

  describe("buildSparkline", () => {
    it("returns null when there is no valid series, so the cell is empty", () => {
      expect(buildSparkline(null)).toBeNull();
      expect(buildSparkline("nope")).toBeNull();
      expect(buildSparkline([])).toBeNull();
    });

    it("sizes the drawing and describes the series for assistive technologies", () => {
      const model = buildSparkline("[3, 5, -1, 8]");
      expect(model.width).toBe(SPARKLINE_WIDTH);
      expect(model.height).toBe(SPARKLINE_HEIGHT);
      expect(model.viewBox).toBe("0 0 100 24");
      expect(model.label).toBe("3, 5, -1, 8");
    });

    it("tells the type that is drawn, falling back to a line for an unknown one", () => {
      expect(buildSparkline([1, 2]).type).toBe("line");
      expect(buildSparkline([1, 2], {type: "bar"}).type).toBe("bar");
      expect(buildSparkline([1, 2], {type: "area"}).type).toBe("area");
      expect(buildSparkline([1, 2], {type: "pie"}).type).toBe("line");
    });

    it("draws a line by default: one path and one hit area with the value per point", () => {
      const model = buildSparkline([3, 5, -1, 8]);
      expect(bySelector(model, "path")).toHaveLength(1);
      expect(hitAreas(model).map(shape => shape.title)).toEqual(["3", "5", "-1", "8"]);
    });

    it("draws the line of [3, 5, -1, 8] exactly (golden)", () => {
      const model = buildSparkline([3, 5, -1, 8]);
      expect(bySelector(model, "path")[0].attrs).toEqual({
        d: "M3 13 L34.33 9 L65.67 21 L97 3",
        fill: "none",
        stroke: "#3b82f6",
        "stroke-width": 1.5,
        "stroke-linecap": "round",
        "stroke-linejoin": "round"
      });
    });

    it("draws the bars of [3, 5, -1, 8] exactly (golden)", () => {
      const model = buildSparkline([3, 5, -1, 8], {type: "bar"});
      const bars = bySelector(model, "rect").filter(shape => shape.title === undefined);
      expect(bars.map(shape => shape.attrs)).toEqual([
        {x: 8.75, y: 13, width: 12, height: 6, fill: "#3b82f6"},
        {x: 32.25, y: 9, width: 12, height: 10, fill: "#3b82f6"},
        {x: 55.75, y: 19, width: 12, height: 2, fill: "#3b82f6"},
        {x: 79.25, y: 3, width: 12, height: 16, fill: "#3b82f6"}
      ]);
    });

    it("draws the area of [1, 2, 3] exactly (golden)", () => {
      const model = buildSparkline([1, 2, 3], {type: "area"});
      const paths = bySelector(model, "path");
      expect(paths.map(shape => shape.attrs)).toEqual([
        {d: "M3 21 L3 21 L50 12 L97 3 L97 21 Z", fill: "#3b82f6", "fill-opacity": 0.25, stroke: "none"},
        {
          d: "M3 21 L50 12 L97 3",
          fill: "none",
          stroke: "#3b82f6",
          "stroke-width": 1.5,
          "stroke-linecap": "round",
          "stroke-linejoin": "round"
        }
      ]);
    });

    it("falls back to a line for an unknown type", () => {
      expect(tags(buildSparkline([1, 2], {type: "pie"}))).toEqual(tags(buildSparkline([1, 2], {type: "line"})));
    });

    it("handles negative values: the zero line, bars below it and the negative colour", () => {
      const model = buildSparkline([3, -2, 1], {type: "bar", negativeColor: "#d64550"});
      const line = bySelector(model, "line");
      expect(line).toHaveLength(1);
      const bars = bySelector(model, "rect").filter(shape => shape.title === undefined);
      expect(bars.map(shape => shape.attrs.fill)).toEqual(["#3b82f6", "#d64550", "#3b82f6"]);
      const zeroY = Number(line[0].attrs.y1);
      // The negative bar hangs from the zero line, the positive ones rise from it
      expect(bars[1].attrs.y).toBe(zeroY);
      expect(bars[0].attrs.y + bars[0].attrs.height).toBeCloseTo(zeroY, 5);
    });

    it("does not draw the zero line when every value is positive", () => {
      expect(bySelector(buildSparkline([1, 2, 3]), "line")).toHaveLength(0);
    });

    it("splits the line where it crosses zero when there is a negative colour", () => {
      const model = buildSparkline([2, -2], {negativeColor: "#d64550"});
      const strokes = bySelector(model, "path").map(shape => shape.attrs.stroke);
      expect(strokes).toEqual(["#3b82f6", "#d64550"]);
    });

    it("keeps a single colour when there is no negative colour", () => {
      const model = buildSparkline([2, -2]);
      expect(bySelector(model, "path")).toHaveLength(1);
    });

    it("draws one dot for a single value", () => {
      const model = buildSparkline([7]);
      const circles = bySelector(model, "circle");
      expect(circles).toHaveLength(1);
      expect(circles[0].attrs.cx).toBe(50);
      expect(circles[0].attrs.cy).toBe(12);
      expect(bySelector(model, "path")).toHaveLength(0);
      expect(hitAreas(model).map(shape => shape.title)).toEqual(["7"]);
    });

    it("draws one bounded bar for a single value", () => {
      const model = buildSparkline([7], {type: "bar"});
      const bars = bySelector(model, "rect").filter(shape => shape.title === undefined);
      expect(bars).toHaveLength(1);
      expect(bars[0].attrs.width).toBe(12);
      expect(bars[0].attrs.x).toBe(44);
    });

    it("draws a flat series in the middle and zero values as a thin mark", () => {
      const flat = buildSparkline([5, 5, 5]);
      expect(bySelector(flat, "path")[0].attrs.d).toBe("M3 12 L50 12 L97 12");
      const zeros = buildSparkline([0, 0], {type: "bar"});
      const bars = bySelector(zeros, "rect").filter(shape => shape.title === undefined);
      expect(bars.map(shape => shape.attrs.height)).toEqual([1, 1]);
    });

    it("marks the lowest and highest points", () => {
      const model = buildSparkline([3, 9, 1, 4], {minMax: true});
      const circles = bySelector(model, "circle").map(shape => shape.attrs);
      expect(circles.map(circle => circle.fill)).toEqual(["#d64550", "#2e9e5b"]);
      expect(circles[0].cy).toBe(21);
      expect(circles[1].cy).toBe(3);
    });

    it("puts the markers of bars on the centre of their bar", () => {
      const model = buildSparkline([3, 5, -1, 8], {type: "bar", minMax: true});
      const circles = bySelector(model, "circle").map(shape => [shape.attrs.cx, shape.attrs.cy]);
      // lowest (-1, third bar) then highest (8, last bar)
      expect(circles).toEqual([[61.75, 21], [85.25, 3]]);
    });

    it("keeps the zero line inside the drawing when every value is negative", () => {
      const line = bySelector(buildSparkline([-3, -5, -1, -8]), "line");
      expect(line).toHaveLength(1);
      expect(line[0].attrs.y1).toBe(3);
      expect(line[0].attrs.y2).toBe(3);
      const area = bySelector(buildSparkline([-3, -5, -1, -8], {type: "area"}), "line");
      expect(area[0].attrs.y1).toBe(3);
    });

    it("does not mark minimum and maximum of a flat series", () => {
      expect(bySelector(buildSparkline([4, 4, 4], {minMax: true}), "circle")).toHaveLength(0);
    });

    it("highlights the last point with a dot on lines and an outline on bars", () => {
      const line = buildSparkline([1, 2, 3], {lastPoint: true});
      const dot = bySelector(line, "circle");
      expect(dot).toHaveLength(1);
      expect(dot[0].attrs.cx).toBe(97);
      const bar = buildSparkline([1, 2, 3], {type: "bar", lastPoint: true});
      const bars = bySelector(bar, "rect").filter(shape => shape.title === undefined);
      expect(bars[2].attrs.stroke).toBe("#111827");
      expect(bars[0].attrs.stroke).toBeUndefined();
    });

    it("accepts the options as strings, like the column attributes arrive", () => {
      const model = buildSparkline([1, 2, 3], {lastPoint: "true", minMax: "false"});
      expect(bySelector(model, "circle")).toHaveLength(1);
    });

    it("uses the colour of the column and ignores values that are not plain colours", () => {
      expect(bySelector(buildSparkline([1, 2], {color: "rgb(1, 2, 3)"}), "path")[0].attrs.stroke).toBe("rgb(1, 2, 3)");
      expect(bySelector(buildSparkline([1, 2], {color: "red"}), "path")[0].attrs.stroke).toBe("red");
      expect(bySelector(buildSparkline([1, 2], {color: "\" onload=\"x"}), "path")[0].attrs.stroke).toBe("#3b82f6");
      expect(bySelector(buildSparkline([1, 2], {color: "url(#a)"}), "path")[0].attrs.stroke).toBe("#3b82f6");
    });

    it("keeps every hit area inside the drawing and in order", () => {
      const model = buildSparkline([1, 2, 3, 4]);
      const areas = hitAreas(model).map(shape => shape.attrs);
      expect(areas[0].x).toBe(0);
      areas.forEach((area, index) => {
        expect(area.fill).toBe("transparent");
        expect(area.height).toBe(SPARKLINE_HEIGHT);
        if (index > 0) {
          expect(area.x).toBeCloseTo(areas[index - 1].x + areas[index - 1].width, 5);
        }
      });
      const last = areas[areas.length - 1];
      expect(last.x + last.width).toBeCloseTo(SPARKLINE_WIDTH, 5);
    });

    it("draws a hundred points", () => {
      const series = Array.from({length: 100}, (_, index) => Math.sin(index / 5) * 10);
      const model = buildSparkline(series, {type: "bar", minMax: true, lastPoint: true});
      expect(hitAreas(model)).toHaveLength(100);
    });
  });
});
