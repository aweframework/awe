// Type-level assertions for utilities/echartsSetup, checked by `npm run typecheck` (not run by Jest)
import {echarts, getEChartsLocale, isDarkTheme, renderSvg} from "../../../../src/utilities/echartsSetup";

const locale: "ES" | "FR" | "EN" = getEChartsLocale("es-ES");
getEChartsLocale(undefined);
// @ts-expect-error the language is text
getEChartsLocale(1);

const svg: string = renderSvg({series: []}, {width: 100, height: 50}, "es");
renderSvg({series: []}, {width: 100, height: 50});
// @ts-expect-error the size is needed
renderSvg({series: []});
// @ts-expect-error the option is an object
renderSvg("option", {width: 1, height: 1});

const dark: boolean = isDarkTheme(document.body);
isDarkTheme(null);
// @ts-expect-error the element is an element
isDarkTheme("body");

// The library is the registered one
const chart = echarts.init(null, null, {renderer: "svg", ssr: true, width: 1, height: 1});

export {locale, svg, dark, chart};
