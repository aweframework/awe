import {aweApplication} from "../awe";
import numeral from "numeral";
import "numeral/locales";

// Translate multiple filter
aweApplication.filter('formatNumber', ['AweSettings', ($settings) => (number, options) => {
  numeral.locale($settings.getLanguage());
  return numeral(number).format(options);
}]);