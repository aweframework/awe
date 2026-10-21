import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {createFocusTrap} from "../services/focusTrap";

export const GRID_TOOLBAR_TEMPLATE = `<div class="grid-toolbar" data-testid="${TestIds.gridToolbar}" ng-attr-data-testid-owner="{{component.address.component}}" ng-attr-data-maximized="{{iconMaximized === true}}">
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-columns-button" data-testid="${TestIds.gridColumnsButton}" title="{{'GRID_COLUMNS_TITLE' | translateMultiple}}" ng-click="openColumns()">
    <i class="fa fa-columns"></i>
    <span class="button-text" translate-multiple="GRID_COLUMNS_BUTTON"></span>
  </button>
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-maximize-button" data-testid="${TestIds.gridMaximizeButton}" ng-attr-aria-pressed="{{maximized === true}}" title="{{(iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE') | translateMultiple}}" ng-click="toggleMaximize()">
    <i class="fa" ng-class="iconMaximized ? 'fa-compress' : 'fa-expand'"></i>
    <span class="button-text" translate-multiple="{{iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE'}}"></span>
  </button>
  <div ng-if="columnsOpen && !sidePanel" class="grid-columns-backdrop" ng-click="closeColumns()"></div>
  <div ng-if="columnsOpen && !sidePanel" class="grid-columns-dialog" role="dialog" data-testid="${TestIds.gridColumnsDialog}" ng-attr-data-testid-owner="{{component.address.component}}" data-type="dialog">
    <div class="grid-columns-title" translate-multiple="GRID_COLUMNS_TITLE"></div>
    <div ng-include="'grid-columns-content'"></div>
    <div class="grid-columns-actions" ng-include="'grid-columns-actions'"></div>
  </div>
</div>`;

// Counter and list of the Columns, shared by the dialog and the side panel
const COLUMNS_CONTENT_TEMPLATE = `<div class="grid-columns-counter" data-testid="${TestIds.gridColumnsCounter}" ng-attr-data-visible="{{component.layoutVisible}}" ng-attr-data-total="{{component.layoutTotal}}">
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
</ul>`;

// Actions of the Columns, shared by the dialog and the side panel
const COLUMNS_ACTIONS_TEMPLATE = `<button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsShowAll}" ng-click="component.showAllColumns()" translate-multiple="GRID_COLUMNS_SHOW_ALL"></button>
<button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsReset}" ng-click="component.resetColumnLayout()" translate-multiple="GRID_COLUMNS_RESET"></button>
<button type="button" class="btn-awe btn-primary" data-testid="${TestIds.gridColumnsSave}" ng-click="component.saveColumnLayout()" translate-multiple="GRID_COLUMNS_SAVE"></button>
<button type="button" class="btn-awe btn-default" data-testid="${TestIds.gridColumnsClose}" ng-click="closeColumns()" translate-multiple="GRID_COLUMNS_CLOSE"></button>`;

// The Columns as a side panel (columns-panel="side-panel", #843): the same Bootstrap modal as the side-panel element, so
// it slides in and out, has its backdrop and closes with Esc or a click outside. The test hook is only set while it is
// open (the modal stays in the page, hidden, once it has been opened)
const COLUMNS_SIDE_PANEL_TEMPLATE = `<div class="modal fade side-panel side-panel-right grid-columns-side-panel" role="dialog" aria-modal="true" tabindex="-1" ng-attr-data-testid="{{columnsOpen ? '${TestIds.gridColumnsDialog}' : undefined}}" ng-attr-data-testid-owner="{{columnsOpen ? component.address.component : undefined}}" data-type="side-panel">
  <div class="modal-dialog" style="max-width: 28rem">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" title="{{'GRID_COLUMNS_CLOSE' | translateMultiple}}">&times;</button>
        <h4 class="modal-title" translate-multiple="GRID_COLUMNS_TITLE"></h4>
      </div>
      <div class="side-panel-body"><div class="panel-body" ng-include="'grid-columns-content'"></div></div>
      <div class="modal-footer grid-columns-actions" ng-include="'grid-columns-actions'"></div>
    </div>
  </div>
</div>`;

