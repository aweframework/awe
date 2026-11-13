import {
  FORMATTERS,
  formatDate,
  formatNumber,
  formatTemplate,
  parseTemplate,
  resolvePath,
  stripHtml,
  toEChartsTimeFormat
} from "../../../src/utilities/chartFormat";

describe('awe-react-client/test/js/utilities/chartFormatTest.jsx', () => {

  describe('formatNumber', () => {
    it('should use the requested number of decimals', () => {
      expect(formatNumber(3.14159, 3)).toBe("3.142");
      expect(formatNumber(2, 2)).toBe("2.00");
      expect(formatNumber(2.5, 0)).toBe("3");
    });

    it('should keep the value as is when decimals are not requested', () => {
      expect(formatNumber(12.5)).toBe("12.5");
      expect(formatNumber(7, undefined)).toBe("7");
    });

    it('should apply the decimal point and the thousands separator of the locale', () => {
      expect(formatNumber(1234567.891, 2, {decimalPoint: ",", thousandsSep: "."})).toBe("1.234.567,89");
      expect(formatNumber(-1234.5, 1, {thousandsSep: ","})).toBe("-1,234.5");
      expect(formatNumber(999, 0, {thousandsSep: ","})).toBe("999");
    });

    it('should return an empty text for values that are not numbers', () => {
      expect(formatNumber(null, 2)).toBe("");
      expect(formatNumber(undefined, 2)).toBe("");
      expect(formatNumber("abc", 2)).toBe("");
    });

    it('should accept numeric texts', () => {
      expect(formatNumber("4.5", 2)).toBe("4.50");
    });
  });

  describe('formatDate', () => {
    const date = new Date(2024, 0, 5, 13, 4, 9, 7).getTime();

    it('should format the numeric codes of Highcharts', () => {
      expect(formatDate(date, "%Y-%m-%d")).toBe("2024-01-05");
      expect(formatDate(date, "%d/%m/%Y")).toBe("05/01/2024");
      expect(formatDate(date, "%H:%M:%S.%L")).toBe("13:04:09.007");
      expect(formatDate(date, "%y %e")).toBe("24 5");
    });

    it('should format the names with the locale', () => {
      const locale = {
        months: ["Enero"],
        shortMonths: ["Ene"],
        weekdays: ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"]
      };
      expect(formatDate(date, "%B %b", locale)).toBe("Enero Ene");
      expect(formatDate(date, "%A %a", locale)).toBe("Viernes Vie");
    });

    it('should use English names by default and keep unknown codes as they are', () => {
      expect(formatDate(date, "%b %a %q %%")).toBe("Jan Fri %q %");
    });

    it('should return an empty text for an invalid date', () => {
      expect(formatDate("not a date", "%Y")).toBe("");
      expect(formatDate(undefined, "%Y")).toBe("");
    });
  });

  describe('toEChartsTimeFormat', () => {
    it('should translate the date codes into the ECharts time template', () => {
      expect(toEChartsTimeFormat("%Y-%m-%d")).toBe("{yyyy}-{MM}-{dd}");
      expect(toEChartsTimeFormat("%e. %b %H:%M:%S")).toBe("{d}. {MMM} {HH}:{mm}:{ss}");
      expect(toEChartsTimeFormat("%B %y %A")).toBe("{MMMM} {yy} {eeee}");
    });

    it('should keep unknown text', () => {
      expect(toEChartsTimeFormat("week %q")).toBe("week %q");
    });
  });

  describe('resolvePath', () => {
    it('should resolve dotted paths', () => {
      expect(resolvePath({point: {y: 4}}, "point.y")).toBe(4);
      expect(resolvePath({y: 0}, "y")).toBe(0);
    });

    it('should return undefined for missing paths', () => {
      expect(resolvePath({point: null}, "point.y")).toBeUndefined();
      expect(resolvePath({}, "a.b.c")).toBeUndefined();
      expect(resolvePath(undefined, "a")).toBeUndefined();
    });
  });

  describe('parseTemplate', () => {
    it('should split a template into text and expressions', () => {
      expect(parseTemplate("<b>{point.name}</b>: {point.y:.2f} %")).toEqual([
        {type: "text", value: "<b>"},
        {type: "expression", expression: {type: "path", path: "point.name"}, spec: undefined},
        {type: "text", value: "</b>: "},
        {type: "expression", expression: {type: "path", path: "point.y"}, spec: ".2f"},
        {type: "text", value: " %"}
      ]);
    });

    it('should keep unbalanced braces as text', () => {
      expect(parseTemplate("a { b")).toEqual([{type: "text", value: "a { b"}]);
      expect(parseTemplate("")).toEqual([]);
      expect(parseTemplate(undefined)).toEqual([]);
    });
  });

  describe('formatTemplate', () => {
    const context = {
      series: {name: "Serie 1"},
      point: {name: "Chrome", x: 1704067200000, y: 1234.5678, z: 7, percentage: 37.456},
      y: 1234.5678,
      value: 20.5
    };

    it('should replace point and series values', () => {
      expect(formatTemplate("{point.name}: {point.y}", context)).toBe("Chrome: 1234.5678");
      expect(formatTemplate("{series.name} - {point.z}", context)).toBe("Serie 1 - 7");
    });

    it('should format numbers with decimals', () => {
      expect(formatTemplate("{point.percentage:.1f} %", context)).toBe("37.5 %");
      expect(formatTemplate("{y:.3f}", context)).toBe("1234.568");
      expect(formatTemplate("{value:.0f} ºC", context)).toBe("21 ºC");
    });

    it('should group thousands only when the comma flag is present', () => {
      const options = {locale: {decimalPoint: ",", thousandsSep: "."}};
      expect(formatTemplate("{y:,.2f}", context, options)).toBe("1.234,57");
      expect(formatTemplate("{y:.2f}", context, options)).toBe("1234,57");
    });

    it('should format dates', () => {
      const date = new Date(2024, 4, 3).getTime();
      expect(formatTemplate("{point.x:%Y-%m-%d}", {point: {x: date}})).toBe("2024-05-03");
    });

    it('should leave empty the values that do not exist', () => {
      expect(formatTemplate("[{point.unknown}] [{nothing:.2f}]", context)).toBe("[] []");
    });

    it('should keep the text of an unknown specification', () => {
      expect(formatTemplate("{point.name:zzz}", context)).toBe("Chrome");
    });

    it('should keep html unless asked to strip it', () => {
      expect(formatTemplate("<b>{point.name}</b>", context)).toBe("<b>Chrome</b>");
      expect(formatTemplate("<b>{point.name}</b><br/>{y:.1f}", context, {stripHtml: true})).toBe("Chrome 1234.6");
    });

    it('should return an empty text without template', () => {
      expect(formatTemplate(undefined, context)).toBe("");
    });
  });

  describe('template language', () => {
    const context = {
      x: "30-35", y: -1234.5, value: 4500, key: "30-35",
      point: {name: "30-35", x: "30-35", y: -1234.5, z: 7, key: "30-35", percentage: 12.5},
      series: {name: "Men", userOptions: {fullname: "Average gross salary", stack: "salary"}}
    };
    const spanish = {
      decimalPoint: ",",
      thousandsSep: ".",
      months: ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre",
        "noviembre", "diciembre"]
    };
    const format = (template, values = context, options = {locale: spanish}) => formatTemplate(template, values, options);

    it('should render the branch of a condition', () => {
      const template = "{#if (gt y 0)}{y:,.0f}€{else}{(multiply y -1):,.0f}€{/if}";

      expect(format(template)).toBe("1.235€");
      expect(format(template, {y: 4321})).toBe("4.321€");
    });

    it('should render a condition without else only when it holds', () => {
      expect(format("[{#if (lt y 0)}negative{/if}]")).toBe("[negative]");
      expect(format("[{#if (gt y 0)}positive{/if}]")).toBe("[]");
    });

    it('should use the truthiness of a value as a condition', () => {
      expect(format("{#if point.z}z={point.z}{else}no z{/if}")).toBe("z=7");
      expect(format("{#if point.z}z{else}no z{/if}", {point: {z: 0}})).toBe("no z");
      expect(format("{#if point.z}z{else}no z{/if}", {})).toBe("no z");
    });

    it('should accept a condition without parentheses', () => {
      expect(format("{#if gt value 1000}big{else}small{/if}")).toBe("big");
      expect(format("{#if eq point.name key}same{/if}")).toBe("same");
    });

    it('should nest conditions', () => {
      const template = "{#if (gt y 0)}up{else}{#if (lt y -1000)}way down{else}down{/if}{/if}";

      expect(format(template)).toBe("way down");
      expect(format(template, {y: -5})).toBe("down");
      expect(format(template, {y: 5})).toBe("up");
    });

    it('should evaluate the comparison helpers', () => {
      const check = (helper, left, right) => format(`{#if (${helper} ${left} ${right})}yes{else}no{/if}`);

      expect([check("gt", 2, 1), check("gt", 1, 1)]).toEqual(["yes", "no"]);
      expect([check("lt", 1, 2), check("lt", 1, 1)]).toEqual(["yes", "no"]);
      expect([check("ge", 1, 1), check("ge", 0, 1)]).toEqual(["yes", "no"]);
      expect([check("le", 1, 1), check("le", 2, 1)]).toEqual(["yes", "no"]);
      expect([check("eq", 3, 3), check("eq", 3, 4)]).toEqual(["yes", "no"]);
      expect([check("ne", 3, 4), check("ne", 3, 3)]).toEqual(["yes", "no"]);
    });

    it('should evaluate the arithmetic helpers', () => {
      expect(format("{multiply 3 4}")).toBe("12");
      expect(format("{divide 9 4}")).toBe("2.25");
      expect(format("{add 1.5 2}")).toBe("3.5");
      expect(format("{subtract 1 3}")).toBe("-2");
    });

    it('should call a helper without parentheses', () => {
      expect(format("{multiply value 0.001}k")).toBe("4.5k");
      expect(format("{multiply value -0.001}k", {value: -4500})).toBe("4.5k");
    });

    it('should keep a raw value exact and round only the result of a helper', () => {
      expect(format("{x} {point.x} {y}", {x: 1704067200001, point: {x: 1704067200001}, y: 0.30000000000000004}))
        .toBe("1704067200001 1704067200001 0.30000000000000004");
      expect(format("{(add y 0)}", {y: 0.30000000000000004})).toBe("0.3");
    });

    it('should not show floating point noise in a calculated value', () => {
      expect(format("{multiply value 0.001}", {value: 300})).toBe("0.3");
      expect(format("{add 0.1 0.2}")).toBe("0.3");
    });

    it('should evaluate parenthesised sub expressions with a format', () => {
      expect(format("{(multiply y -1):,.0f}")).toBe("1.235");
      expect(format("{(multiply y -1):,.2f}")).toBe("1.234,50");
      expect(format("{(add (multiply y 2) 1)}")).toBe("-2468");
      expect(format("{(y)}")).toBe("-1234.5");
    });

    it('should leave empty the result of a calculation without numbers', () => {
      expect(format("[{multiply missing 2}]")).toBe("[]");
    });

    it('should keep as text the words between braces that do not start with a helper', () => {
      expect(format("[{unknownHelper 1 2}]")).toBe("[{unknownHelper 1 2}]");
      expect(format("[{constructor 1 2}]")).toBe("[{constructor 1 2}]");
      expect(format("{#if (nothing 1 2)}a{else}b{/if}")).toBe("{#if (nothing 1 2)}a{else}b{/if}");
      expect(format("{multiply (nothing 1 2) 2}")).toBe("{multiply (nothing 1 2) 2}");
      expect(format("{Not a tag at all}")).toBe("{Not a tag at all}");
    });

    it('should read the keys of the point and of the series', () => {
      expect(format("{key} {x} {point.key} {point.name} {point.percentage:.1f} {point.z}")).toBe("30-35 30-35 30-35 30-35 12,5 7");
      expect(format("{series.name}: {series.userOptions.fullname}")).toBe("Men: Average gross salary");
      expect(format("{series.userOptions.missing}|{point.missing.deeper}")).toBe("|");
    });

    it('should group thousands with the separator of the locale', () => {
      expect(format("{value:,.0f}")).toBe("4.500");
      expect(format("{value:.0f}")).toBe("4500");
      expect(format("{value:,.0f}", {value: 4500}, {locale: {thousandsSep: " "}})).toBe("4 500");
    });

    it('should format dates with the month names of the locale', () => {
      const date = new Date(2024, 2, 15).getTime();

      expect(format("{x:%B %Y}", {x: date})).toBe("marzo 2024");
      expect(format("{x:%d/%m/%Y}", {x: date})).toBe("15/03/2024");
      expect(format("{(add x 0):%B}", {x: date})).toBe("marzo");
    });

    it('should keep as text what is not a tag of the language', () => {
      expect(format("a { b")).toBe("a { b");
      expect(format("{\"json\": 1}")).toBe("{\"json\": 1}");
      expect(format("{(multiply y}")).toBe("{(multiply y}");
      expect(format("{else}{/if}")).toBe("{else}{/if}");
      expect(format("{#if}x{/if}")).toBe("{#if}x{/if}");
    });

    it('should close the conditions that stay open', () => {
      expect(format("{#if (gt y 0)}up{else}down")).toBe("down");
      expect(format("{#if (lt y 0)}down")).toBe("down");
    });

    it('should escape the values but not the template when asked', () => {
      const values = {point: {name: "<b>R&D</b>"}};

      expect(format("<td>{point.name}</td>", values, {escapeValues: true}))
        .toBe("<td>&lt;b&gt;R&amp;D&lt;/b&gt;</td>");
      expect(format("<td>{point.name}</td>", values)).toBe("<td><b>R&D</b></td>");
    });

    it('should parse the blocks of a template', () => {
      const [text, block] = parseTemplate("a{#if (gt y 0)}{y}{else}-{/if}");

      expect(text).toEqual({type: "text", value: "a"});
      expect(block).toEqual({
        type: "if",
        condition: {type: "call", helper: "gt", args: [{type: "path", path: "y"}, {type: "number", value: 0}]},
        whenTrue: [{type: "expression", expression: {type: "path", path: "y"}, spec: undefined}],
        whenFalse: [{type: "text", value: "-"}]
      });
    });

    it('should format a long html tooltip with conditions', () => {
      const html = "<tr><td>{series.userOptions.fullname}: </td><td><b>{#if (gt y 0)}{y:,.0f}€{else}" +
        "{(multiply y -1):,.0f}€{/if}</b></td></tr>";

      expect(format(html)).toBe("<tr><td>Average gross salary: </td><td><b>1.235€</b></td></tr>");
    });

    it('should read a long run of braces as text', () => {
      const braces = "{".repeat(100000);
      const unclosed = "{a ".repeat(30000);

      expect(format(braces)).toBe(braces);
      expect(format(unclosed)).toBe(unclosed);
    });

    it('should limit the depth of the parentheses', () => {
      const deep = (depth) => `{${"(".repeat(depth)}y${")".repeat(depth)}}`;
      // Each pair of braces closes at the first closing brace, so the rest of the opening ones stays as text
      const nested = "{(".repeat(20000) + "y" + ")}".repeat(20000);

      expect(format(deep(20))).toBe("-1234.5");
      expect(format(deep(21))).toBe(deep(21));
      expect(format(nested)).toBe("{(".repeat(19999) + "-1234.5" + ")}".repeat(19999));
    });
  });

  describe('stripHtml', () => {
    it('should remove tags and turn line breaks into spaces', () => {
      expect(stripHtml("<b>Name</b>: 4 %<br/>Next")).toBe("Name: 4 % Next");
      expect(stripHtml("<span style=\"color:red\">x</span>")).toBe("x");
      expect(stripHtml(undefined)).toBe("");
    });
  });

  describe('FORMATTERS', () => {
    it('should format the value of the currency magnitude formatter', () => {
      expect(FORMATTERS.formatCurrencyMagnitude(2500000)).toBe("2.5M");
      expect(FORMATTERS.formatCurrencyMagnitude(1234)).toBe("1.23K");
      expect(FORMATTERS.formatCurrencyMagnitude(12)).toBe("12");
      expect(FORMATTERS.formatCurrencyMagnitude(0)).toBe("0");
      expect(FORMATTERS.formatCurrencyMagnitude(-3000)).toBe("-3K");
    });

    it('should move to the next magnitude when the rounded value reaches it', () => {
      expect(FORMATTERS.formatCurrencyMagnitude(999999)).toBe("1M");
      expect(FORMATTERS.formatCurrencyMagnitude(999.999)).toBe("1K");
      expect(FORMATTERS.formatCurrencyMagnitude(-999999)).toBe("-1M");
    });

    it('should return an empty text for a value that is not a number', () => {
      expect(FORMATTERS.formatCurrencyMagnitude(undefined)).toBe("");
      expect(FORMATTERS.formatCurrencyMagnitude(null)).toBe("");
      expect(FORMATTERS.formatCurrencyMagnitude(NaN)).toBe("");
    });
  });
});
