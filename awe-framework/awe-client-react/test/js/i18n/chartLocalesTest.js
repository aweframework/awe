import {localeOptions} from "primereact/api";
import "../../../src/i18n/i18n";
import chartsEN from "../../../src/i18n/charts/charts-en.json";
import chartsES from "../../../src/i18n/charts/charts-es.json";
import chartsFR from "../../../src/i18n/charts/charts-fr.json";

// Texts that the ECharts adapter reads from the locale (echartsOption, echartsTooltip and chartFormat)
const CHART_KEYS = ["decimalPoint", "drillUpText", "months", "noData", "shortMonths", "thousandsSep", "weekdays"];

const LANGUAGES = [
  ["en-GB", chartsEN, {decimalPoint: ".", firstMonth: "January"}],
  ["es-ES", chartsES, {decimalPoint: ",", firstMonth: "Enero"}],
  ["fr-FR", chartsFR, {decimalPoint: ",", firstMonth: "Janvier"}]
];

describe("chart locales", () => {
  it.each(LANGUAGES)("should hold only the chart texts in %s", (language, texts, expected) => {
    expect(Object.keys(texts).sort()).toEqual(CHART_KEYS);
    expect(texts.months).toHaveLength(12);
    expect(texts.shortMonths).toHaveLength(12);
    expect(texts.weekdays).toHaveLength(7);
    expect(texts.decimalPoint).toBe(expected.decimalPoint);
    expect(texts.months[0]).toBe(expected.firstMonth);
  });

  it.each(LANGUAGES)("should register the chart texts of its own file in the %s locale", (language, texts) => {
    const locale = localeOptions(language);
    CHART_KEYS.forEach(key => expect(locale[key]).toEqual(texts[key]));
  });
});
