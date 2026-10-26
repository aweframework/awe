// Type-level assertions for utilities/echartsSeries, checked by `npm run typecheck` (not run by Jest)
import {ActionSerie, getEChartsSerie, mergeSeriesPoints} from "../../../../src/utilities/echartsSeries";

// The series of an action may bring its translation or its Highcharts fields
const serie: ActionSerie = {id: "s", type: "column", xValue: "x", yValue: "y", data: [[1, 2]], echarts: {type: "bar"}};
// @ts-expect-error the data are points
const wrongSerie: ActionSerie = {data: "points"};

const built: Record<string, unknown> = getEChartsSerie(serie, true);
getEChartsSerie({}, false);
// @ts-expect-error the inverted flag is needed
getEChartsSerie(serie);

const rows: Array<Record<string, unknown>> = mergeSeriesPoints([{x: 1}], [serie]);
// @ts-expect-error the values are rows
mergeSeriesPoints("rows", [serie]);

export {serie, wrongSerie, built, rows};