const COLUMN_ITEM_TEMPLATE = `<span class="grid-column-handle fa fa-bars" draggable="true" data-testid="${TestIds.gridColumnHandle}" title="{{'GRID_COLUMNS_DRAG' | translateMultiple}}"></span>
<input type="checkbox" class="grid-column-checkbox" data-testid="${TestIds.gridColumnCheckbox}" ng-checked="column.visible" ng-click="component.setColumnVisible(column.name, !column.visible)"/>
<span class="grid-column-label" translate-multiple="{{column.label}}"></span>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnUp}" title="{{'GRID_COLUMNS_MOVE_UP' | translateMultiple}}" ng-disabled="column.first" ng-click="component.moveColumn(column.name, -1)"><i class="fa fa-arrow-up"></i></button>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnDown}" title="{{'GRID_COLUMNS_MOVE_DOWN' | translateMultiple}}" ng-disabled="column.last" ng-click="component.moveColumn(column.name, 1)"><i class="fa fa-arrow-down"></i></button>`;

// Grid toolbar directive: Columns dialog (or side panel) and Maximize
aweApplication.directive('aweGridToolbar',
  ['$templateCache', '$compile', 'AweSettings',
    function ($templateCache, $compile, $settings) {
      $templateCache.put('grid-column-item', COLUMN_ITEM_TEMPLATE);
      $templateCache.put('grid-columns-content', COLUMNS_CONTENT_TEMPLATE);
      $templateCache.put('grid-columns-actions', COLUMNS_ACTIONS_TEMPLATE);

      return {
        restrict: 'E',
        template: GRID_TOOLBAR_TEMPLATE,
        link: function (scope, element) {
          const component = scope.component;
          let dragged = null;
          let sidePanel = null;

          scope.columnsOpen = false;
          scope.sidePanel = false;

          // Run a scope change from a Bootstrap event, which may come inside or outside a digest
          const applyScope = fn => (scope.$root.$$phase ? fn() : scope.$apply(fn));

          // Drag and drop of the handle onto a sibling
          const bindDragAndDrop = host => {
            host.on("dragstart", ".grid-column-handle", function (event) {
              dragged = $(this).closest(".grid-column-item").attr("column-id");
              if (event.originalEvent && event.originalEvent.dataTransfer) {
                event.originalEvent.dataTransfer.effectAllowed = "move";
                event.originalEvent.dataTransfer.setData("text/plain", dragged);
              }
            });
            host.on("dragover", ".grid-column-item", function (event) {
              event.preventDefault();
            });
            host.on("drop", ".grid-column-item", function (event) {
              event.preventDefault();
              const target = $(this).attr("column-id");
              const name = dragged || (event.originalEvent && event.originalEvent.dataTransfer ? event.originalEvent.dataTransfer.getData("text/plain") : null);
              dragged = null;
              if (name) {
                scope.$apply(() => component.moveColumnTo(name, target));
              }
            });
          };

          // The side panel keeps the focus inside while it is shown and gives it back to the Columns button
          const trap = createFocusTrap(() => sidePanel && sidePanel[0]);

          // The side panel is created the first time it is opened, in the page body (no container of the grid clips it)
          const getSidePanel = () => {
            if (!sidePanel) {
              sidePanel = $compile(COLUMNS_SIDE_PANEL_TEMPLATE)(scope);
              $(document.body).append(sidePanel);
              bindDragAndDrop(sidePanel);
              sidePanel.on("show.bs.modal", () => trap.activate());
              sidePanel.on("shown.bs.modal", () => trap.focusFirst());
              sidePanel.on("hidden.bs.modal", () => {
                trap.deactivate();
                applyScope(() => {
                  scope.columnsOpen = false;
                });
              });
            }
            return sidePanel;
          };

          scope.openColumns = function () {
            component.refreshLayoutNodes();
            // The grid columns-panel, or the application default (gridColumnsPanel setting) when the grid does not set it
            const panel = (component.controller || {}).columnsPanel || $settings.get("gridColumnsPanel");
            scope.sidePanel = panel === "side-panel";
            scope.columnsOpen = true;
            if (scope.sidePanel) {
              getSidePanel().modal("show");
            }
          };

          scope.closeColumns = function () {
            if (scope.sidePanel && sidePanel) {
              // Bootstrap slides it out and then raises hidden.bs.modal, which marks the Columns as closed
              sidePanel.modal("hide");
            } else {
              scope.columnsOpen = false;
            }
          };

          // The grid grows over the maximize target like a window (Maximize service, started by the grid)
          scope.toggleMaximize = function () {
            scope.togglePanel();
          };

          bindDragAndDrop($(element));

          scope.$on("$destroy", () => {
            $(element).off();
            trap.deactivate();
            if (sidePanel) {
              const modal = sidePanel.data("bs.modal");
              if (modal && modal.isShown) {
                modal.removeBackdrop();
                $(document.body).removeClass("modal-open");
              }
              sidePanel.off().remove();
              sidePanel = null;
            }
          });
        }
      };
    }
  ]);
