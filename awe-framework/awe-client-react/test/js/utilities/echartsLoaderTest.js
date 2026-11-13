import {renderSvgWith} from "../../../src/utilities/echartsSvg";

describe('echartsLoader', () => {
  const load = () => {
    let loader;
    jest.isolateModules(() => {
      loader = require("../../../src/utilities/echartsLoader");
    });
    return loader.loadEcharts;
  };

  it('loads ECharts with the AWE charts, SVG renderer and locales registered, once', async () => {
    const loadEcharts = load();

    const first = await loadEcharts();

    expect(await loadEcharts()).toBe(first);
    const svg = first.renderSvg({xAxis: {type: "category", data: ["a"]}, yAxis: {}, series: [{type: "radar", data: []}, {type: "bar", data: [1]}]},
      {width: 100, height: 50}, "es-ES");
    expect(svg).toContain("<svg");
    const chart = first.echarts.init(null, null, {renderer: "svg", ssr: true, width: 10, height: 10, locale: "FR"});
    expect(chart).toBeDefined();
    chart.dispose();
  });

  it('can load again after a failed load', async () => {
    let offline = true;
    jest.doMock("echarts/charts", () => {
      if (offline) {
        throw new Error("offline");
      }
      return jest.requireActual("echarts/charts");
    });
    const loadEcharts = load();

    await expect(loadEcharts()).rejects.toThrow("offline");
    offline = false;

    await expect(loadEcharts()).resolves.toHaveProperty("echarts");
    jest.dontMock("echarts/charts");
  });
});

describe('renderSvgWith', () => {
  it('removes the style element and draws the drop shadow with SVG 1.1 primitives', () => {
    const chart = {
      setOption: jest.fn(),
      dispose: jest.fn(),
      renderToSVGString: () => '<svg><style>.a{}</style><feDropShadow dx="1" flood-color="red"/></svg>'
    };

    const svg = renderSvgWith({init: () => chart}, {}, {width: 1, height: 1});

    expect(svg).not.toContain("<style");
    expect(svg).not.toContain("feDropShadow");
    expect(svg).toContain('<feOffset in="blur" dx="1" dy="2"');
    expect(svg).toContain('flood-color="red" flood-opacity="1"');
    expect(chart.dispose).toHaveBeenCalled();
  });
});
