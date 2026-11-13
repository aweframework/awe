import {resolvePivotLocale} from "../../../../main/resources/js/awe/directives/plugins/pivotLocale";

describe("resolvePivotLocale", () => {
  const locale = {aggregators: {}, renderers: {}, localeStrings: {}};

  it("keeps the language when the pivot registers it as is", () => {
    expect(resolvePivotLocale("en-GB", {"en-GB": locale, en: locale})).toBe("en-GB");
  });

  it("uses the primary subtag of the AWE language, as the pivot language bundles register it", () => {
    expect(resolvePivotLocale("en-GB", {en: locale})).toBe("en");
    expect(resolvePivotLocale("es-ES", {en: locale, es: locale})).toBe("es");
    expect(resolvePivotLocale("fr-FR", {en: locale, fr: locale})).toBe("fr");
  });

  it("falls back to English when the language has no pivot locale", () => {
    expect(resolvePivotLocale("de-DE", {es: locale, en: locale})).toBe("en");
  });

  it("falls back to any registered locale when English is not registered", () => {
    expect(resolvePivotLocale("eu-ES", {es: locale})).toBe("es");
  });

  it("falls back to English when no language is set", () => {
    expect(resolvePivotLocale(null, {en: locale})).toBe("en");
  });
});
