// Type-level assertions for utilities/echartsOption, checked by `npm run typecheck` (not run by Jest)
import {
  ChartContext,
  ChartOption,
  PALETTE,
  PlotArea,
  buildEChartsOption,
  findDrilldownId,
  hasAweKeys,
  stripAwe
} from "../../../../src/utilities/echartsOption";

const palette: string[] = PALETTE;
// @ts-expect-error the palette is text
const numbers: number[] = PALETTE;

const present: boolean = hasAweKeys({a: [{awe: {}}]});
hasAweKeys(null);
// @ts-expect-error the answer is not text
const text: string = hasAweKeys({});

// The hints are removed from any node, and the result is the same kind of value
const clean: unknown = stripAwe({a: 1, awe: {}});
const typed: number[] = stripAwe([1, 2]);
// @ts-expect-error a copy of a list is not text
const wrong: string = stripAwe([1]);

// The series of a drilldown is found by its identifier, null when there is none
const id: string | null = findDrilldownId({series: [{id: "a", awe: {drilldown: "b"}}]}, "a");
findDrilldownId(undefined, "a");
// @ts-expect-error the identifier is text
findDrilldownId({}, 1);
// @ts-expect-error the answer can be null
const always: string = findDrilldownId({}, "a");

// The context has what the chart needs to know about its environment
const context: ChartContext = {
  t: (key) => key, locale: {noData: "No data", drillUpText: "Back", decimalPoint: ","}, width: 800, height: 400,
  drill: {from: "a", to: "b"}, onBack: () => undefined, dark: true
};
const lean: ChartContext = {t: (key) => key, width: 0, height: 0, drill: null};
// @ts-expect-error the translator is needed
const noTranslator: ChartContext = {width: 0, height: 0};
// @ts-expect-error the size is a number
const wrongSize: ChartContext = {t: (key) => key, width: "800", height: 400};

// The plot area is measured in pixels
const area: PlotArea = {left: 16, top: 16, width: 100, height: 100};
// @ts-expect-error the plot area needs its size
const noArea: PlotArea = {left: 0, top: 0};

// The option is built from the model, and its series are always a list
const option: ChartOption = buildEChartsOption({series: []}, [{x: 1}], context);
const series: unknown[] = option.series;
buildEChartsOption(undefined, [], context);
buildEChartsOption(null, [], context);
// @ts-expect-error the values are rows
buildEChartsOption({}, "rows", context);
// @ts-expect-error the context is needed
buildEChartsOption({}, []);

export {palette, numbers, present, text, clean, typed, wrong, id, always, context, lean, noTranslator, wrongSize, area, noArea, option, series};
