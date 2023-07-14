import {aweApplication} from "./../awe";
import {getIconTemplate} from "./component";

export const textInputTemplate =
`<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="validator input {{::validatorGroup}} focus-target" ng-class="{'input-group': controller.unit}">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="text" class="form-control {{classes}}" ng-disabled="controller.readonly" ng-model="model.selected" ng-click="click($event)"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" placeholder="{{controller.placeholder| translateMultiple}}" ng-model-options="{updateOn: 'change'}"
             ng-focus="focus()" ng-blur="blur()" ng-change="component.modelChange()" autocomplete="off" ng-press-enter="submit($event)"/>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
      <span ng-if="controller.unit" class="input-group-addon unit" translate-multiple="{{controller.unit}}" ng-cloak></span>
    </div>
  </div>
</div>`;

export const textColumnTemplate = `<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition">
    <div class="input input-group-{{::size}} focus-target">
      <input type="text" class="form-control col-xs-12 {{classes}} {{component.model.values[0].style}}" ng-disabled="component.controller.readonly" ng-model="component.model.selected"
             placeholder="{{::component.controller.placeholder| translateMultiple}}" ng-model-options="{updateOn: 'change'}" autocomplete="off"
             ng-focus="focus()" ng-blur="blur()" ng-click="click($event)" ng-change="component.columnModelChange()" ng-press-enter="saveRow($event)"/>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

export const textViewInputTemplate = `<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" ng-cloak>
    <label ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="input {{classes}} {{::validatorGroup}}">
      <span ng-if="controller.unit" class="label label-warning pull-right" translate-multiple="{{controller.unit}}"></span>
      <div ng-click="onClick()">
        ${getIconTemplate("text-icon")}
        <span class="text-value" ng-cloak>{{component.visibleValue}}</span>
      </div>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
    </div>
  </div>
</div>`;

export const textViewColumnTemplate = `<div ng-show="component.controller.visible" class="column-input text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-mouseup="onClick()" ng-cloak title="{{(component.controller.title || component.model.values[0].title || component.visibleValue) | translateMultiple}}">
  <span class="{{classes}} {{component.model.values[0].style}} col-xs-{{(component.controller.unit || component.model.values[0].unit) ? '10' : '12'}}" ng-cloak>
    ${getIconTemplate("text-icon fa-fw")}
    <span class="text-value" ng-cloak>{{component.visibleValue}}</span>
  </span>
  <span ng-if="component.controller.unit || component.model.values[0].unit" class="col-xs-2 text-right" ng-cloak>
    <span class="label label-warning" translate-multiple="{{component.controller.unit || component.model.values[0].unit}}"></span>
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;


export const passwordInputTemplate = `<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="validator input {{::validatorGroup}} focus-target" ng-class="{'input-group': controller.unit, 'has-warning has-feedback': $root.status.isCapsLockOn}">
      ${getIconTemplate("{{::iconClass}}")}
      <input type="password" class="form-control {{classes}}" ng-disabled="controller.readonly" ng-model="model.selected" ng-click="click($event)"
             ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" placeholder="{{controller.placeholder| translateMultiple}}" ng-model-options="{updateOn: 'change'}"
             ng-focus="focus()" ng-blur="blur()" ng-change="component.modelChange()" ng-press-enter="submit($event)" autocomplete="off"/>
      <span ng-show="$root.status.isCapsLockOn" class="fa fa-{{::size}} fa-arrow-circle-up form-control-feedback"></span>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
      <span ng-if="controller.unit" class="input-group-addon unit" translate-multiple="{{controller.unit}}" ng-cloak></span>
    </div>
  </div>
</div>`;

export const passwordColumnTemplate = `<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition">
    <div class="input input-group-{{::size}} focus-target"><!--  ng-class="{'input-group': component.controller.unit, 'has-warning has-feedback': $root.isCapsLockOn}" -->
      <input type="password" class="form-control col-xs-12 {{classes}} {{component.model.values[0].style}}" ng-disabled="component.controller.readonly" ng-model="component.model.selected"
             placeholder="{{::component.controller.placeholder| translateMultiple}}" ng-model-options="{updateOn: 'change'}"
             ng-focus="focus()" ng-blur="blur()" ng-change="component.columnModelChange()" ng-press-enter="saveRow($event)" autocomplete="off"/>
      <span ng-show="$root.isCapsLockOn" class="fa fa-{{::size}} fa-arrow-circle-up form-control-feedback"></span>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

export const textareaInputTemplate = `<div ng-show="controller.visible" class="criterion {{criterionClass}}" ui-dependency="dependencies" ng-attr-criterion-id="{{::controller.id}}" ng-cloak>
  <awe-context-menu ng-cloak></awe-context-menu>
  <div ng-class="::groupClass" ng-cloak>
    <label ng-attr-for="{{::controller.id}}" ng-class="::labelClass" ng-style="::labelStyle" ng-cloak>
      <i ng-if="::controller.help" class="help-target fa fa-fw fa-question-circle"></i>
      {{controller.label| translateMultiple}}
    </label>
    <div class="validator input {{::leftLabelInput}} focus-target">
      ${getIconTemplate("{{::iconClass}}")}
      <textarea class="validator form-control {{classes}}" ng-model="model.selected" ng-disabled="controller.readonly" autocomplete="off"
                ng-attr-id="{{::controller.id}}" ng-attr-name="{{::controller.id}}" placeholder="{{controller.placeholder| translateMultiple}}"
                rows="{{controller.areaRows}}" ng-model-options="{updateOn: 'change'}"
                ng-focus="focus()" ng-blur="blur()" ng-change="component.modelChange()"></textarea>
      <awe-loader class="loader" ng-if="controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
    </div>
  </div>
