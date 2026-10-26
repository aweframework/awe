// Type-level assertions for utilities/echartsBars, checked by `npm run typecheck` (not run by Jest)
import {BarEnvironment, roundBars} from "../../../../src/utilities/echartsBars";

const environment: BarEnvironment = {inverted: false, width: 400, height: 300};
// @ts-expect-error the size is needed
const noSize: BarEnvironment = {inverted: false};

// The bars are rounded in place, there is no result
const result: void = roundBars([{type: "bar", data: [[0, 1]]}], environment);
roundBars([{type: "bar", data: [{value: [0, 1]}], stack: "s"}], environment);
// @ts-expect-error the series are a list
roundBars({type: "bar", data: []}, environment);
// @ts-expect-error a series has points
roundBars([{type: "bar"}], environment);

export {environment, noSize, result};
