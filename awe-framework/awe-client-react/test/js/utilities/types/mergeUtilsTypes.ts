// Type-level assertions for utilities/mergeUtils, checked by `npm run typecheck` (not run by Jest)
import {calculateDeltas, mergeComponentState} from "../../../../src/utilities/mergeUtils";

const base = {attributes: {visible: true}, model: {values: [{value: 1}]}};
const merged: {attributes?: Record<string, unknown>; model?: Record<string, unknown>} = mergeComponentState(base, {attributes: {visible: false}});
mergeComponentState(undefined, {model: {}});
mergeComponentState(base);
const deltas: Record<string, unknown> = calculateDeltas(base, {attributes: {visible: false}});
calculateDeltas(null, {model: {}});

// @ts-expect-error the attributes are an object
mergeComponentState(base, {attributes: "hidden"});
// @ts-expect-error the current state is required
calculateDeltas(base);
// @ts-expect-error the stored model is an object
calculateDeltas(base, {storedModel: 5});
export {merged, deltas};
