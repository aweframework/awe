import _ from "lodash";

import {addLocale} from "primereact/api";

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
  addLocale('es', {
    firstDayOfWeek: 1,
    dayNames: ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"],
    dayNamesShort: ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"],
    dayNamesMin: ["D", "L", "M", "X", "J", "V", "S"],
    monthNames: ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"],
    monthNamesShort: ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"],
    today: 'Hoy',
    clear: 'Limpiar',
    dateFormat: 'dd/mm/yy',
    weekHeader: 'Sm'
  });

  addLocale('fr', {
    firstDayOfWeek: 1,
    dayNames: ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
    dayNamesShort: ["Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"],
    dayNamesMin: ["D", "L", "M", "X", "J", "V", "S"],
    monthNames: ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"],
    monthNamesShort: ["Jan", "Feb", "Mar", "Avr", "Mai", "Jui", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"],
    today: "Aujourd'hui",
    clear: 'Nettoyer',
    dateFormat: 'dd/mm/yy',
    weekHeader: 'Sm'
  });
}

/**
 * Transform string to date
 * @param {string} date
 * @returns {Date}
 * @memberOf Dates
 */
export function toDate(date) {
  if (date) {
    const parts = String(date).split("/");
    return new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0]));
  }
  return null;
}

/**
 * Transform date to string
 * @param {Date} date
 * @returns {string}
 * @memberOf Dates
 */
export function fromDate(date) {
  return date ? date.toLocaleDateString("es") : null;
}

/**
 * Transform date (time) to string
 * @param {Date} date
 * @returns {string}
 * @memberOf Dates
 */
export function fromTime(date) {
  return date ? date.toLocaleTimeString("es") : null;
}

/**
 * Transform string to date (time)
 * @param {string} date
 * @returns {Date}
 * @memberOf Dates
 */
export function toTime(date) {
  if (date) {
    const parts = date.split(":");
    return new Date(0, 0, 0, Number(parts[0]), Number(parts[1]), Number(parts[2]) || 0);
  }
  return null;
}

/**
 * Get available dates
 * @param model Model
 * @returns {Date[]}
 * @memberOf Dates
 */
export function getAvailableDates(model) {
  return (model || [])
    .map(data => toDate(data.value))
    .sort();
}

/**
 * Get disabled dates
 * @param availableDates
 * @returns {Date[]}
 * @memberOf Dates
 */
export function getDisabledDates(availableDates) {
  const disabledDates = [];

  // Retrieve all disabled dates in values between min and max
  let currentDate = getMinDate(availableDates);
  let maxDate = getMaxDate(availableDates);

  while (currentDate < maxDate) {
    if (!availableDates.includes(currentDate)) {
      disabledDates.push(currentDate);
    }
    currentDate.setDate(currentDate.getDate() + 1);
  }

  return disabledDates;
}

/**
 * Get max available date
 * @param availableDates
 * @returns {*}
 * @memberOf Dates
 */
export function getMaxDate(availableDates) {
  let maxDate = _.last(availableDates);
  if (maxDate) {
    let nextDate = new Date(maxDate.getTime());
    nextDate.setDate(nextDate.getDate() + 1);
    return nextDate;
  }
  return null;

}

/**
 * Get min available date
 * @param availableDates
 * @returns {*}
 * @memberOf Dates
 */
export function getMinDate(availableDates) {
  let minDate = _.head(availableDates);
  return minDate ? new Date(minDate.getTime()) : null;
}
