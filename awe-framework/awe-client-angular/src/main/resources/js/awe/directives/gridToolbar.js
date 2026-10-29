import {aweApplication} from "../awe";
import {TestIds} from "../data/testIds";
import {createFocusTrap} from "../services/focusTrap";
import {iconMarkup, iconTemplate} from "../services/icon";

// Space between the Columns button and the popover, and between the popover and the edges of the window
const GAP = 8;
const MARGIN = 8;
// Time to wait for the click which follows a mouse down on the Columns button (it would open the popover again)
const CLICK_TIMEOUT = 1000;

// The Columns as a popover or a dialog in the toolbar. The class is static: one set by ng-class is animated, so it would
// only be there on the next animation frame, after the popover is placed
const columnsRootTemplate = (panel, attributes = "") => `<div ng-if="columnsOpen && columnsPanel === '${panel}'" class="grid-columns-${panel}" role="dialog" aria-label="{{'GRID_COLUMNS_TITLE' | translateMultiple}}"${attributes} data-testid="${TestIds.gridColumnsDialog}" ng-attr-data-testid-owner="{{component.address.component}}" data-type="${panel}" ng-attr-aria-busy="{{component.layoutBusy ? 'true' : undefined}}">
    <div ng-include="'grid-columns-header'"></div>
    <div class="grid-columns-body" ng-include="'grid-columns-content'"></div>
    <div class="grid-columns-actions" ng-include="'grid-columns-actions'"></div>
  </div>`;

// The Columns (popover, dialog or side panel) are aria-busy while Save or Restore default wait for the server
export const GRID_TOOLBAR_TEMPLATE = `<div class="grid-toolbar" data-testid="${TestIds.gridToolbar}" ng-attr-data-testid-owner="{{component.address.component}}" ng-attr-data-maximized="{{iconMaximized === true}}">
  <awe-grid-group-by ng-if="::component.groupingConfigured" class="grid-group-by"></awe-grid-group-by>
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-columns-button" data-testid="${TestIds.gridColumnsButton}" title="{{'GRID_COLUMNS_TITLE' | translateMultiple}}" aria-haspopup="dialog" ng-attr-aria-expanded="{{columnsOpen === true}}" ng-click="openColumns()">
    ${iconTemplate("columns")}
    <span class="button-text" translate-multiple="GRID_COLUMNS_BUTTON"></span>
  </button>
  <button type="button" class="btn-awe btn-default grid-toolbar-button grid-maximize-button" data-testid="${TestIds.gridMaximizeButton}" ng-attr-aria-pressed="{{maximized === true}}" title="{{(iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE') | translateMultiple}}" ng-click="toggleMaximize()">
    <i>${iconMarkup("compress", 'ng-if="iconMaximized"')}${iconMarkup("expand", 'ng-if="!iconMaximized"')}</i>
    <span class="button-text" translate-multiple="{{iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE'}}"></span>
  </button>
  <div ng-if="columnsOpen && columnsPanel === 'dialog'" class="grid-columns-backdrop" ng-click="closeColumns()"></div>
  ${columnsRootTemplate("popover")}
  ${columnsRootTemplate("dialog", ' aria-modal="true"')}
</div>`;

// Header of the Columns, shared by the popover, the dialog and the side panel: the title, the "N of M visible" counter
// as its subtitle and the close button
const COLUMNS_HEADER_TEMPLATE = `<div class="grid-columns-header">
  <div class="grid-columns-titles">
    <h4 class="grid-columns-title" translate-multiple="GRID_COLUMNS_TITLE"></h4>
    <p class="grid-columns-counter" data-testid="${TestIds.gridColumnsCounter}" ng-attr-data-visible="{{component.layoutVisible}}" ng-attr-data-total="{{component.layoutTotal}}">
      {{component.layoutVisible}} <span translate-multiple="SCREEN_TEXT_GRID_OF"></span> {{component.layoutTotal}} <span translate-multiple="GRID_COLUMNS_VISIBLE"></span>
    </p>
  </div>
  <button type="button" class="btn-awe grid-columns-close" data-testid="${TestIds.gridColumnsClose}" aria-label="{{'GRID_COLUMNS_CLOSE' | translateMultiple}}" title="{{'GRID_COLUMNS_CLOSE' | translateMultiple}}" ng-click="closeColumns()">${iconMarkup("times")}</button>
</div>`;

