import {aweApplication} from "../awe";
import "./plugins/uiModal";
import {getIconTemplate} from "../services/component";
import {TestIds} from "../data/testIds";
import {initDialogComponent} from "./dialog";
import {AVATAR_TONES, getInitials, STATUS_TONES, toTone} from "../services/grid/initials";
import {getSwatchColor} from "../services/grid/swatch";

const DEFAULT_MAX_WIDTH = "40rem";

// Icon of a tag of the identity header: decorative, the text of the tag is its value (as the icon of a status cell)
const tagIconTemplate = getIconTemplate("awe-cell-icon", "span", "tag.icon", "aria-hidden=\"true\"").replace(/>\s+</g, "><");

const template = `<div ng-attr-id="{{::controller.id}}" ng-cloak>
  <div class="modal fade side-panel side-panel-{{::position}}" ui-modal trap-tab on-close="component.closeDialog()" data-testid="${TestIds.sidePanel}" ng-attr-data-testid-owner="{{::controller.id}}" role="dialog" aria-modal="true" ng-attr-aria-labelledby="{{::titleId}}" ng-cloak>
    <div class="modal-dialog {{::controller.style}}" ng-style="::{'max-width': maxWidth}">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" data-testid="${TestIds.sidePanelClose}">&times;</button>
          <h4 class="modal-title" ng-if="!identity" ng-attr-id="{{::titleId}}" data-testid="${TestIds.sidePanelTitle}">
            ${getIconTemplate("panel-title-icon")}
            <span translate-multiple="{{::controller.label}}"></span>
          </h4>
          <div class="panel-identity" ng-if="identity">
            <span class="panel-identity-avatar awe-cell-swatch" aria-hidden="true" ng-if="identity.swatch" ng-attr-style="--awe-swatch-color: {{identity.swatch}}"></span>
            <span class="panel-identity-avatar awe-cell-avatar awe-cell-avatar-{{identity.tone}}" data-tone="{{identity.tone}}" aria-hidden="true" ng-if="!identity.swatch && identity.initials" ng-bind="identity.initials"></span>
            <div class="panel-identity-text">
              <h4 class="panel-identity-title" ng-attr-id="{{::titleId}}" data-testid="${TestIds.sidePanelTitle}" ng-bind="identity.title"></h4>
              <p class="panel-identity-subtitle" ng-if="identity.subtitle" data-testid="${TestIds.sidePanelSubtitle}" ng-bind="identity.subtitle"></p>
              <div class="panel-identity-tags" ng-if="identity.tags.length">
                <span ng-repeat="tag in identity.tags track by $index" class="awe-cell-status awe-cell-status-{{tag.tone}}" ng-class="{'awe-cell-status-plain': tag.plain}" data-tone="{{tag.tone}}" data-testid="${TestIds.sidePanelTag}">${tagIconTemplate}<span ng-bind="tag.text"></span></span>
              </div>
            </div>
          </div>
        </div>
        <div class="side-panel-body" ng-transclude></div>
      </div>
    </div>
  </div>
</div>`;

/**
 * Text of a cell of a grid row: its label, or its value (empty when the row has not the field)
 * @param {*} cell Cell
 * @returns {string} Text
 */
const cellText = cell => {
  const text = cell !== null && typeof cell === "object" ? cell.label ?? cell.value : cell;
  return text == null ? "" : String(text);
};

/**
 * Value of a cell of a grid row
 * @param {*} cell Cell
 * @returns {*} Value
 */
const cellValue = cell => cell !== null && typeof cell === "object" ? cell.value : cell;

/**
 * Identity of the header of a side panel (#793): the avatar (or the colour swatch), title, subtitle and tags of the
 * selected row of its header grid, or null when the panel has no header grid or not exactly one row of it is selected
 * @param {Object} controller Side panel controller
 * @param {Object} grid Model of the header grid (values and selected row identifiers)
 * @returns {Object|null} Identity
 */
export function getPanelIdentity(controller, grid) {
  if (!controller || !controller.headerGrid || !grid) {
    return null;
  }
  const selected = [].concat(grid.selected ?? []).filter(id => id != null);
  const row = selected.length === 1 ? (grid.values || []).find(item => String(cellValue(item.id)) === String(selected[0])) : null;
  if (!row) {
    return null;
  }
  const text = field => field ? cellText(row[field]) : "";
  const tags = (controller.headerTags || [])
    .map(tag => ({
      text: text(tag.field),
      tone: toTone(cellValue(row[tag.toneField]), STATUS_TONES),
      plain: !!tag.iconField,
      icon: text(tag.iconField)
    }))
    .filter(tag => tag.text.trim() !== "");
  return {
    title: text(controller.headerTitleField),
    subtitle: text(controller.headerSubtitleField),
    initials: getInitials(text(controller.headerAvatarField)),
    tone: toTone(cellValue(row[controller.headerAvatarToneField]), AVATAR_TONES),
    swatch: controller.headerSwatchField ? getSwatchColor(cellValue(row[controller.headerSwatchField])) : "",
    tags
  };
}

// Side panel directive: a dialog anchored to the left or right edge of the screen (#843)
aweApplication.directive('aweSidePanel',
  ['Component', 'ActionController', 'AweUtilities', 'Control',
    function (Component, $actionController, Utilities, Control) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          sidePanelId: '@'
        },
        link: function (scope) {
          const component = initDialogComponent(scope, scope.sidePanelId, {Component, $actionController, Utilities});
          if (!component) {
            return false;
          }

          const controller = component.controller || {};
          scope.position = controller.position === "left" ? "left" : "right";
          scope.maxWidth = controller.maxWidth || DEFAULT_MAX_WIDTH;

          // Identity header (#793): the header follows the selected row of the header grid; its title names the panel
          if (controller.headerGrid) {
            scope.titleId = controller.id + "-title";
            scope.$watch(() => getPanelIdentity(component.controller,
              Control.getAddressModel({view: component.address.view, component: component.controller.headerGrid})),
            identity => {
              scope.identity = identity && {
                ...identity,
                tags: identity.tags.map(tag => ({...tag, icon: Utilities.extractIcon(tag.icon)}))
              };
            }, true);
          }
        }
      };
    }
  ]);
