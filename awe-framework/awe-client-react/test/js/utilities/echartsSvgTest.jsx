import models from "../components/fixtures/chrTstEchartsModels.json";
import {buildEChartsOption} from "../../../src/utilities/echartsOption";
import {getEChartsLocale, isDarkTheme, renderSvg} from "../../../src/utilities/echartsSetup";

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

  describe('isDarkTheme', () => {
    afterEach(() => {
      document.body.style.removeProperty("--surface-a");
      document.body.style.backgroundColor = "";
    });

    it('should not be dark without theme', () => {
      expect(isDarkTheme(document.body)).toBe(false);
      expect(isDarkTheme(null)).toBe(false);
    });

    it('should read the surface color of the theme', () => {
      document.body.style.setProperty("--surface-a", "#1e1e1e");
      expect(isDarkTheme(document.body)).toBe(true);

      document.body.style.setProperty("--surface-a", "#ffffff");
      expect(isDarkTheme(document.body)).toBe(false);

      document.body.style.setProperty("--surface-a", "#222");
      expect(isDarkTheme(document.body)).toBe(true);
    });

    it('should fall back to the background of the page', () => {
      document.body.style.backgroundColor = "rgb(10, 10, 10)";

      expect(isDarkTheme(document.body)).toBe(true);
    });
  });
});