// List of the Columns, shared by the popover, the dialog and the side panel
const COLUMNS_CONTENT_TEMPLATE = `<ul class="grid-columns-list">
  <li ng-repeat="node in component.layoutNodes" ng-class="{'grid-column-group': node.group !== undefined}" ng-attr-data-testid="{{node.group !== undefined ? '${TestIds.gridColumnGroup}' : undefined}}">
    <div ng-if="node.group !== undefined" class="grid-column-group-title" translate-multiple="{{node.label}}"></div>
    <ul ng-if="node.group !== undefined">
      <li ng-repeat="column in node.columns" class="grid-column-item" ng-include="'grid-column-item'" data-testid="${TestIds.gridColumnItem}" ng-attr-column-id="{{column.name}}"></li>
    </ul>
    <div ng-if="node.group === undefined" class="grid-column-item" ng-init="column = node.column" ng-include="'grid-column-item'" data-testid="${TestIds.gridColumnItem}" ng-attr-column-id="{{column.name}}"></div>
  </li>
</ul>`;

// Actions of the Columns, shared by the popover, the dialog and the side panel: Restore default at the start, Cancel and
// Save at the end. While a Save waits for the server its button shows the spinner and "Saving…" and is disabled
const COLUMNS_ACTIONS_TEMPLATE = `<button type="button" class="btn-awe btn-text grid-columns-reset" data-testid="${TestIds.gridColumnsReset}" ng-click="resetColumns()">${iconMarkup("rotate-left")}<span translate-multiple="GRID_COLUMNS_RESET"></span></button>
<button type="button" class="btn-awe btn-default grid-columns-cancel" data-testid="${TestIds.gridColumnsCancel}" ng-click="cancelColumns()" translate-multiple="GRID_COLUMNS_CANCEL"></button>
<button type="button" class="btn-awe btn-primary grid-columns-save" data-testid="${TestIds.gridColumnsSave}" ng-class="{'is-loading': saving}" ng-disabled="saving" ng-click="saveColumns()">${iconMarkup("spinner fa-spin", 'ng-if="saving"')}<span translate-multiple="{{saving ? 'GRID_COLUMNS_SAVING' : 'GRID_COLUMNS_SAVE'}}"></span></button>`;

// The Columns as a side panel (columns-panel="side-panel", #843): the same Bootstrap modal as the side-panel element, so
// it slides in and out, has its backdrop and closes with Esc or a click outside. The test hook is only set while it is
// open (the modal stays in the page, hidden, once it has been opened)
const COLUMNS_SIDE_PANEL_TEMPLATE = `<div class="modal fade side-panel side-panel-right grid-columns-side-panel" role="dialog" aria-modal="true" aria-label="{{'GRID_COLUMNS_TITLE' | translateMultiple}}" tabindex="-1" ng-attr-data-testid="{{columnsOpen ? '${TestIds.gridColumnsDialog}' : undefined}}" ng-attr-data-testid-owner="{{columnsOpen ? component.address.component : undefined}}" data-type="side-panel" ng-attr-aria-busy="{{component.layoutBusy ? 'true' : undefined}}">
  <div class="modal-dialog" style="max-width: var(--awe-grid-columns-panel-width)">
    <div class="modal-content">
      <div class="modal-header" ng-include="'grid-columns-header'"></div>
      <div class="side-panel-body"><div class="panel-body" ng-include="'grid-columns-content'"></div></div>
      <div class="modal-footer grid-columns-actions" ng-include="'grid-columns-actions'"></div>
    </div>
  </div>
</div>`;

// A column of the list: drag handle, checkbox, label (muted while hidden) and the arrows, shown on the hovered or
// focused row
const COLUMN_ITEM_TEMPLATE = `${iconTemplate("bars", {tag: "span", classes: "grid-column-handle", attributes: `draggable="true" data-testid="${TestIds.gridColumnHandle}" title="{{'GRID_COLUMNS_DRAG' | translateMultiple}}"`})}
<input type="checkbox" class="grid-column-checkbox" data-testid="${TestIds.gridColumnCheckbox}" aria-label="{{column.label | translateMultiple}}" ng-checked="column.visible" ng-click="component.setColumnVisible(column.name, !column.visible)"/>
<span class="grid-column-label" ng-class="{'grid-column-hidden': !column.visible}" translate-multiple="{{column.label}}"></span>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnUp}" title="{{'GRID_COLUMNS_MOVE_UP' | translateMultiple}}" aria-label="{{'GRID_COLUMNS_MOVE_UP' | translateMultiple}}" ng-disabled="column.first" ng-click="component.moveColumn(column.name, -1)">${iconTemplate("arrow-up")}</button>
<button type="button" class="btn-link grid-column-move" data-testid="${TestIds.gridColumnDown}" title="{{'GRID_COLUMNS_MOVE_DOWN' | translateMultiple}}" aria-label="{{'GRID_COLUMNS_MOVE_DOWN' | translateMultiple}}" ng-disabled="column.last" ng-click="component.moveColumn(column.name, 1)">${iconTemplate("arrow-down")}</button>`;

