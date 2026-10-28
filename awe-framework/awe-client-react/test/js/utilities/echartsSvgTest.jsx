import models from "../components/fixtures/chrTstEchartsModels.json";
import {buildEChartsOption} from "../../../src/utilities/echartsOption";
import {getEChartsLocale, renderSvg} from "../../../src/utilities/echartsSetup";

const day = (day) => new Date(2024, 0, day).getTime();
const values = Array.from({length: 6}, (_, index) => ({
  dates: day(index + 1), serie1: 10 + index, serie2: 5 + index, serie3: 1 + index,
  names: `Name ${index}`, subserie1: index, Ord: index, serie1_1: index * 2, serie1_2: 10 * (index + 1)
}));
const locale = {decimalPoint: ".", thousandsSep: ",", noData: "No data to display", drillUpText: "Back to {series.name}"};

describe('awe-react-client/test/js/utilities/echartsSvgTest.jsx', () => {
  let warn;
  let error;

  // jsdom has no canvas, which ECharts uses to measure the texts
  beforeAll(() => {
    jest.spyOn(HTMLCanvasElement.prototype, "getContext").mockImplementation(() => ({
      font: "",
      measureText: (text) => ({width: String(text).length * 7})
    }));
  });

  afterAll(() => {
    HTMLCanvasElement.prototype.getContext.mockRestore();
  });

  beforeEach(() => {
    warn = jest.spyOn(console, "warn").mockImplementation(() => {});
    error = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    warn.mockRestore();
    error.mockRestore();
  });

  it.each(Object.keys(models))('should draw %s with the real ECharts without warnings', (name) => {
    const option = buildEChartsOption(JSON.parse(JSON.stringify(models[name])), values,
      {t: (text) => text, locale, width: 800, height: 400});

    const svg = renderSvg(option, {width: 800, height: 400}, "es-ES");

    expect(svg).toContain("<svg");
    expect(svg).toContain("width=\"800\"");
    expect(svg.length).toBeGreaterThan(500);
    expect(warn).not.toHaveBeenCalled();
    expect(error).not.toHaveBeenCalled();
  });

  it.each(Object.keys(models))('should draw %s in a small chart with the real ECharts without warnings', (name) => {
    const size = {width: 260, height: 165};
    const option = buildEChartsOption(JSON.parse(JSON.stringify(models[name])), values, {t: (text) => text, locale, ...size});

    const svg = renderSvg(option, size, "es-ES");

    expect(svg).toContain("width=\"260\"");
    expect(warn).not.toHaveBeenCalled();
    expect(error).not.toHaveBeenCalled();
  });

  it('should leave out the style element of the hover rules, which the PDF renderer (Batik) cannot read', () => {
    const option = buildEChartsOption(JSON.parse(JSON.stringify(models.ChrLinTst)), values,
      {t: (text) => text, locale, width: 800, height: 400});

    const svg = renderSvg(option, {width: 800, height: 400}, "es-ES");

    expect(svg).toContain("<path");
    expect(svg).not.toMatch(/<style/);
    expect(svg).not.toContain(":hover");
  });

  it('should draw the shadows with SVG 1.1 filters, which the PDF renderer (Batik) can read', () => {
    const model = JSON.parse(JSON.stringify(models.ChrSemiCircleTst));
    model.series[0].label = {...model.series[0].label, textShadowOffsetX: 2, textShadowOffsetY: 1, textShadowBlur: 2,
      textShadowColor: "black"};
    const option = buildEChartsOption(model, values, {t: (text) => text, locale, width: 800, height: 400});

    const svg = renderSvg(option, {width: 800, height: 400}, "es-ES");

    expect(svg).not.toContain("feDropShadow");
    expect(svg).toMatch(/<filter [^>]*>\s*<feGaussianBlur in="SourceAlpha" stdDeviation="1 1" result="blur"\/>\s*<feOffset in="blur" dx="2" dy="1" result="offset"\/>\s*<feFlood flood-color="black" flood-opacity="1"\/>\s*<feComposite in2="offset" operator="in" result="shadow"\/>\s*<feMerge><feMergeNode in="shadow"\/><feMergeNode in="SourceGraphic"\/><\/feMerge>\s*<\/filter>/);
    expect(svg).toMatch(/filter="url\(#[^)]+\)"/);
  });

  it('should draw the no data message', () => {
    const option = buildEChartsOption(JSON.parse(JSON.stringify(models.ChrLinTst)), [],
      {t: (text) => text, locale, width: 800, height: 400});

    expect(renderSvg(option, {width: 800, height: 400}, "en")).toContain("No data to display");
  });

  it('should pick the ECharts locale of a language', () => {
    expect(getEChartsLocale("es-ES")).toBe("ES");
    expect(getEChartsLocale("fr")).toBe("FR");
    expect(getEChartsLocale("en-GB")).toBe("EN");
    expect(getEChartsLocale("de-DE")).toBe("EN");
    expect(getEChartsLocale(undefined)).toBe("EN");
  });
});
