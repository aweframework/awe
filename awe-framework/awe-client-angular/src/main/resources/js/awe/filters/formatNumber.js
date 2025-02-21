import {aweApplication} from "../awe";
import numeral from "numeral";
import "numeral/locales";

// Translate multiple filter
aweApplication.filter('formatNumber', ['AweSettings', ($settings) => (number, options) => {
  let language = "es";
  const thousandsSeparator = $settings.get("numericOptions").aSep;
  if (thousandsSeparator === ".") {
    language = "en-gb";
  }

  numeral.locale(language);
  return numeral(number).format(options);
}]);