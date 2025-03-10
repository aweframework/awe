import {addLocale} from "primereact/api";
import es from "./dates/dates-es.json";
import fr from "./dates/dates-fr.json";

/**
 * Date utility functions
 * @category Utilities
 * @namespace Dates
 */

/**
 * Init date locales
 * @memberOf Dates
 */
export function initDateLocales() {
  [
    {lang: "es", locale: es},
    {lang: "fr", locale: fr}
  ].forEach(l => addLocale(l.lang, l.locale));
}
