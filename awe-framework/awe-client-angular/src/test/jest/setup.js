window.mocha = {};
window.beforeEach = window.beforeEach || beforeEach;
window.afterEach = window.afterEach || afterEach;
window.beforeAll = window.beforeAll || beforeAll;
window.afterAll = window.afterAll || afterAll;

const jquery = require("jquery");
const lodash = require("lodash");

window.$ = jquery;
window.jQuery = jquery;
global.$ = jquery;
global.jQuery = jquery;
global._ = lodash;
window._ = lodash;
global.Highcharts = global.Highcharts || {};
global.HighchartsLocale = global.HighchartsLocale || {};
window.constructor = window.constructor || global.constructor;

require("angular/angular");
global.angular = window.angular;

require("angular-mocks");
delete window.mocha;

function formatAutoNumericValue(value, options = {}) {
  if (value === null || value === undefined || value === "") {
    return "";
  }

  const decimalSeparator = options.aDec || ".";
  const thousandSeparator = options.aSep || ",";
  const precision = Number.isFinite(options.mDec) ? options.mDec : 0;
  const fixed = Number(value).toFixed(precision);
  const parts = fixed.split(".");
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, thousandSeparator);

  return parts.length > 1 ? `${parts[0]}${decimalSeparator}${parts[1]}` : parts[0];
}

jquery.fn.autoNumeric = jquery.fn.autoNumeric || function autoNumeric(commandOrOptions, value) {
  if (commandOrOptions === "set") {
    const options = this.data("autoNumericOptions") || {};
    this.data("autoNumericRawValue", value);
    this.val(formatAutoNumericValue(value, options));
    return this;
  }

  if (commandOrOptions === "get") {
    const rawValue = this.data("autoNumericRawValue");
    return rawValue === null || rawValue === undefined || rawValue === "" ? "" : String(rawValue);
  }

  if (commandOrOptions === "update") {
    this.data("autoNumericOptions", value || {});
    return this;
  }

  this.data("autoNumericOptions", commandOrOptions || {});
  return this;
};

global.inject = window.inject;
global.ngModule = window.module;

afterEach(() => {
  document.body.innerHTML = "";
  jest.clearAllMocks();
  jest.useRealTimers();
});
