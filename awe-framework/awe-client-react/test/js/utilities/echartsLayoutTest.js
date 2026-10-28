import models from "../components/fixtures/chrTstEchartsModels.json";
import {buildEChartsOption} from "../../../src/utilities/echartsOption";

const clone = (value) => JSON.parse(JSON.stringify(value));
const locale = {decimalPoint: ".", thousandsSep: ",", noData: "No data to display", drillUpText: "Back"};
const SMALL = {width: 260, height: 165};
const WIDE = {width: 800, height: 400};
const names = (count, prefix = "Category") => Array.from({length: count}, (_, index) => `${prefix} number ${index}`);
const rows = (labels) => labels.map((name, index) => ({dates: name, names: name, serie1: index + 1, serie2: 1, serie3: 2}));
const build = (name, labels, size, mutate = () => {}) => {
  const model = clone(models[name]);
  mutate(model);
  return buildEChartsOption(model, rows(labels), {t: (text) => text, locale, ...size});
};
const categories = (labels, size, mutate = () => {}) => build("ChrBarTst", labels, size, model => {
  model.xAxis[0].type = "category";
  mutate(model);
});

describe('awe-react-client/test/js/utilities/echartsLayoutTest.js', () => {

  describe('data labels', () => {
    it('should hide the data labels that overlap, in bubbles and in bars', () => {
      const bubbles = build("ChrBubTst", names(20), SMALL);
      const bars = build("ChrBarHorTst", names(20), WIDE);

      expect(bubbles.series.every(serie => serie.labelLayout?.hideOverlap === true)).toBe(true);
      expect(bars.series.every(serie => serie.labelLayout?.hideOverlap === true)).toBe(true);
    });

    it('should keep the label layout that the model sets, and leave the series without labels', () => {
      const option = build("ChrBarHorTst", names(5), WIDE, model => {
        model.series[0].labelLayout = {hideOverlap: false};
        delete model.series[1].label;
      });

      expect(option.series[0].labelLayout).toEqual({hideOverlap: false});
      expect(option.series[1].labelLayout).toBeUndefined();
    });
  });

  describe('title', () => {
    it('should keep the plot below a smaller title in a small chart', () => {
      const small = build("ChrLinTst", names(3), SMALL);
      const wide = build("ChrLinTst", names(3), WIDE);

      expect(small.title.textStyle.fontSize).toBeLessThan(18);
      expect(small.title.textStyle.fontSize).toBe(13);
      // The title (and the subtitle) on top, then the plot
      expect(small.grid.top).toBeGreaterThanOrEqual(8 + 22);
      expect(small.grid.top).toBeLessThan(wide.grid.top);
      expect(wide.title.textStyle).toBeUndefined();
    });

    it('should keep the margins of a wide chart', () => {
      const grid = build("ChrLinTst", names(3), WIDE).grid;

      // Margin, title and subtitle
      expect(grid.top).toBe(16 + 28 + 20);
      expect(grid.left).toBe(16);
    });

    it('should leave the title font that the model sets', () => {
      const option = build("ChrLinTst", names(3), SMALL, model => {
        model.title.textStyle = {fontSize: 20};
      });

      expect(option.title.textStyle.fontSize).toBe(20);
    });
  });

  describe('legend', () => {
    it('should scroll so that it keeps one row, in cartesian charts and in pies', () => {
      const bars = build("ChrBarTst", names(3), SMALL, model => {
        model.legend.show = true;
      });
      const pie = build("ChrPieTst", names(6, "Slice"), SMALL);

      expect(bars.legend.type).toBe("scroll");
      expect(pie.legend.type).toBe("scroll");
    });

    it('should wrap in a chart that has room, so that every entry stays visible', () => {
      const bars = build("ChrBarTst", names(3), WIDE, model => {
        model.legend.show = true;
      });
      const pie = build("ChrPieTst", names(6, "Slice"), WIDE);

      expect(bars.legend.type).toBeUndefined();
      expect(pie.legend.type).toBeUndefined();
    });

    it('should list every entry in a printed chart, where a scrolling legend would hide the next pages', () => {
      const bars = build("ChrBarTst", names(3), {...WIDE, print: true}, model => {
        model.legend.show = true;
      });
      const pie = build("ChrPieTst", names(6, "Slice"), {...SMALL, print: true});

      expect(bars.legend.type).toBeUndefined();
      expect(pie.legend.type).toBe("plain");
    });

    it('should keep the type of the legend that the model sets', () => {
      const option = build("ChrPieTst", names(3), WIDE, model => {
        model.legend.type = "plain";
      });

      expect(option.legend.type).toBe("plain");
    });

    it('should draw smaller markers and texts in a small chart only', () => {
      const small = build("ChrPieTst", names(3), SMALL).legend;
      const wide = build("ChrPieTst", names(3), WIDE).legend;

      expect(small.itemWidth).toBeLessThan(25);
      expect(small.textStyle.fontSize).toBe(11);
      expect(wide.itemWidth).toBeUndefined();
      expect(wide.textStyle).toBeUndefined();
    });

    it('should take less room for the legend of a small chart', () => {
      const show = (model) => {
        model.legend.show = true;
        model.legend.top = "bottom";
      };
      const small = build("ChrLinTst", names(3), SMALL, show).grid;
      const smallWithout = build("ChrLinTst", names(3), SMALL).grid;
      const wide = build("ChrLinTst", names(3), WIDE, show).grid;
      const wideWithout = build("ChrLinTst", names(3), WIDE).grid;

      expect(small.bottom - smallWithout.bottom).toBeLessThan(wide.bottom - wideWithout.bottom);
    });
  });

  describe('category labels', () => {
    it('should not rotate the labels of a small chart: it hides the ones that overlap and cuts the long ones', () => {
      const label = categories(names(8), SMALL).xAxis[0].axisLabel;

      expect(label.rotate).toBeUndefined();
      expect(label.hideOverlap).toBe(true);
      expect(label.overflow).toBe("truncate");
      expect(label.width).toBeGreaterThanOrEqual(20);
      expect(label.width).toBeLessThanOrEqual(SMALL.width / 8);
    });

    it('should leave the plot a usable height in a small chart', () => {
      const grid = categories(names(8), SMALL).grid;

      expect(SMALL.height - grid.top - grid.bottom).toBeGreaterThanOrEqual(60);
    });

    it('should limit the room of the rotated labels to a fraction of the height of the chart', () => {
      const size = {width: 400, height: 280};
      const base = categories(["a"], size).grid.bottom;

      const option = categories(names(12, "A long category name"), size);

      expect(option.xAxis[0].axisLabel.rotate).toBe(45);
      expect(option.grid.bottom - base).toBeLessThanOrEqual(Math.round(size.height * 0.2));
      // The labels that are longer than that room are cut
      expect(option.xAxis[0].axisLabel.overflow).toBe("truncate");
      expect(option.xAxis[0].axisLabel.width).toBeGreaterThan(0);
    });

    it('should keep the rotation and the room of a wide chart', () => {
      const base = categories(["a"], {width: 600, height: 400}).grid.bottom;

      const option = categories(names(12), {width: 600, height: 400});

      expect(option.xAxis[0].axisLabel.rotate).toBe(45);
      expect(option.xAxis[0].axisLabel.width).toBeUndefined();
      expect(option.grid.bottom).toBe(base + 70);
    });
  });

  describe('pie', () => {
    const slices = ["adminflare", "asphalt", "clean", "default", "dust", "purple hills"];

    it('should place a small pie below the title, in the area that the title and the legend leave', () => {
      const pie = build("ChrPieTst", slices, SMALL).series[0];

      // Plot area: margins of 8, title 22 on top and the legend of 26 below
      const top = 8 + 22;
      const height = SMALL.height - top - 8 - 26;
      expect(pie.center[1]).toBe(top + height / 2);
      expect(pie.center[0]).toBe(SMALL.width / 2);
      expect(pie.radius).toBeLessThanOrEqual(height / 2);
    });

    it('should hide the outside labels when the radius is too small for them', () => {
      const small = build("ChrPieTst", slices, SMALL).series[0];

      expect(small.label.show).toBe(false);
    });

    it('should keep and fit the labels of a pie that has room, without overlaps', () => {
      const pie = build("ChrPieTst", slices, WIDE).series[0];

      expect(pie.label.show).toBe(true);
      expect(pie.labelLayout.hideOverlap).toBe(true);
    });

    it('should keep the radius and the center of a wide pie', () => {
      const pie = build("ChrPieTst", slices, WIDE).series[0];

      expect(pie.radius).toBe("60%");
      expect(pie.center).toEqual(["50%", "54%"]);
    });
  });
});
