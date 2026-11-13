// Type-level assertions for utilities/suggest, checked by `npm run typecheck` (not run by Jest)
import {getSuggestionLabel, withTextLabel, withTextLabels} from "../../../../src/utilities/suggest";

const label: string = getSuggestionLabel({value: 1, label: 2});
const labels: {value: number; label: string}[] = withTextLabels([{value: 1, label: 2}]);
const single: {value: number; label: string} = withTextLabel({value: 1, label: 2});
const many: {value: number; label: string}[] = withTextLabel([{value: 1, label: 2}]);
const typed: string = withTextLabel("typed text");

// @ts-expect-error the label of a suggestion is text
const wrongLabel: number = getSuggestionLabel({value: 1});
// @ts-expect-error the suggestions are a list
withTextLabels({value: 1});
// @ts-expect-error a suggestion is an object, not a number
getSuggestionLabel(15);
// @ts-expect-error a numeric label becomes text, it is not kept as a number
const numericLabel: number = withTextLabel({value: 1, label: 2}).label;
// @ts-expect-error a suggestion without label is returned unchanged: it has no text label
const missingLabel: string = withTextLabel({value: 1}).label;
export {label, labels, single, many, typed, wrongLabel, numericLabel, missingLabel};
