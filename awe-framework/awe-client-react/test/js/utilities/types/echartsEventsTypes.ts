// Type-level assertions for utilities/echartsEvents, checked by `npm run typecheck` (not run by Jest)
import {ChartZoom, DataZoomAction, EventChart, ZoomRange, parseChartOptions, pointOf, zoomOf, zoomRequests} from "../../../../src/utilities/echartsEvents";

// A point is read from the event of ECharts, whatever it has
const point: {x: unknown; y: unknown} = pointOf({value: [1, 2]}, true);
pointOf(undefined);
pointOf({name: "A", value: 3});
// @ts-expect-error the inverted flag is a boolean
pointOf({}, "yes");

// The zoom has a range for each axis
const chart: EventChart = {getOption: () => ({dataZoom: [{xAxisIndex: [0], start: 0, end: 50}]})};
const zoom: ZoomRange = zoomOf(chart);
const min: unknown = zoom.xMin;
zoomOf(undefined, true);
// @ts-expect-error the chart is an object
zoomOf("chart");

// The options come as an object or as JSON
const options: Record<string, unknown> = parseChartOptions('{"a": 1}');
parseChartOptions({a: 1});
parseChartOptions(undefined);

// The requests are actions of ECharts
const wanted: ChartZoom = {x: {min: 1, max: 2}, y: {min: null, max: null}};
const requests: DataZoomAction[] = zoomRequests(chart, wanted, false);
const type: "dataZoom" = requests[0].type;
const index: number = requests[0].dataZoomIndex;
zoomRequests(undefined, undefined);
// @ts-expect-error the range is a number or null
const wrongZoom: ChartZoom = {x: {min: "a"}};

export {point, chart, zoom, min, options, wanted, requests, type, index, wrongZoom};
