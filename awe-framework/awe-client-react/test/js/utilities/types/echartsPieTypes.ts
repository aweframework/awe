// Type-level assertions for utilities/echartsPie, checked by `npm run typecheck` (not run by Jest)
import {PieSeries, fitPieLabels} from "../../../../src/utilities/echartsPie";

const pie: PieSeries = {type: "pie", data: [{name: "A", value: 1}], radius: ["40%", "75%"], label: {show: true}};
// @ts-expect-error a pie has its data
const noData: PieSeries = {type: "pie"};

// The radius is changed in place, there is no result
const result: void = fitPieLabels(pie, {width: 400, height: 300});
// @ts-expect-error the size is needed
fitPieLabels(pie, {width: 400});
// @ts-expect-error the pie is an object
fitPieLabels("pie", {width: 400, height: 300});

export {pie, noData, result};
