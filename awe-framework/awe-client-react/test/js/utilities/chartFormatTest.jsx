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
        {type: "expression", path: "point.name", spec: undefined},
        {type: "text", value: "</b>: "},
        {type: "expression", path: "point.y", spec: ".2f"},
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