</div>`;
export const textareaColumnTemplate = `<div ng-show="component.controller.visible" class="validator column-input criterion text-{{::component.controller.align}} no-animate" ui-dependency="dependencies" ng-cloak>
  <span class="visible-value" ng-cloak>{{component.visibleValue}}</span>
  <span class="edition">
    <div class="input focus-target">
      <textarea class="form-control col-xs-12 {{classes}} {{component.model.values[0].style}}" ng-model="component.model.selected" ng-disabled="component.controller.readonly" autocomplete="off"
                placeholder="{{::component.controller.placeholder| translateMultiple}}" ng-model-options="{updateOn: 'change'}"
                ng-focus="focus()" ng-blur="blur()" ng-click="click($event)" ng-change="component.columnModelChange()"></textarea>
    </div>
    ${getIconTemplate("{{::iconClass}}")}
  </span>
  <awe-loader class="loader no-animate" ng-if="component.controller.loading" icon-loader="{{::iconLoader}}" ng-cloak></awe-loader>
</div>`;

// Text service
aweApplication.factory('Text',
  ['Criterion', 'AweUtilities', 'Control',
    /**
     * Criterion generic methods
     * @param {Service} Criterion
     * @param {Service} Utilities Utilities service
     * @param {Control} Control Control service
     */
    function (Criterion, Utilities, Control) {
      /**
       * Text constructor
       * @param {Scope} scope Numeric scope
       * @param {String} id Numeric id
       * @param {String} element Numeric element
       */
      function Text(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Criterion(this.scope, this.id, this.element);
        let  text = this;
        this.component.asText = function () {
          return text.init();
        };
        return this.component;
      }
      Text.prototype = {
        /**
         * Initialize text
         */
        init: function () {
          // Initialize criterion
          let  component = this.component;
          if (!component.asCriterion()) {
            // If criterion is wrong, cancel initialization
            return false;
          }

          // Update icon class
          if (component.controller.icon) {
            component.scope.iconClass = " fa fa-" + component.controller.icon;
          } else if (component.controller.iconMdi) {
            component.scope.iconClass = " material-icons";
          }

          /**********************************************************************
           * PRIVATE METHODS
           **********************************************************************/

          /**
           * Retrieves visible value for the selector
           * @returns {string} visible value
           */
          function fixModel(changed) {
            let  selected = component.model.selected;
            let  values = component.model.values;

            // Changed selected
            if ("values" in changed) {
              component.model.selected = values[0].value;
            } else if ("selected" in changed) {
              values[0] = {...(values[0] || {}), value: selected, label: selected || ""};
            }
          }

          /**********************************************************************
           * SCOPE METHODS
           **********************************************************************/

          /**
           * Launch click event
           */
          component.scope.onClick = function () {
            Utilities.timeout(function () {
              component.storeEvent('click');
            });
          };

          /**********************************************************************
           * COMPONENT METHODS
           **********************************************************************/

          /**
           * Retrieves visible value for the selector
           * @returns {string} visible value
           */
          component.getVisibleValue = function () {
            let  visibleValue = "";
            if (component.model.values.length > 0) {
              visibleValue = component.model.values[0].label;
            } else {
              visibleValue = component.model.selected;
            }
            return visibleValue;
          };

          /**
           * Update the model on model changed
           */
          component.onModelChanged = function (changed) {
            // Fill data
            fixModel(changed || {});

            // Fill data
            component.updateVisibleValue();
          };

          // Fix model on load
          if (component.model.values.length > 0) {
            component.onModelChanged({values: true});
          } else if (!Utilities.isEmpty(component.model.selected)) {
            component.onModelChanged({selected: true});
          }

          /**********************************************************************
           * EVENTS
           **********************************************************************/

          // Action listener definition
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, service: component, method: "onModelChanged"});

          // Initialization ok
          return true;
        }
      };
      return Text;
    }
  ]);
