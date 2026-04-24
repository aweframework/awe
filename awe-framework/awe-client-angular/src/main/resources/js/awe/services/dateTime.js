import {aweApplication} from "../awe";
import "../directives/plugins/uiDate";
import "../directives/plugins/uiTime";
import moment from "moment";
import {getIconTemplate} from "./component";

export const calendarInputTemplate =
`<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies"
     ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" title="{{controller.title| translateMultiple}}" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label | translateMultiple}}
    </label>
    <div class="validator input-group input-append date {{::validatorGroup}} focus-target" ui-date="aweDateOptions"
         initialized="initialized" ng-readonly="controller.readonly">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="text" class="form-control {{classes}}"
             placeholder="{{controller.placeholder| translateMultiple}}" autoComplete="off" ng-click="click($event)"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" ng-disabled="controller.readonly"
             ng-model="model.selected" ng-change="component.modelChange()"
             ng-model-options="{updateOn: 'change'}" ng-focus="focus()" ng-blur="blur()"
             ng-press-enter="submit($event)"/>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
      <span class="input-group-addon add-on">
        <i class="fa fa-calendar"></i>
      </span>
    </div>
  </div>
</div>`;

export const calendarColumnTemplate =
  `<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition" title="{{component.model.values[0].title| translateMultiple}}">
    <div class="input-group input-append date input-group-{{::size}} focus-target" ui-date="aweDateOptions" initialized="initialized">
      <input type="text" class="form-control col-xs-12 {{classes}} {{component.model.values[0].style}}" placeholder="{{::component.controller.placeholder| translateMultiple}}"
             ng-disabled="component.controller.readonly" ng-model="component.model.selected" ng-focus="focus()" ng-blur="blur()"
             ng-model-options="{updateOn: 'change'}" ng-click="click($event)" ng-change="component.columnModelChange()"
             ng-press-enter="saveRow($event)" autocomplete="off"/>
      <span class="input-group-addon add-on">
        <i class="fa fa-calendar"></i>
      </span>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

export const timeInputTemplate =
`<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" title="{{controller.title| translateMultiple}}" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="validator input-group input-append date {{::validatorGroup}} focus-target">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="text" ui-time="aweTimeOptions" class="form-control add-on {{classes}}" autocomplete="off" ng-click="click($event)"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" ng-model="model.selected"
             ng-disabled="controller.readonly" placeholder="{{controller.placeholder| translateMultiple}}" ng-press-enter="submit($event)"
             ng-focus="focus()" ng-blur="blur()" ng-change="component.modelChange()" ng-model-options="{updateOn: 'change'}" initialized="initialized"/>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
      <span class="input-group-addon add-on">
        <i class="fa fa-clock-o"></i>
      </span>
    </div>
  </div>
</div>`;

export const timeColumnTemplate =
`<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition" title="{{component.model.values[0].title| translateMultiple}}">
    <div class="input-group input-group-{{::size}} input-append date focus-target">
      <input type="text" ui-time="aweTimeOptions" class="form-control add-on col-xs-12 {{classes}} {{component.model.values[0].style}}"
             ng-press-enter="saveRow($event)" autocomplete="off" ng-model="component.model.selected" ng-disabled="component.controller.readonly"
             ng-model-options="{updateOn: 'change'}" placeholder="{{::component.controller.placeholder| translateMultiple}}" ng-focus="focus()"
             ng-blur="blur()" ng-click="click($event)" ng-change="component.columnModelChange()" initialized="initialized"/>
      <span class="input-group-addon add-on">
        <i class="fa fa-clock-o"></i>
      </span>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

