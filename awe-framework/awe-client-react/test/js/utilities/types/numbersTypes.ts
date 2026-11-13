// Type-level assertions for utilities/numbers, checked by `npm run typecheck` (not run by Jest)
import {formatNumber, getFirstDefinedValueAsNumber, isNumber, translateNumberFormat} from "../../../../src/utilities/numbers";

const first: number | undefined = getFirstDefinedValueAsNumber(undefined, "3", 4);
const formatted: string = formatNumber(1234.5, {precision: 2, digitGroupSeparator: ".", decimalCharacter: ","});
const parts = translateNumberFormat({currencySymbol: "€", pSign: "s"});
const locale: string = parts.locale;
const grouping: boolean = parts.useGrouping;
const prefix: string = parts.prefix;
const isNum: boolean = isNumber("1");
declare const unknownValue: unknown;
if (isNumber(unknownValue)) {
  const narrowed: number = unknownValue;
  narrowed.toFixed();
}

// @ts-expect-error the number format is an object
translateNumberFormat("#,##0.00");
// @ts-expect-error the precision is a number or a numeric text
formatNumber(1, {precision: {}});
// @ts-expect-error the separators are text
formatNumber(1, {digitGroupSeparator: 1});
// @ts-expect-error the first defined value is a number or undefined, never text
const wrong: string = getFirstDefinedValueAsNumber(1);
export {first, formatted, locale, grouping, prefix, isNum, wrong};
