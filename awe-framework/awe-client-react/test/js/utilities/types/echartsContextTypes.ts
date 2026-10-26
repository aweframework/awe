// Type-level assertions for utilities/echartsContext, checked by `npm run typecheck` (not run by Jest)
import {
  AWE,
  AweHints,
  ModelNode,
  PointParams,
  SERIES_INFO,
  SeriesInfo,
  asArray,
  hintsOf,
  isPie,
  pointContext,
  withoutAwe
} from "../../../../src/utilities/echartsContext";

const key: "awe" = AWE;
// @ts-expect-error the key is a literal
const wrongKey: "other" = AWE;

// A node of the model has hints, and anything else ECharts reads
const node: ModelNode = {type: "bar", awe: {xValue: "x", yValue: "y", inverted: true}};
// @ts-expect-error the hints are an object
const wrongNode: ModelNode = {awe: "x"};
// @ts-expect-error a hint has its type
const wrongHint: AweHints = {xValue: 1};

const hints: AweHints = hintsOf(node);
const noHints: AweHints = hintsOf(undefined);
hintsOf(null);
// @ts-expect-error a node is an object
hintsOf("node");

const clean: Record<string, unknown> = withoutAwe(node);
withoutAwe(undefined);
// @ts-expect-error the result is not a model node with hints
const withHints: {awe: AweHints} = withoutAwe(node);

const pie: boolean = isPie({type: "pie"});
// @ts-expect-error the series is an object
isPie("pie");

const list: number[] = asArray<number>([1]);
const unknownList: unknown[] = asArray("no");

// The series that were built are described apart
const info: SeriesInfo = {silent: true, color: "#fff", valueSuffix: " %", borderRadius: "30%", userOptions: {}};
// @ts-expect-error the radius is a size
const wrongInfo: SeriesInfo = {borderRadius: true};
SERIES_INFO.set({}, info);
const found: SeriesInfo | undefined = SERIES_INFO.get({});

// A point is described for the formats
const params: PointParams = {seriesName: "A", name: "n", value: [1, 2], percent: 10, color: "red"};
const context = pointContext(params, false, info);
const x: unknown = context.point.x;
const y: unknown = context.y;
const seriesName: unknown = context.series.name;
pointContext(params, true);
// @ts-expect-error the inverted flag is needed
pointContext(params);

export {key, wrongKey, node, wrongNode, wrongHint, hints, noHints, clean, withHints, pie, list, unknownList, info, wrongInfo, found, params, context, x, y, seriesName};