// Date and time service
aweApplication.factory('DateTime',
  ['Criterion', 'AweUtilities', 'AweSettings',
    /**
     * DateTime generic methods
     * @constructor Criterion constructor
     * @param {function} Criterion
     * @param {object} Utilities Utilities service
     * @param {object} $settings $settings service
     */
    function (Criterion, Utilities, $settings) {

      /**
       * DateTime constructor
       * @param {Scope} scope DateTime scope
       * @param {String} id DateTime id
       * @param {String} element DateTime element
       */
      function DateTime(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.validDates = {};
        this.validMonths = {};
        this.validYears = {};
        this.component = new Criterion(this.scope, this.id, this.element);
        let datetime = this;
        this.component.asDate = function () {
          return datetime.asDate();
        };
        this.component.asFilteredDate = function () {
          return datetime.asFilteredDate();
        };
        this.component.asTime = function () {
          return datetime.asTime();
        };
        return this.component;
      }
      DateTime.prototype = {
        /**
         * Initialize date criteria
         */
        asDate: function () {
          // Set date options
          let dateOptions = {
            container: "body",
            format: "dd/mm/yyyy",
            todayHighlight: true,
            todayBtn: 'linked',
            autoclose: true,
            enableOnReadonly: false,
            maxViewMode: 2,
            language: $settings.getLanguage()
          };

          // Define type as text
          let component = this.component;

          // Initialize criterion
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }

          // Update date options
          updateDateOptions(dateOptions, component);

          /**
           * Update the model on model changed
           */
          component.onSelectedChanged = function () {
            if (component.pluginInitialized) {
              component.updateModelSelected();
            }
          };

          /**
           * Event listeners
           */
          component.listeners = component.listeners || {};

          // Action listener definition
          Utilities.defineModelChangeListeners(component.listeners, {scope: this.scope, check: ["selected"], service: component, method: "onSelectedChanged"});

          // Store options
          this.scope.aweDateOptions = dateOptions;

          // Return initialization flag
          return true;
        },
        /**
         * Initialize filtered date criteria
         */
        asFilteredDate: function () {
          let component = this.component;
          let dateTime = this;
          // Set filtered date options
          let filteredDateOptions = {
            container: "body",
            format: "dd/mm/yyyy",
            todayHighlight: true,
            todayBtn: true,
            autoclose: true,
            enableOnReadonly: false,
            maxViewMode: 2,
            language: $settings.getLanguage()
          };

          // Define type as text
          component.pluginInitialized = false;

          // Initialize criterion
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }

          // Initialize valid dates
          let filterValidDates = function () {
            dateTime.validDates = {};
            dateTime.validMonths = {};
            dateTime.validYears = {};
            _.each(component.model.values, function (date) {
              if (!date || !date.value) {
                return;
              }

              let momentDate = moment(date.value, "DD/MM/YYYY", true);
              if (!momentDate.isValid()) {
                return;
              }

              dateTime.validDates[date.value] = true;
              dateTime.validMonths[momentDate.format('MMYYYY')] = true;
              dateTime.validYears[momentDate.format('YYYY')] = true;
            });
          };

          /**
           * Check if dates are valid
           * @param {string} date
           * @param {string} format date format
           * @param {object} filteredSet Set to check
           * @returns {boolean}
           */
          let filterDates = function (date, format, filteredSet) {
            return moment(date).format(format) in filteredSet;
          };

          // Set filtered dates in dates with a day value
          filteredDateOptions.beforeShowDay = (date) =>  filterDates(date, 'DD/MM/YYYY', dateTime.validDates);
          filteredDateOptions.beforeShowMonth = (date) =>  filterDates(date, 'MMYYYY', dateTime.validMonths);
          filteredDateOptions.beforeShowYear = (date) =>  filterDates(date, 'YYYY', dateTime.validYears);

          // Update date options
          updateDateOptions(filteredDateOptions, component);

          /**
           * Update the model on model changed
           */
          component.onModelChanged = function () {
            filterValidDates();

            if (component.pluginInitialized) {
              component.updateModelValues();
            }
          };

          /**
           * Update the model on model changed
           */
          component.onSelectedChanged = function () {
            if (component.pluginInitialized) {
              component.updateModelSelected();
            }
          };

          // Update model at initial load
          filterValidDates();

          /**
           * Event listeners
           */
          component.listeners = component.listeners || {};

          // Action listener definition
          Utilities.defineModelChangeListeners(component.listeners, {scope: this.scope, check: ["values"], service: component, method: "onModelChanged"});
          Utilities.defineModelChangeListeners(component.listeners, {scope: this.scope, check: ["selected"], service: component, method: "onSelectedChanged"});

          // Store options
          this.scope.aweDateOptions = filteredDateOptions;

          // Return initialization flag
          return true;
        },
        /**
         * Initialize time criteria
         */
        asTime: function () {
          let component = this.component;
          // Set time options
          let timeOptions = {
            minuteStep: 1,
            showSeconds: true,
            secondStep: 1,
            showInputs: false,
            showMeridian: false,
            defaultTime: false
          };

          // Initialize criterion
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }

          // Update date options
          updateDateOptions(timeOptions, component);

          // Store options
          component.scope.aweTimeOptions = timeOptions;

          // Return initialization flag
          return true;
        }
      };

      /**
       * Update date options
       *
       * @param {Object} dateOptions Date options
       * @param {Object} component of Criteria
       */
      function updateDateOptions(dateOptions, component) {
        // Check flag showWeekends
        if ('showWeekends' in component.controller && !component.controller['showWeekends']) {
          dateOptions.daysOfWeekDisabled = [0, 6];
        }
        // Check flag showFutureDates
        if ('showFutureDates' in component.controller && !component.controller['showFutureDates']) {
          // Get selected value to disable next future dates
          let  targetDate = component.model.selected;
          if (targetDate) {
            dateOptions.endDate = targetDate;
          }
        }

        // Check date format
        if ('dateFormat' in component.controller && component.controller['dateFormat'] !== null) {
          dateOptions.format = component.controller['dateFormat'];
        }

        // Check flag of Today button
        if ('showTodayButton' in component.controller && component.controller['showTodayButton'] !== null) {
          dateOptions.todayBtn = component.controller['showTodayButton'] ? "linked" : false;
        }

        // Check view mode
        if ('dateViewMode' in component.controller && component.controller['dateViewMode'] !== null) {
          dateOptions.minViewMode = component.controller['dateViewMode'];
        }
      }

      return DateTime;
    }
  ]);
