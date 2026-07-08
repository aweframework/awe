import {Calendar} from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import listPlugin from '@fullcalendar/list';
import interactionPlugin from '@fullcalendar/interaction';
import esLocale from '@fullcalendar/core/locales/es';
import frLocale from '@fullcalendar/core/locales/fr';
import enGbLocale from '@fullcalendar/core/locales/en-gb';
import euLocale from '@fullcalendar/core/locales/eu';

/**
 * Event calendar widget
 *
 * Custom AngularJS widget embedded through the AWE <widget> element. It mounts
 * FullCalendar (a third-party library) inside the widget, bringing its own
 * JavaScript and styles. Purpose: verify the open widget mechanism end to end
 * and prove the vision of extending the AWE UI with any framework's components
 * plus their styles, without enabling components one by one.
 *
 * Screen usage: <widget type="event-calendar" id="myCalendar" style="expand"/>
 *   -> renders <awe-event-calendar event-calendar-id="myCalendar">
 *   -> resolved by this directive (aweEventCalendar)
 *
 * The directive integrates with the framework lifecycle through the Component
 * service, exactly like the built-in fileManager / fileViewer widgets. Events
 * are kept in memory; persisting them would go through the widget server-action.
 *
 * `aweApplication` is the global AngularJS module exposed by the main AWE bundle
 * (see awe.js: self.aweApplication). External widget bundles reference it as a
 * global, so no import is required for it.
 */
