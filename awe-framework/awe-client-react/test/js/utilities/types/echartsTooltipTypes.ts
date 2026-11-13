// Type-level assertions for utilities/echartsTooltip, checked by `npm run typecheck` (not run by Jest)
import {TooltipEnvironment, TooltipHints, buildTooltip, createValueText} from "../../../../src/utilities/echartsTooltip";

const hints: TooltipHints = {numberDecimals: 2, prefix: "$", suffix: " %", pointFormat: "{y}", useHTML: true};
// @ts-expect-error the decimals are a number
const wrongHints: TooltipHints = {numberDecimals: "2"};

// The writer gives text for any value
const text = createValueText(hints, {thousandsSep: ","}, " kg");
const written: string = text.write(1);
const asked: boolean = text.formatted;
createValueText({}, {});
// @ts-expect-error the hints are an object
createValueText("hints", {});

const environment: TooltipEnvironment = {inverted: false, locale: {}, timeX: true, infoOf: () => ({silent: true})};
// @ts-expect-error the environment needs the info of each series
const noInfo: TooltipEnvironment = {inverted: false, locale: {}};

const tooltip: Record<string, unknown> = buildTooltip({trigger: "axis", awe: hints}, environment);
// @ts-expect-error the environment is needed
buildTooltip({}, undefined);

export {hints, wrongHints, text, written, asked, environment, noInfo, tooltip};
