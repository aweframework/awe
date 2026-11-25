import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {getIcon} from "../services/icon";
import {buildMonthView, getInitialMonth, readMonthDates, shiftMonth} from "awe-client-react/utilities/monthView";

const template = `<div class="month-view {{::controller.style}}" ng-attr-id="{{::monthViewId}}" role="group" ng-attr-aria-labelledby="{{::monthViewId}}-title" ng-show="controller.visible" ui-dependency="dependencies" ng-cloak>
  <div class="month-view-head">
    <button type="button" class="month-view-nav" ng-attr-aria-label="{{view.previous}}" ng-attr-title="{{view.previous}}" ng-click="move(-1)">${getIconTemplate("", "span", "icons.previous")}</button>
    <strong class="month-view-title" ng-attr-id="{{::monthViewId}}-title" aria-live="polite" ng-bind="view.title"></strong>
    <button type="button" class="month-view-nav" ng-attr-aria-label="{{view.next}}" ng-attr-title="{{view.next}}" ng-click="move(1)">${getIconTemplate("", "span", "icons.next")}</button>
  </div>
  <table class="month-view-days">
    <thead><tr><th ng-repeat="day in view.weekdays track by $index" scope="col" ng-attr-abbr="{{day.long}}" ng-bind="day.narrow"></th></tr></thead>
    <tbody>
      <tr ng-repeat="week in view.weeks track by week.key"><td ng-repeat="day in week.days track by day.key" ng-class="{'is-weekend': day.weekend, 'is-excluded': day.excluded, 'is-today': day.today}" ng-attr-aria-label="{{day.label || undefined}}" ng-attr-title="{{day.title || undefined}}" ng-attr-aria-current="{{day.today ? 'date' : undefined}}" ng-bind="day.day"></td></tr>
    </tbody>
  </table>
  <div class="month-view-legend"><span class="month-view-swatch" aria-hidden="true"></span><span ng-bind="view.legend"></span></div>
  <div class="month-view-strip" role="group" ng-attr-aria-label="{{view.strip}}">
    <button ng-repeat="month in view.months track by month.month" type="button" ng-attr-aria-pressed="{{month.pressed}}" ng-attr-aria-label="{{month.label}}" ng-click="show(month.month)"><span ng-bind="month.short"></span><span class="month-view-count" ng-class="{'is-empty': !month.count}" ng-bind="month.count"></span></button>
  </div>
</div>`;

// Month view directive (#993): a read-only month, the week starting on Monday, with the dates of a grid of the same
// screen struck through, and the strip of the months of the year with the number of dates of each one; clicking a month
// of the strip shows it. The widget parameters name the grid ("grid"), its date column ("dateColumn") and, optionally,
// the column with the name of each date ("nameColumn", the tooltip of the day). It reads the current rows of the grid
// model, where the grid keeps the rows added or changed and not saved yet, and draws again when they change: no server
// call. It opens on the month of the next date from today, else on the month of today, and follows the rows until the
// user moves to another month. The "legend" parameter replaces the locale key of the legend. The rules are shared with
// the React engine (awe-client-react/utilities/monthView)
aweApplication.directive('aweMonthView',
  ['Component', 'Control', 'AweSettings', '$filter',
    function (Component, Control, $settings, $filter) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          monthViewId: '@'
        },
        link: function (scope) {
          const component = new Component(scope, scope.monthViewId);
          if (!component.asComponent()) {
            return false;
          }

          const translate = $filter('translateMultiple');
          // The month the user moved to: until then the view follows the dates of the grid
          let chosen = null;
          let dates = [];
          let shown = null;
          scope.icons = {previous: getIcon("lucide:chevron-left"), next: getIcon("lucide:chevron-right")};

          const parameters = () => component.controller.parameters || {};

          /**
           * Read the dates of the current rows of the grid
           * @returns {object[]} Dates
           */
          const readDates = () => {
            const {grid, dateColumn, nameColumn} = parameters();
            const model = grid ? Control.getAddressModel({view: component.address.view, component: grid}) : null;
            return readMonthDates(model?.values || [], {dateColumn, nameColumn});
          };

          /**
           * Build what the template draws from the dates and the month shown
           */
          const render = () => {
            const today = new Date();
            shown = chosen || getInitialMonth(dates, today);
            scope.view = buildMonthView({dates, shown, today, language: $settings.getLanguage(), translate,
              legend: parameters().legend});
          };

          /**
           * Move to the previous or to the next month
           * @param {number} delta Months to move
           */
          scope.move = delta => {
            chosen = shiftMonth(shown, delta);
            render();
          };

          /**
           * Show a month of the year shown (the strip)
           * @param {number} month Month (0 to 11)
           */
          scope.show = month => {
            chosen = {year: shown.year, month};
            render();
          };

          // A cell edited, a row added or deleted and a reload change the rows of the grid model: the view draws again
          // when the dates they give change
          scope.$watch(() => JSON.stringify(dates = readDates()), render);
          component.listeners["languageChanged"] = scope.$on("languageChanged", render);
          return true;
        }
      };
    }
  ]);
