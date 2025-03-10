import i18n from "i18next";
import {initReactI18next} from "react-i18next";
import Backend from "i18next-http-backend";
import {initDateLocales} from "./dates";
import "./highcharts";

i18n
  .use(initReactI18next) // passes i18n down to react-i18next
  .use(Backend);

// Init date locales
initDateLocales();

export default i18n;