/**
 * Offset of the box fixed elements are placed in: the window, or an ancestor which creates a containing block for them
 * (a transformed Bootstrap modal dialog)
 * @param {HTMLElement} node Fixed element
 * @returns {{top: number, left: number}} Offset
 */
export const getFixedOffset = node => {
  for (let parent = node.parentElement; parent && parent !== document.body; parent = parent.parentElement) {
    const style = window.getComputedStyle(parent);
    const creates = value => value && value !== "none";
    if (creates(style.transform) || creates(style.filter) || creates(style.perspective) ||
      /paint|layout|strict|content/.test(style.contain || "")) {
      const rect = parent.getBoundingClientRect();
      return {top: rect.top, left: rect.left};
    }
  }
  return {top: 0, left: 0};
};

// Grid toolbar directive: Columns panel (a popover anchored to the Columns button by default, a dialog or a side panel)
// and Maximize
aweApplication.directive('aweGridToolbar',
  ['$templateCache', '$compile', 'AweSettings',
    function ($templateCache, $compile, $settings) {
      $templateCache.put('grid-column-item', COLUMN_ITEM_TEMPLATE);
      $templateCache.put('grid-columns-header', COLUMNS_HEADER_TEMPLATE);
      $templateCache.put('grid-columns-content', COLUMNS_CONTENT_TEMPLATE);
      $templateCache.put('grid-columns-actions', COLUMNS_ACTIONS_TEMPLATE);

      return {
        restrict: 'E',
        template: GRID_TOOLBAR_TEMPLATE,
        link: function (scope, element) {
          const component = scope.component;
          let dragged = null;
          let sidePanel = null;
          // Layout the grid had when the Columns were opened (or at the last Restore default): Cancel goes back
          let snapshot = null;
          let listening = false;

          scope.columnsOpen = false;
          scope.columnsPanel = null;
          scope.sidePanel = false;

          // Run a scope change from a Bootstrap or DOM event, which may come inside or outside a digest
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

          const takeSnapshot = () => {
            const state = component.columnLayoutState;
            snapshot = state ? {order: state.order.slice(), hidden: state.hidden.slice()} : null;
          };

          const getButton = () => element.find(".grid-columns-button")[0];
          const getPopover = () => element.find(".grid-columns-popover")[0] || null;

          // The side panel and the popover keep the focus inside while they are shown and give it back to the Columns
          // button
          const trap = createFocusTrap(() => sidePanel && sidePanel[0]);
          const popoverTrap = createFocusTrap(getPopover);

          // Place the popover below the Columns button, aligned to its right edge, inside the window: above the button
          // when it does not fit below and there is more room above. Its maximum height is the room on that side, so a
          // long list of columns scrolls inside it
          const place = () => {
            const popover = getPopover();
            const button = getButton();
            if (!popover || !button) {
              return;
            }
            const rect = button.getBoundingClientRect();
            const size = popover.getBoundingClientRect();
            const clamp = (value, max) => Math.max(MARGIN, Math.min(value, max));
            const below = rect.bottom + GAP;
            const roomBelow = window.innerHeight - MARGIN - below;
            const roomAbove = rect.top - GAP - MARGIN;
            const above = size.height > roomBelow && roomAbove > roomBelow;
            const room = Math.max(0, above ? roomAbove : roomBelow);
            popover.style.maxHeight = `${Math.round(room)}px`;
            const height = Math.min(size.height, room);
            let top = above ? rect.top - GAP - height : below;
            top = clamp(top, window.innerHeight - height - MARGIN);
            const left = clamp(rect.right - size.width, window.innerWidth - size.width - MARGIN);
            const offset = getFixedOffset(popover);
            popover.style.top = `${Math.round(top - offset.top)}px`;
            popover.style.left = `${Math.round(left - offset.left)}px`;
          };

          // A mouse down on the Columns button closes the popover: its click must not open it again
          const swallowButtonClick = button => {
            const onClick = event => {
              document.removeEventListener("click", onClick, true);
              if (button.contains(event.target)) {
                event.preventDefault();
                event.stopPropagation();
              }
            };
            document.addEventListener("click", onClick, true);
            setTimeout(() => document.removeEventListener("click", onClick, true), CLICK_TIMEOUT);
          };

          // A mouse down outside the popover closes it, keeping the changes
          const onMouseDown = event => {
            const popover = getPopover();
            if (!popover || popover.contains(event.target)) {
              return;
            }
            const button = getButton();
            if (button && button.contains(event.target)) {
              swallowButtonClick(button);
            }
            applyScope(() => scope.closeColumns());
          };

          // Esc closes the popover, and not the window which holds the grid (a dialog or another popover)
          const onKeyDown = event => {
            const popover = getPopover();
            if (event.key !== "Escape" || !popover || !(popover.contains(event.target) || event.target === document.body)) {
              return;
            }
            event.stopPropagation();
            applyScope(() => scope.closeColumns());
          };

          const listen = on => {
            if (listening === on) {
              return;
            }
            listening = on;
            const method = on ? "addEventListener" : "removeEventListener";
            document[method]("mousedown", onMouseDown, true);
            document[method]("keydown", onKeyDown);
            element[0][method]("keydown", onKeyDown);
            window[method]("resize", place);
            window[method]("scroll", place, true);
          };

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

          // The grid columns-panel, or the application default (gridColumnsPanel setting) when the grid does not set it
          const getColumnsPanel = () => {
            const panel = (component.controller || {}).columnsPanel || $settings.get("gridColumnsPanel");
            return ["dialog", "side-panel"].includes(panel) ? panel : "popover";
          };

          scope.openColumns = function () {
            if (scope.columnsOpen) {
              return;
            }
            component.refreshLayoutNodes();
            takeSnapshot();
            scope.columnsPanel = getColumnsPanel();
            scope.sidePanel = scope.columnsPanel === "side-panel";
            scope.columnsOpen = true;
            if (scope.sidePanel) {
              getSidePanel().modal("show");
            } else if (scope.columnsPanel === "popover") {
              popoverTrap.activate();
              listen(true);
              // The popover is shown once the digest renders it
              scope.$$postDigest(() => {
                if (scope.columnsOpen && getPopover()) {
                  place();
                  popoverTrap.focusFirst();
                }
              });
            }
          };

          // Close the Columns keeping the changes (close button, Esc, click outside)
          scope.closeColumns = function () {
            if (scope.sidePanel && sidePanel) {
              // Bootstrap slides it out and then raises hidden.bs.modal, which marks the Columns as closed
              sidePanel.modal("hide");
              return;
            }
            if (scope.columnsPanel === "popover") {
              listen(false);
              // Give the focus back to the Columns button while the focus is still inside
              popoverTrap.deactivate();
            }
            scope.columnsOpen = false;
          };

          // Cancel: the grid gets back the layout it had when the Columns were opened, and they are closed
          scope.cancelColumns = function () {
            component.changeColumnLayout(snapshot);
            scope.closeColumns();
          };

          // Restore default keeps the Columns open and is the new layout Cancel goes back to
          scope.resetColumns = function () {
            component.resetColumnLayout();
            takeSnapshot();
          };

          // Save closes the Columns, as the close button, once the server answers that the layout was saved; when it
          // fails they stay open
          scope.saveColumns = function () {
            if (scope.saving) {
              return;
            }
            scope.saving = true;
            component.saveColumnLayout(() => applyScope(() => {
              if (scope.columnsOpen) {
                scope.closeColumns();
              }
            }));
          };

          // The Save ends (it worked or failed) when the Columns are no longer busy
          scope.$watch(() => component.layoutBusy, busy => {
            if (!busy) {
              scope.saving = false;
            }
          });

          // The grid grows over the maximize target like a window (Maximize service, started by the grid)
          scope.toggleMaximize = function () {
            scope.togglePanel();
          };

          bindDragAndDrop($(element));

          scope.$on("$destroy", () => {
            listen(false);
            $(element).off();
            trap.deactivate();
            popoverTrap.deactivate();
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
