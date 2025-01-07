import {aweApplication} from "../awe";

// Translate multiple filter
aweApplication.filter('translateMultiple', ['$translate', ($translate) => {
  let  filter = (value) => String(value || "").split(" ").map(text => $translate.instant(text)).join(" ");
  filter.$stateful = true;
  return filter;
}]);
