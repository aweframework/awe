import {fitPieLabels} from "../../../src/utilities/echartsPie";
import {buildTooltip, createValueText} from "../../../src/utilities/echartsTooltip";
import {buildLegends, linkedNames, seriesNames} from "../../../src/utilities/echartsLegend";

describe('awe-react-client/test/js/utilities/echartsHelpersTest.js', () => {
  const t = (key) => key;

  describe('fitPieLabels', () => {
    const pie = (radius, label = {show: true}) => ({
      type: "pie", radius, label, data: [{name: "A very long name of a slice", value: 3}, {name: "B", value: 1}]
    });

    it('should cap the radius by the room that the longest label leaves', () => {
      const built = pie("75%");
      fitPieLabels(built, {width: 300, height: 300});
      expect(built.radius).toBe(30);
    });

    it('should shrink the inner radius of a donut in the same proportion', () => {
      const built = pie(["50%", "100%"]);
      fitPieLabels(built, {width: 300, height: 300});
      expect(built.radius).toEqual([15, 30]);
    });

    it('should keep the radius of a pie that fits', () => {
      const built = pie("20%");
      fitPieLabels(built, {width: 1000, height: 300});
      expect(built.radius).toBe("20%");
    });

    it('should not change a pie without size, without labels or with the labels inside', () => {
      const unsized = pie("75%");
      fitPieLabels(unsized, {width: 0, height: 0});
      const hidden = pie("75%", {show: false});
      fitPieLabels(hidden, {width: 300, height: 300});
      const inside = pie("75%", {show: true, position: "inside"});
      fitPieLabels(inside, {width: 300, height: 300});
      expect([unsized.radius, hidden.radius, inside.radius]).toEqual(["75%", "75%", "75%"]);
    });

    it('should measure the text that the formatter of the label gives', () => {
      const built = pie("75%", {show: true, formatter: ({name, percent}) => `${name} ${percent}%`});
      fitPieLabels(built, {width: 300, height: 300});
      expect(built.radius).toBe(30);
    });
  });

  describe('createValueText', () => {
    it('should write the decimals, the prefix and the suffix', () => {
      const text = createValueText({numberDecimals: 1, prefix: "$", suffix: " k"}, {});
      expect(text.formatted).toBe(true);
      expect(text.write(1234.56)).toBe("$1234.6 k");
    });

    it('should write a dash when there is no value', () => {
      const text = createValueText({}, {});
      expect(text.formatted).toBe(false);
      expect([null, undefined, ""].map(text.write)).toEqual(["-", "-", "-"]);
    });

    it('should let the suffix of the series replace the one of the tooltip', () => {
      expect(createValueText({suffix: " k"}, {}, " kg").write(2)).toBe("2 kg");
    });
  });

  describe('buildTooltip', () => {
    const environment = {inverted: false, locale: {}, timeX: false, infoOf: () => ({})};

    it('should keep the tooltip of the model, confined, without hints and formatters', () => {
      expect(buildTooltip({trigger: "axis", awe: {}}, environment)).toEqual({trigger: "axis", confine: true});
    });

    it('should create the value formatter from the hints', () => {
      const built = buildTooltip({awe: {numberDecimals: 2, suffix: "%"}}, environment);
      expect(built.valueFormatter(3)).toBe("3.00%");
      expect(built.formatter).toBeUndefined();
    });

    it('should create the formatter from the formats of the tooltip', () => {
      const built = buildTooltip({awe: {pointFormat: "{series.name}: {point.y}"}}, environment);
      const text = built.formatter([{seriesName: "A", value: [1, 5], seriesIndex: 0, marker: ""}]);
      expect(text).toBe("A: 5");
    });

    it('should not show the series that are silent', () => {
      const silent = {...environment, infoOf: ({seriesIndex}) => ({silent: seriesIndex === 1})};
      const built = buildTooltip({awe: {pointFormat: "{y}"}}, silent);
      const params = [{value: [1, 5], seriesIndex: 0}, {value: [1, 7], seriesIndex: 1}];
      expect(built.formatter(params)).toBe("5");
    });
  });

  describe('legends', () => {
    const series = [{name: "A", type: "bar"}, {name: "B", silent: true, awe: {linkedTo: ":previous"}}];

    it('should give a silent linked series the name of its target', () => {
      expect(seriesNames(series, t)).toEqual(["A", "A"]);
    });

    it('should keep the name of a linked series that shows in the tooltip', () => {
      const visible = [series[0], {name: "B", awe: {linkedTo: ":previous"}}];
      expect(seriesNames(visible, t)).toEqual(["A", "B"]);
      expect(linkedNames(visible, "A", t)).toEqual(["B"]);
    });

    it('should list the series of a cartesian chart once, in the order of the model', () => {
      expect(buildLegends({show: true, awe: {}}, series, true, t)).toEqual({show: true, data: ["A"]});
    });

    it('should add a hidden legend for the linked series that show in the tooltip', () => {
      const visible = [series[0], {name: "B", awe: {linkedTo: ":previous"}}];
      expect(buildLegends({awe: {}}, visible, true, t)).toEqual([{data: ["A"]}, {show: false, data: ["B"]}]);
    });

    it('should leave the legend of a chart without axes as the model gives it', () => {
      expect(buildLegends({orient: "vertical", awe: {}}, [{type: "pie", name: "P"}], false, t)).toEqual({orient: "vertical"});
    });
  });
});
