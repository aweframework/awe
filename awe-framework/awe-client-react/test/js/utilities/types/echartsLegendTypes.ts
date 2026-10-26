// Type-level assertions for utilities/echartsLegend, checked by `npm run typecheck` (not run by Jest)
import {buildLegends, linkedNames, seriesNames} from "../../../../src/utilities/echartsLegend";

const t = (key: string) => key;
const series = [{name: "A", type: "bar"}, {name: "B", awe: {linkedTo: ":previous"}, silent: true}];

// A series without a name has no name
const names: Array<string | undefined> = seriesNames(series, t);
// @ts-expect-error the translator is a function
seriesNames(series, "t");
// @ts-expect-error the names are not all defined
const defined: string[] = seriesNames(series, t);

const linked: string[] = linkedNames(series, "A", t);
// @ts-expect-error the name is text
linkedNames(series, 1, t);

// The legends are one, or the visible one and the hidden one
const legends = buildLegends({show: true}, series, true, t);
const list: unknown = Array.isArray(legends) ? legends[1] : legends.data;
// @ts-expect-error the legends are not always a list
const always: unknown[] = legends;
// @ts-expect-error the chart is cartesian or not
buildLegends({}, series, "yes", t);

export {t, series, names, defined, linked, legends, list, always};
