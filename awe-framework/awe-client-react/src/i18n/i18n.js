import i18n from "i18next";
import {initReactI18next} from "react-i18next";
import Backend from "i18next-http-backend";
import datesES from "./dates/dates-es.json";
import datesFR from "./dates/dates-fr.json";
import highchartsES from "./highcharts/highcharts-es.json";
import highchartsFR from "./highcharts/highcharts-fr.json";
import highchartsEN from "./highcharts/highcharts-en.json";
import {addLocale} from "primereact/api";

i18n
  .use(initReactI18next) // passes i18n down to react-i18next
  .use(Backend);

function initLocales() {
  [
    {lang: "es-ES", locales: {...datesES, ...highchartsES}},
    {lang: "fr-FR", locales: {...datesFR, ...highchartsFR}},
    {lang: "en-GB", locales: {...highchartsEN}}
  ].forEach(l => addLocale(l.lang, l.locales));
}

initLocales();

export default i18n;
