import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import "../directives/plugins/uiDateRange";
import dayjs from "dayjs";
import {getIconTemplate} from "./component";
import {parseDate} from "./dateUtils";

// Format of the dates of the criterion (the one the server reads)
export const DATE_RANGE_FORMAT = "DD/MM/YYYY";
// Suffixes of the two parameters the criterion sends
export const DATE_RANGE_FROM_SUFFIX = ".from";
export const DATE_RANGE_TO_SUFFIX = ".to";

const PRESET_SEPARATOR = /[\s,]+/;
const PRESET_PERIOD = /^(\d+)([DWMY])$/;
const PRESET_TODAY = "TODAY";
const PRESET_YEAR_TO_DATE = "YTD";
const PRESET_ALL = "ALL";
const PERIOD_UNITS = {D: "days", W: "weeks", M: "months", Y: "years"};

/**
 * Parse the list of presets of a date range criterion
 * @param {string} presets Presets separated by commas or spaces
 * @returns {string[]} Preset codes, upper-cased
 */
export function parsePresets(presets) {
  return String(presets || "")
    .split(PRESET_SEPARATOR)
    .map(preset => preset.trim().toUpperCase())
    .filter(preset => preset.length > 0);
}

/**
 * Resolve the dates of a quick preset of the date range criterion from the current date.
 * Supported presets: TODAY, YTD (year to date), ALL (no dates) and a period back from today: nD, nW, nM or nY
 * (days, weeks, months, years; for instance 7D, 1M, 3M, 12M, 3Y)
 * @param {string} code Preset code
 * @param {Date} today Current date
 * @returns {{from: string, to: string}|null} Range as dd/mm/yyyy dates or null when the preset is unknown
 */
export function resolvePreset(code, today = new Date()) {
  const preset = String(code || "").toUpperCase();
  const period = PRESET_PERIOD.exec(preset);
  const end = dayjs(today).startOf("day");
  let range = null;
  if (preset === PRESET_ALL) {
    range = {from: "", to: ""};
  } else if (preset === PRESET_TODAY) {
    range = {from: end.format(DATE_RANGE_FORMAT), to: end.format(DATE_RANGE_FORMAT)};
  } else if (preset === PRESET_YEAR_TO_DATE) {
    range = {from: dayjs(end).startOf("year").format(DATE_RANGE_FORMAT), to: end.format(DATE_RANGE_FORMAT)};
  } else if (period) {
    range = {
      from: dayjs(end).subtract(Number(period[1]), PERIOD_UNITS[period[2]]).format(DATE_RANGE_FORMAT),
      to: end.format(DATE_RANGE_FORMAT)
    };
  }
  return range;
}

/**
 * Check that the end of a range is not before its start (an open end is always valid)
 * @param {string} from Start date as dd/mm/yyyy
 * @param {string} to End date as dd/mm/yyyy
 * @returns {boolean} Range is valid
 */
export function isDateRangeValid(from, to) {
  if (!from || !to) {
    return true;
  }
  const start = parseDate(from, true);
  const end = parseDate(to, true);
  return start.isValid() && end.isValid() && !end.isBefore(start);
}

export const dateRangeInputTemplate =
`<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies"
     ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" title="{{controller.title| translateMultiple}}" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label | translateMultiple}}
    </label>
    <div class="validator input-group input-daterange date {{::validatorGroup}} focus-target" ui-date-range="aweDateOptions"
         initialized="initialized" ng-readonly="controller.readonly">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="text" class="form-control {{classes}}" autoComplete="off" ng-click="click($event)"
             placeholder="{{controller.placeholder || 'DATE_RANGE_FROM' | translateMultiple}}"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" ng-disabled="controller.readonly"
             ng-model="range.from" ng-change="onRangeChange()" ng-model-options="{updateOn: 'change'}"
             ng-focus="focus()" ng-blur="blur()" ng-press-enter="submit($event)" data-testid="${TestIds.dateRangeFrom}"/>
      <span class="input-group-addon add-on"><i class="fa fa-long-arrow-right"></i></span>
      <input type="text" class="form-control {{classes}}" autoComplete="off" ng-click="click($event)"
             placeholder="{{'DATE_RANGE_TO' | translateMultiple}}"
             ng-attr-id="{{::controller.id}}-to" ng-attr-name="{{::controller.id}}-to" ng-disabled="controller.readonly"
             ng-model="range.to" ng-change="onRangeChange()" ng-model-options="{updateOn: 'change'}"
             ng-focus="focus()" ng-blur="blur()" ng-press-enter="submit($event)" data-testid="${TestIds.dateRangeTo}"/>
      <awe-loader class="loader" skeleton="field" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
      <span class="input-group-addon add-on">
        <i class="fa fa-calendar"></i>
      </span>
    </div>
    <div class="date-range-presets" ng-if="presets.length > 0">
      <button type="button" class="btn btn-xs btn-default" ng-repeat="preset in presets" ng-class="{active: isPresetActive(preset.code)}"
              ng-disabled="controller.readonly" ng-click="applyPreset(preset.code)" ng-attr-option-id="{{preset.code}}"
              ng-attr-data-active="{{isPresetActive(preset.code)}}" data-testid="${TestIds.dateRangePreset}">{{presetLabel(preset.code)}}</button>
    </div>
  </div>
</div>`;

