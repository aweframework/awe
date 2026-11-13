import i18n from "i18next";
import {initReactI18next} from "react-i18next";
import Backend from "i18next-http-backend";
import datesES from "./dates/dates-es.json";
import datesFR from "./dates/dates-fr.json";
import chartsES from "./charts/charts-es.json";
import chartsFR from "./charts/charts-fr.json";
import chartsEN from "./charts/charts-en.json";
import {addLocale} from "primereact/api";

i18n
  .use(initReactI18next) // passes i18n down to react-i18next
  .use(Backend);

function initLocales() {
  [
    {lang: "es-ES", locales: {...datesES, ...chartsES}},
    {lang: "fr-FR", locales: {...datesFR, ...chartsFR}},
    {lang: "en-GB", locales: {...chartsEN}}
  ].forEach(l => addLocale(l.lang, l.locales));
}

initLocales();

export default i18n;
