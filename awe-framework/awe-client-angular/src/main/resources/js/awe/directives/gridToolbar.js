import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";

export const GRID_TOOLBAR_TEMPLATE = `<div class="grid-toolbar" data-testid="${TestIds.gridToolbar}" ng-attr-data-testid-owner="{{component.address.component}}" ng-attr-data-maximized="{{iconMaximized === true}}">
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-columns-button" data-testid="${TestIds.gridColumnsButton}" title="{{'GRID_COLUMNS_TITLE' | translateMultiple}}" ng-click="openColumns()">
    <i class="fa fa-columns"></i>
    <span class="button-text" translate-multiple="GRID_COLUMNS_BUTTON"></span>
  </button>
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-maximize-button" data-testid="${TestIds.gridMaximizeButton}" ng-attr-aria-pressed="{{maximized === true}}" title="{{(iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE') | translateMultiple}}" ng-click="toggleMaximize()">
    <i class="fa" ng-class="iconMaximized ? 'fa-compress' : 'fa-expand'"></i>
    <span class="button-text" translate-multiple="{{iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE'}}"></span>
  </button>
  <div ng-if="columnsOpen" class="grid-columns-backdrop" ng-click="closeColumns()"></div>
  <div ng-if="columnsOpen" class="grid-columns-dialog" role="dialog" data-testid="${TestIds.gridColumnsDialog}" ng-attr-data-testid-owner="{{component.address.component}}">
    <div class="grid-columns-title" translate-multiple="GRID_COLUMNS_TITLE"></div>
    <div class="grid-columns-counter" data-testid="${TestIds.gridColumnsCounter}" ng-attr-data-visible="{{component.layoutVisible}}" ng-attr-data-total="{{component.layoutTotal}}">
      {{component.layoutVisible}} <span translate-multiple="SCREEN_TEXT_GRID_OF"></span> {{component.layoutTotal}} <span translate-multiple="GRID_COLUMNS_VISIBLE"></span>
    </div>
    <ul class="grid-columns-list">
      <li ng-repeat="node in component.layoutNodes" ng-class="{'grid-column-group': node.group !== undefined}" ng-attr-data-testid="{{node.group !== undefined ? '${TestIds.gridColumnGroup}' : undefined}}">
        <div ng-if="node.group !== undefined" class="grid-column-group-title" translate-multiple="{{node.label}}"></div>
        <ul ng-if="node.group !== undefined">
          <li ng-repeat="column in node.columns" class="grid-column-item" ng-include="'grid-column-item'" data-testid="${TestIds.gridColumnItem}" ng-attr-column-id="{{column.name}}"></li>
        </ul>
        <div ng-if="node.group === undefined" class="grid-column-item" ng-init="column = node.column" ng-include="'grid-column-item'" data-testid="${TestIds.gridColumnItem}" ng-attr-column-id="{{column.name}}"></div>
      </li>
    </ul>
    <div class="grid-columns-actions">
      <button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsShowAll}" ng-click="component.showAllColumns()" translate-multiple="GRID_COLUMNS_SHOW_ALL"></button>
      <button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsReset}" ng-click="component.resetColumnLayout()" translate-multiple="GRID_COLUMNS_RESET"></button>
      <button type="button" class="btn-awe btn-primary" data-testid="${TestIds.gridColumnsSave}" ng-click="component.saveColumnLayout()" translate-multiple="GRID_COLUMNS_SAVE"></button>
      <button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsClose}" ng-click="closeColumns()" translate-multiple="GRID_COLUMNS_CLOSE"></button>
    </div>
  </div>
</div>`;

const COLUMN_ITEM_TEMPLATE = `<span class="grid-column-handle fa fa-bars" draggable="true" data-testid="${TestIds.gridColumnHandle}" title="{{'GRID_COLUMNS_DRAG' | translateMultiple}}"></span>
<input type="checkbox" class="grid-column-checkbox" data-testid="${TestIds.gridColumnCheckbox}" ng-checked="column.visible" ng-click="component.setColumnVisible(column.name, !column.visible)"/>
<span class="grid-column-label" translate-multiple="{{column.label}}"></span>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnUp}" title="{{'GRID_COLUMNS_MOVE_UP' | translateMultiple}}" ng-disabled="column.first" ng-click="component.moveColumn(column.name, -1)"><i class="fa fa-arrow-up"></i></button>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnDown}" title="{{'GRID_COLUMNS_MOVE_DOWN' | translateMultiple}}" ng-disabled="column.last" ng-click="component.moveColumn(column.name, 1)"><i class="fa fa-arrow-down"></i></button>`;

// Grid toolbar directive: Columns dialog and Maximize
aweApplication.directive('aweGridToolbar',
  ['$templateCache',
    function ($templateCache) {
      $templateCache.put('grid-column-item', COLUMN_ITEM_TEMPLATE);

      return {
        restrict: 'E',
        template: GRID_TOOLBAR_TEMPLATE,
        link: function (scope, element) {
          const component = scope.component;
          let dragged = null;

          scope.columnsOpen = false;

          scope.openColumns = function () {
            component.refreshLayoutNodes();
            scope.columnsOpen = true;
          };

          scope.closeColumns = function () {
            scope.columnsOpen = false;
          };

          // The grid grows over the maximize target like a window (Maximize service, started by the grid)
          scope.toggleMaximize = function () {
            scope.togglePanel();
          };

          // Drag and drop of the handle onto a sibling
          $(element).on("dragstart", ".grid-column-handle", function (event) {
            dragged = $(this).closest(".grid-column-item").attr("column-id");
            if (event.originalEvent && event.originalEvent.dataTransfer) {
              event.originalEvent.dataTransfer.effectAllowed = "move";
              event.originalEvent.dataTransfer.setData("text/plain", dragged);
            }
          });
          $(element).on("dragover", ".grid-column-item", function (event) {
            event.preventDefault();
          });
          $(element).on("drop", ".grid-column-item", function (event) {
            event.preventDefault();
            const target = $(this).attr("column-id");
            const name = dragged || (event.originalEvent && event.originalEvent.dataTransfer ? event.originalEvent.dataTransfer.getData("text/plain") : null);
            dragged = null;
            if (name) {
              scope.$apply(() => component.moveColumnTo(name, target));
            }
          });

          scope.$on("$destroy", () => $(element).off());
        }
      };
    }
  ]);