// Date range criterion service
aweApplication.factory('DateRange',
  ['Criterion', 'AweUtilities', 'AweSettings', 'Validator', '$translate',
    /**
     * Date range criterion: a start and an end date (sent to the server as two parameters) with quick presets
     * @param {function} Criterion Criterion constructor
     * @param {object} Utilities Utilities service
     * @param {object} $settings $settings service
     * @param {object} Validator Validator service
     * @param {object} $translate Translate service
     */
    function (Criterion, Utilities, $settings, Validator, $translate) {

      /**
       * DateRange constructor
       * @param {Scope} scope DateRange scope
       * @param {String} id DateRange id
       * @param {String} element DateRange element
       */
      function DateRange(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Criterion(this.scope, this.id, this.element);
        this.component.asDateRange = () => this.asDateRange();
        return this.component;
      }

      /**
       * Read the start and the end of the range from the model selected values
       * @param {Array|string} selected Model selected values
       * @returns {{from: string, to: string}} Range
       */
      function readRange(selected) {
        const [from, to] = Utilities.asArray(selected);
        return {from: from || "", to: to || ""};
      }

      DateRange.prototype = {
        /**
         * Initialize date range criteria
         */
        asDateRange: function () {
          let component = this.component;
          let scope = component.scope;

          // Initialize criterion
          component.pluginInitialized = false;
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }

          // The end of the range cannot be before its start
          Validator.initController(component.controller, component.address);
          component.controller.validationRules = {...component.controller.validationRules, dateRange: true};

          let applied = null;
          scope.range = readRange(component.model.selected);
          scope.presets = parsePresets(component.controller.presets)
            .filter(code => resolvePreset(code) !== null)
            .map(code => ({code}));
          scope.aweDateOptions = {
            container: "body",
            format: "dd/mm/yyyy",
            todayHighlight: true,
            todayBtn: "linked",
            autoclose: true,
            enableOnReadonly: false,
            maxViewMode: 2,
            language: $settings.getLanguage()
          };

          /**
           * Store the range in the model
           * @param {string} from Start date
           * @param {string} to End date
           */
          const setSelected = function (from, to) {
            const selected = [from || "", to || ""];
            if (!_.isEqual(selected, component.model.selected)) {
              component.model.selected = selected;
              component.modelChange();
            }
          };

          /**
           * A date has been edited
           */
          scope.onRangeChange = function () {
            setSelected(scope.range.from, scope.range.to);
          };

          /**
           * Apply a quick preset
           * @param {string} code Preset code
           */
          scope.applyPreset = function (code) {
            const range = resolvePreset(code);
            if (range) {
              applied = {code, ...range};
              scope.range.from = range.from;
              scope.range.to = range.to;
              setSelected(range.from, range.to);
            }
          };

          /**
           * A preset is active while the dates are the ones it set, so editing a date (or resetting) clears it
           * @param {string} code Preset code
           * @returns {boolean} Preset is active
           */
          scope.isPresetActive = function (code) {
            return applied !== null && applied.code === code && applied.from === scope.range.from && applied.to === scope.range.to;
          };

          /**
           * Translated label of a preset (its code when there is no translation)
           * @param {string} code Preset code
           * @returns {string} Label
           */
          scope.presetLabel = function (code) {
            const key = "DATE_RANGE_PRESET_" + code;
            const label = $translate.instant(key);
            return label === key ? code : label;
          };

          /**
           * Retrieve the criterion data: the start and the end of the range are two parameters, null when empty
           * @returns {Object} Data from criteria
           */
          component.getData = function () {
            const range = readRange(component.model.selected);
            return {
              [component.address.component + DATE_RANGE_FROM_SUFFIX]: range.from || null,
              [component.address.component + DATE_RANGE_TO_SUFFIX]: range.to || null
            };
          };

          /**
           * Retrieve the range as text
           * @returns {string} Visible value
           */
          component.getVisibleValue = function () {
            const range = readRange(component.model.selected);
            return [range.from, range.to].filter(date => date !== "").join(" - ");
          };

          /**
           * Update the dates shown when the model changes
           */
          component.onSelectedChanged = function () {
            const range = readRange(component.model.selected);
            scope.range.from = range.from;
            scope.range.to = range.to;
            if (component.pluginInitialized) {
              component.updateModelSelected();
            }
          };

          // Event listeners
          component.listeners = component.listeners || {};
          Utilities.defineModelChangeListeners(component.listeners, {scope: scope, check: ["selected"], service: component, method: "onSelectedChanged"});

          // Return initialization flag
          return true;
        }
      };

      return DateRange;
    }
  ]);
