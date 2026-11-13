/**
 * @jest-environment node
 */
import models from "../components/fixtures/chrTstEchartsModels.json";
import {DEFAULT_SIZE, renderChart} from "../../../src/renderer/chartRenderer";

const day = (day) => Date.UTC(2024, 0, day);
const values = Array.from({length: 6}, (_, index) => ({
  dates: day(index + 1), serie1: 10 + index, serie2: 5 + index, serie3: 1 + index,
  names: `Name ${index}`, subserie1: index, Ord: index, serie1_1: index * 2, serie1_2: 10 * (index + 1)
}));
const copy = (model) => JSON.parse(JSON.stringify(model));

describe('awe-react-client/test/js/renderer/chartRendererTest.js', () => {
  let warn;
  let error;

  beforeEach(() => {
    warn = jest.spyOn(console, "warn").mockImplementation(() => {});
    error = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    warn.mockRestore();
    error.mockRestore();
  });

  it('should run without a DOM, like the chart server', () => {
    expect(typeof document).toBe("undefined");
    expect(typeof window).toBe("undefined");
  });

  it.each(Object.keys(models))('should render %s to an SVG image without warnings', (name) => {
    const svg = renderChart({infile: copy(models[name]), values, width: 800, height: 300, language: "en-GB"});

    expect(svg).toMatch(/^<svg/);
    expect(svg).toContain("width=\"800\"");
    expect(svg).toContain("height=\"300\"");
    expect(svg.length).toBeGreaterThan(500);
    expect(warn).not.toHaveBeenCalled();
    expect(error).not.toHaveBeenCalled();
  });

  it('should use the size of the Highcharts export server when the request has none', () => {
    const svg = renderChart({infile: copy(models.ChrLinTst), values});

    expect(DEFAULT_SIZE).toEqual({width: 600, height: 400});
    expect(svg).toContain("width=\"600\"");
    expect(svg).toContain("height=\"400\"");
  });

  it('should translate the texts of the chart with the translations of the request', () => {
    const model = {...copy(models.ChrLinTst), title: {text: "CHART_TITLE"}};

    const svg = renderChart({infile: model, values, translations: {CHART_TITLE: "Monthly sales"}});

    expect(svg).toContain("Monthly sales");
    expect(svg).not.toContain("CHART_TITLE");
  });

  it('should keep a text without translation as it is', () => {
    const model = {...copy(models.ChrLinTst), title: {text: "Untranslated title"}};

    expect(renderChart({infile: model, values})).toContain("Untranslated title");
  });

  it.each([
    ["es-ES", "No hay datos a mostrar"],
    ["fr-FR", "Aucune donnée à afficher"],
    ["en-GB", "No data to display"],
    [undefined, "No data to display"],
    ["de-DE", "No data to display"]
  ])('should write the empty chart message in the language %s', (language, message) => {
    expect(renderChart({infile: copy(models.ChrLinTst), values: [], language})).toContain(message);
  });

  it('should format the numbers with the separators of the language', () => {
    const model = copy(models.ChrBarTst);
    const big = values.map((row, index) => ({...row, serie1: 1234567.5 + index}));
    model.series = model.series.map(serie => ({...serie, label: {show: true}, awe: {...serie.awe, labelFormat: "{y:,.1f}"}}));

    const spanish = renderChart({infile: copy(model), values: big, language: "es-ES"});
    const english = renderChart({infile: copy(model), values: big, language: "en-GB"});

    expect(spanish).toContain("1.234.567,5");
    expect(english).toContain("1,234,567.5");
  });

  it('should bind a series to its own rows when the request gives them', () => {
    const model = copy(models.ChrPieTst);
    model.series[0].awe = {...model.series[0].awe, values: [{names: "Own slice", serie1: 100}]};

    const svg = renderChart({infile: model, values});

    expect(svg).toContain("Own slice");
    expect(svg).not.toContain("Name 0");
  });

  it('should fail when the request has no chart', () => {
    expect(() => renderChart({values})).toThrow("The request has no chart (infile)");
  });
});