aweApplication.directive('aweEventCalendar',
  ['Component', '$timeout', 'AweSettings',
    function (Component, $timeout, $settings) {

      const EVENT_COLORS = ['#1b809e', '#2e7d32', '#c1440e', '#6a1b9a', '#f9a825'];

      // Map AWE language codes (locals-<code>.js) to FullCalendar locales
      const LOCALES_BY_CODE = {
        'es-ES': esLocale,
        'fr-FR': frLocale,
        'en-GB': enGbLocale,
        'eu-ES': euLocale
      };
      const LOCALES_BY_PREFIX = {es: esLocale, fr: frLocale, en: enGbLocale, eu: euLocale};

      /**
       * Resolve the FullCalendar locale from the AWE-configured language, so the
       * calendar follows the framework locale (not the browser language).
       * @param {String} [language] AWE language code (e.g. 'en-GB'); defaults to the current one
       * @returns {Object} FullCalendar locale
       */
      function resolveLocale(language) {
        const code = language || $settings.getLanguage();
        if (code && LOCALES_BY_CODE[code]) {
          return LOCALES_BY_CODE[code];
        }
        const prefix = (code || 'en').slice(0, 2).toLowerCase();
        return LOCALES_BY_PREFIX[prefix] || enGbLocale;
      }

      /**
       * Format a date as an input[type=date] value (YYYY-MM-DD)
       * @param {Date} date Source date
       * @returns {String} Date value
       */
      function toDateValue(date) {
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return date.getFullYear() + '-' + month + '-' + day;
      }

      /**
       * Truncate a date to its day (00:00), returning a new Date
       * @param {Date} date Source date
       * @returns {Date} Date at midnight
       */
      function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
      }

      /**
       * Parse a 'YYYY-MM-DD' day string into a local Date
       * @param {String} value Day string (extra time part is ignored)
       * @returns {Date} Parsed date
       */
      function parseDayString(value) {
        const parts = value.slice(0, 10).split('-');
        return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
      }

      return {
        restrict: 'E',
        replace: true,
        scope: {
          widgetId: '@eventCalendarId'
        },
        template:
          '<div class="awe-ec">' +
          '  <div class="awe-ec-mount"></div>' +
          '  <div class="awe-ec-composer">' +
          '    <div class="awe-ec-composer-head">' +
          '      <i class="fa" ng-class="mode === \'edit\' ? \'fa-pencil\' : \'fa-plus-circle\'"></i>' +
          '      <span>{{(mode === \'edit\' ? \'SCR_EVENT_CALENDAR_EDIT\' : \'SCR_EVENT_CALENDAR_NEW\') | translate}}</span>' +
          '    </div>' +
          '    <form class="awe-ec-form" ng-submit="submitForm()">' +
          '      <input class="awe-ec-input awe-ec-input-title" type="text" ng-model="form.title" placeholder="{{\'SCR_EVENT_CALENDAR_TITLE\' | translate}}" required/>' +
          '      <div class="awe-ec-field">' +
          '        <label>{{\'SCR_EVENT_CALENDAR_DATE\' | translate}}</label>' +
          '        <input class="awe-ec-input" type="date" ng-model="form.date" required/>' +
          '      </div>' +
          '      <div class="awe-ec-field">' +
          '        <label>{{\'SCR_EVENT_CALENDAR_TIME\' | translate}}</label>' +
          '        <input class="awe-ec-input" type="time" ng-model="form.time"/>' +
          '      </div>' +
          '      <div class="awe-ec-colors">' +
          '        <span class="awe-ec-color" ng-repeat="color in colors" ng-style="{\'background\': color}"' +
          '              ng-class="{\'awe-ec-color-on\': form.color === color}" ng-click="form.color = color"></span>' +
          '      </div>' +
          '      <div class="awe-ec-form-actions">' +
          '        <button type="submit" class="awe-ec-btn awe-ec-btn-primary" ng-disabled="!form.title">{{\'BUTTON_SAVE\' | translate}}</button>' +
          '        <button type="button" class="awe-ec-btn awe-ec-btn-danger" ng-if="mode === \'edit\'" ng-click="removeEvent()">{{\'BUTTON_DELETE\' | translate}}</button>' +
          '        <button type="button" class="awe-ec-btn" ng-if="mode === \'edit\'" ng-click="resetForm()">{{\'BUTTON_CANCEL\' | translate}}</button>' +
          '      </div>' +
          '    </form>' +
          '  </div>' +
          '</div>',
        /**
         * Link function
         * @param {Object} scope Directive scope
         * @param {Object} element Directive node
         */
        link: function (scope, element) {
          // Init as framework component (view, context, controller, lifecycle)
          const component = new Component(scope, scope.widgetId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          let calendar = null;
          scope.colors = EVENT_COLORS;
          scope.mode = 'create';
          scope.form = {};

          /**
           * Reset the composer to create mode on a given day
           * @param {Date|String} [day] Day as Date or 'YYYY-MM-DD'; defaults to today.
           *   input[type=date]/[type=time] bind to Date objects in AngularJS, so the
           *   model holds Date instances, not strings.
           */
          scope.resetForm = function (day) {
            let base;
            if (day instanceof Date) {
              base = day;
            } else if (day) {
              base = parseDayString(day);
            } else {
              base = new Date();
            }
            scope.mode = 'create';
            scope.form = {
              id: null,
              title: '',
              date: startOfDay(base),
              time: null,
              color: EVENT_COLORS[0]
            };
          };

          /**
           * Load an existing FullCalendar event into the composer (edit mode)
           * @param {Object} fcEvent FullCalendar event
           */
          function editEvent(fcEvent) {
            const start = fcEvent.start || new Date();
            scope.mode = 'edit';
            scope.form = {
              id: fcEvent.id,
              title: fcEvent.title,
              date: startOfDay(start),
              time: fcEvent.allDay ? null : new Date(start),
              color: fcEvent.backgroundColor || EVENT_COLORS[0]
            };
          }

          /**
           * Build the start Date and all-day flag from the composer inputs
           * (both fields hold Date objects). FullCalendar accepts a Date for start.
           * @returns {Object} {start, allDay}
           */
          function buildStart() {
            const day = scope.form.date;
            if (scope.form.time instanceof Date) {
              return {
                start: new Date(day.getFullYear(), day.getMonth(), day.getDate(),
                  scope.form.time.getHours(), scope.form.time.getMinutes()),
                allDay: false
              };
            }
            return {start: startOfDay(day), allDay: true};
          }

          /**
           * Create or update an event from the composer
           */
          scope.submitForm = function () {
            if (!scope.form.title || !calendar) {
              return;
            }
            const when = buildStart();
            if (scope.mode === 'edit') {
              const existing = calendar.getEventById(scope.form.id);
              if (existing) {
                existing.setProp('title', scope.form.title);
                existing.setProp('backgroundColor', scope.form.color);
                existing.setProp('borderColor', scope.form.color);
                existing.setAllDay(when.allDay);
                existing.setStart(when.start);
              }
            } else {
              calendar.addEvent({
                id: 'ec-' + Math.round(window.performance.now() * 1000),
                title: scope.form.title,
                start: when.start,
                allDay: when.allDay,
                backgroundColor: scope.form.color,
                borderColor: scope.form.color
              });
            }
            scope.resetForm(scope.form.date);
          };

          /**
           * Remove the event currently loaded in the composer
           */
          scope.removeEvent = function () {
            if (!calendar || !scope.form.id) {
              return;
            }
            const existing = calendar.getEventById(scope.form.id);
            if (existing) {
              existing.remove();
            }
            scope.resetForm();
          };

          scope.resetForm();

          // Create the FullCalendar instance once the node is laid out
          $timeout(function () {
            const mount = element[0].querySelector('.awe-ec-mount');
            calendar = new Calendar(mount, {
              plugins: [dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
              locale: resolveLocale(),
              initialView: 'dayGridMonth',
              firstDay: 1,
              height: '100%',
              headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,listWeek'
              },
              selectable: true,
              nowIndicator: true,
              dayMaxEvents: true,
              events: seedEvents(),
              // Clicking a day preloads the composer for that date
              dateClick: function (info) {
                scope.$applyAsync(function () {
                  scope.resetForm(info.dateStr.slice(0, 10));
                });
              },
              // Clicking an event loads it into the composer for edit/delete
              eventClick: function (info) {
                info.jsEvent.preventDefault();
                scope.$applyAsync(function () {
                  editEvent(info.event);
                });
              }
            });
            calendar.render();
          });

          // Follow AWE language changes at runtime (buttons, month and day names)
          scope.$on('languageChanged', function (event, language) {
            if (calendar) {
              calendar.setOption('locale', resolveLocale(language));
            }
          });

          // Release FullCalendar resources with the widget
          scope.$on('$destroy', function () {
            if (calendar) {
              calendar.destroy();
              calendar = null;
            }
          });

          /**
           * Sample events around today so the widget shows data on first load
           * @returns {Array} Seed events
           */
          function seedEvents() {
            const today = new Date();
            const day = toDateValue(today);
            const soon = new Date(today);
            soon.setDate(today.getDate() + 2);
            const later = new Date(today);
            later.setDate(today.getDate() + 5);
            return [
              {id: 'seed-1', title: 'Daily standup', start: day + 'T09:30:00', backgroundColor: EVENT_COLORS[0], borderColor: EVENT_COLORS[0]},
              {id: 'seed-2', title: 'Design review', start: day + 'T16:00:00', backgroundColor: EVENT_COLORS[3], borderColor: EVENT_COLORS[3]},
              {id: 'seed-3', title: 'Release planning', start: toDateValue(soon) + 'T11:00:00', backgroundColor: EVENT_COLORS[1], borderColor: EVENT_COLORS[1]},
              {id: 'seed-4', title: 'Team offsite', start: toDateValue(later), allDay: true, backgroundColor: EVENT_COLORS[2], borderColor: EVENT_COLORS[2]}
            ];
          }
        }
      };
    }
  ]);
