// Type-level assertions for utilities/chartFormat, checked by `npm run typecheck` (not run by Jest)
import {
  ChartLocale,
  FORMATTERS,
  FormatOptions,
  escapeHtml,
  formatDate,
  formatNumber,
  formatTemplate,
  parseTemplate,
  resolvePath,
  stripHtml,
  toEChartsTimeFormat
} from "../../../../src/utilities/chartFormat";

// The locale is optional in all its parts
const locale: ChartLocale = {decimalPoint: ",", thousandsSep: ".", months: ["Enero"]};
// @ts-expect-error the separators are text
const wrongLocale: ChartLocale = {decimalPoint: 1};

// The named formatters take a number
const formatted: string = FORMATTERS.formatCurrencyMagnitude(1000);
// @ts-expect-error a formatter takes a number
FORMATTERS.formatCurrencyMagnitude("1000");

// A number is formatted from a number or a text, and the result is text
const number: string = formatNumber("4.5", 2, locale);
formatNumber(1);
// @ts-expect-error the decimals are a number
formatNumber(1, "2");
// @ts-expect-error the locale is an object
formatNumber(1, 2, "es");

// A date is formatted from a timestamp, a text or a date
const date: string = formatDate(new Date(), "%Y", locale);
formatDate(1, "%Y");
formatDate("1", "%Y");
// @ts-expect-error the date is not a boolean
formatDate(true, "%Y");

const pattern: string = toEChartsTimeFormat("%Y-%m-%d");
const escaped: string = escapeHtml(1);
const plain: string = stripHtml("<b>a</b>");

// A template is formatted with a context and options
const options: FormatOptions = {locale, stripHtml: true, escapeValues: true};
const text: string = formatTemplate("{a}", {a: 1}, options);
formatTemplate("{a}", {a: 1});
// @ts-expect-error the template is text
formatTemplate(1, {});
// @ts-expect-error the options are an object
formatTemplate("a", {}, true);
// @ts-expect-error the options are typed
const wrongOptions: FormatOptions = {stripHtml: "yes"};

// The parser and the resolver are the ones of chartTemplate
parseTemplate("a");
const resolved: unknown = resolvePath({}, "a");

export {locale, wrongLocale, formatted, number, date, pattern, escaped, plain, options, text, wrongOptions, resolved};
