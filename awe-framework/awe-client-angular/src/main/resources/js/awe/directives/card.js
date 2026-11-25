import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {getIcon, iconMarkup} from "../services/icon";
import {buildCard, CARD_FIELDS, cardSparklineMarkup} from "../services/card";
import {TestIds} from "../data/testIds";
import {buildStatusStrip} from "../services/grid/statusStrip";
import {bindTooltip, stripElement} from "./column/statusStrip";

const delta = `<span class="awe-card-delta awe-card-delta-{{card.delta.tone}}" data-testid="${TestIds.cardDelta}" data-trend="{{card.delta.trend}}" data-tone="{{card.delta.tone}}">
  <span class="awe-card-delta-arrow" aria-hidden="true" ng-bind="card.delta.arrow"></span><span class="awe-card-delta-text" ng-bind="card.delta.text"></span><span class="sr-only" ng-bind="card.delta.label"></span>
</span>`;

// Half circle of the gauge: the bar draws the percent of its length (pathLength 100)
const GAUGE_ARC = "M 8 50 A 42 42 0 0 1 92 50";

// Style of a child of the card shown at the right of its header instead of its body (#993)
export const CARD_HEADER_ACTIONS = "awe-card-header-actions";

// The maximize button of a window (#993): its title and accessible name tell what it does next
const maximizeText = "{{(iconMaximized ? 'SCREEN_TEXT_RESTORE' : 'SCREEN_TEXT_MAXIMIZE') | translateMultiple}}";
const maximizeButton = `<button type="button" class="maximize-button awe-card-maximize-button" ng-if="::maximizable" data-testid="${TestIds.cardMaximizeButton}" ng-attr-title="${maximizeText}" ng-attr-aria-label="${maximizeText}" ng-attr-aria-pressed="{{iconMaximized === true}}" ng-click="togglePanel()"><i>${iconMarkup("compress", 'ng-if="iconMaximized"')}${iconMarkup("expand", 'ng-if="!iconMaximized"')}</i></button>`;

const template = `<section class="awe-card {{::controller.style}}" ng-class="{'maximized': maximized, 'maximizing': maximizing, 'resizing resizeTarget': panelResizing}" ng-attr-id="{{::cardId}}" ng-attr-aria-labelledby="{{card.title ? cardId + '-title' : undefined}}" data-testid="${TestIds.card}" ng-attr-data-testid-owner="{{::cardId}}" ng-attr-data-maximized="{{maximizable ? iconMaximized === true : undefined}}" ng-show="card.visible" ui-dependency="dependencies" ng-cloak>
  <header class="awe-card-header" ng-if="card.title || card.subtitle || card.chip || card.icon || card.deltaInHeader || hasHeaderActions || maximizable">
    <div class="awe-card-heading" ng-if="card.title || card.subtitle">
      <h3 class="awe-card-title" ng-attr-id="{{::cardId}}-title" ng-if="card.title && !card.titleSegments" ng-bind="card.title"></h3>
      <h3 class="awe-card-title" ng-attr-id="{{::cardId}}-title" ng-if="card.titleSegments"><span ng-repeat="segment in card.titleSegments track by $index"><strong ng-if="segment.strong" ng-bind="segment.text"></strong><span ng-if="!segment.strong" ng-bind="segment.text"></span></span></h3>
      <p class="awe-card-subtitle" ng-if="card.subtitle" ng-bind="card.subtitle"></p>
    </div>
    <span class="awe-card-chip awe-card-chip-{{card.chip.tone}}" data-testid="${TestIds.cardChip}" data-tone="{{card.chip.tone}}" ng-if="card.chip" ng-bind="card.chip.text"></span>
    <span class="awe-card-icon awe-card-icon-{{card.iconTone}}" aria-hidden="true" ng-if="card.icon">${getIconTemplate("awe-card-icon-glyph", "span", "getCardIcon()")}</span>
    ${delta.replace("<span ", '<span ng-if="card.deltaInHeader" ')}
    ${maximizeButton}
  </header>
  <ul class="awe-card-pills" ng-if="card.pills.length > 0">
    <li class="awe-card-pill" ng-repeat="pill in card.pills">${getIconTemplate("awe-card-pill-icon", "span", "getPillIcon(pill)", 'aria-hidden="true"')}<span class="awe-card-pill-label" ng-if="pill.label" ng-bind="pill.label"></span><span class="awe-card-pill-separator" aria-hidden="true" ng-if="pill.label && pill.code">·</span><code class="awe-card-pill-code" ng-if="pill.code" ng-bind="pill.code"></code></li>
  </ul>
  <div class="awe-card-value" data-testid="${TestIds.cardValue}" ng-if="card.value"><span class="awe-card-number" ng-bind="card.value.number"></span><span class="awe-card-unit" ng-if="card.value.unit" ng-bind="card.value.unit"></span></div>
  <div class="awe-card-stack" role="img" ng-attr-aria-label="{{card.stack.label}}" ng-if="card.stack"><span ng-repeat="segment in card.stack.segments" class="awe-card-stack-segment awe-card-stack-{{segment.tone}}" data-tone="{{segment.tone}}" ng-style="{'flex-grow': segment.value}" ng-attr-title="{{segment.text}}"></span></div>
  <div class="awe-card-sparkline awe-card-sparkline-{{card.iconTone}}" data-testid="${TestIds.cardSparkline}" ng-if="card.sparkline" ng-bind-html="sparkline"></div>
  <div class="awe-card-strip" ng-if="card.strip"><span class="column-status-strip"></span></div>
  <div class="awe-card-tiles-block" ng-if="card.tiles.length > 0">
    <span class="awe-card-tiles-label" ng-attr-id="{{::cardId}}-tiles" ng-if="card.tilesLabel" ng-bind="card.tilesLabel"></span>
    <ol class="awe-card-tiles" ng-attr-aria-labelledby="{{card.tilesLabel ? cardId + '-tiles' : undefined}}">
      <li class="awe-card-tile" ng-repeat="tile in card.tiles track by $index"><span class="awe-card-tile-label" ng-bind="tile.label"></span><strong class="awe-card-tile-value" ng-if="tile.value" ng-bind="tile.value"></strong><span class="awe-card-tile-hint" ng-if="tile.hint" ng-bind="tile.hint"></span></li>
    </ol>
  </div>
  <div class="awe-card-body"></div>
  <div class="awe-card-progress awe-card-progress-{{card.iconTone}}" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{{card.progress.width}}" data-testid="${TestIds.cardProgress}" ng-if="card.progress && card.progressStyle !== 'gauge'"><span class="awe-card-progress-bar" ng-style="{width: card.progress.width + '%'}"></span></div>
  <div class="awe-card-progress awe-card-gauge awe-card-progress-{{card.iconTone}}" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{{card.progress.width}}" data-testid="${TestIds.cardProgress}" ng-if="card.progress && card.progressStyle === 'gauge'"><svg class="awe-card-gauge-arc" viewBox="0 0 100 56" aria-hidden="true" focusable="false"><path class="awe-card-gauge-track" d="${GAUGE_ARC}" pathLength="100"/><path class="awe-card-gauge-bar" d="${GAUGE_ARC}" pathLength="100" ng-attr-stroke-dasharray="{{card.progress.width}} 100"/></svg><span class="awe-card-gauge-value" ng-bind="card.progress.percent + ' %'"></span></div>
  <p class="awe-card-progress-text" ng-if="card.progress" ng-bind="card.progress.text"></p>
  <p class="awe-card-description" ng-if="card.description" ng-bind="card.description"></p>
  <details class="awe-card-details" ng-if="card.details"><summary ng-bind="card.detailsLabel || card.details"></summary><p ng-bind="card.details"></p></details>
  <footer class="awe-card-footer" ng-if="card.footer || card.deltaInFooter || card.caption">
    <span class="awe-card-footer-text" ng-if="card.footer" ng-bind="card.footer"></span>
    <span class="awe-card-footer-end" ng-if="card.deltaInFooter || card.caption">${delta.replace("<span ", '<span ng-if="card.deltaInFooter" ')}<span class="awe-card-caption" ng-if="card.caption" ng-bind="card.caption"></span></span>
  </footer>
</section>`;

// Card directive (#899): a KPI tile filled from the first row of its model over its attributes, with an optional body
// of child components, a sparkline and a target progress (bar or gauge). The view rules are shared with the React
// engine (services/card). A child with the awe-card-header-actions style goes to the right of the header (a view
// switch, buttons) and, with the maximize attribute, a button at the end of the header grows the card over its maximize
// target and restores it, like a window (Maximize service, #993)
aweApplication.directive('aweCard',
  ['Component', 'Control', 'AweSettings', 'Maximize', '$filter', '$sce',
    function (Component, Control, $settings, Maximize, $filter, $sce) {
      return {
        restrict: 'E',
        transclude: true,
        replace: true,
        template,
        scope: {
          cardId: '@'
        },
        link: function (scope, element, attrs, controller, transclude) {
          const component = new Component(scope, scope.cardId);
          if (!component.asComponent()) {
            return false;
          }

          const translate = $filter('translateMultiple');
          const getRow = () => component.model?.values?.[0] || null;
          // Status strip of the card: the strip of the status strip column (its bars and its tooltip), drawn after the
          // digest that shows its holder (ng-if), and again only when its holder or its statuses change
          let stripTarget = null;
          let drawnStrip = null;
          let hideStripTooltip = () => undefined;
          const drawStrip = () => {
            const target = element.find(".awe-card-strip > .column-status-strip")[0] || null;
            const strip = target ? buildStatusStrip(scope.card?.strip) : null;
            const key = JSON.stringify(strip);
            if (target === stripTarget && key === drawnStrip) {
              return;
            }
            hideStripTooltip();
            hideStripTooltip = () => undefined;
            stripTarget = target;
            drawnStrip = key;
            if (target) {
              $(target).empty();
            }
            if (target && strip) {
              const holder = stripElement(strip);
              hideStripTooltip = bindTooltip(holder);
              target.appendChild(holder);
            }
          };
          const render = () => {
            scope.card = buildCard(component.controller, getRow(), {
              language: $settings.getLanguage(), translate
            });
            // The SVG of the sparkline is written by the shared renderer (escaped values), so it is trusted
            scope.sparkline = $sce.trustAsHtml(cardSparklineMarkup(scope.card.sparkline));
            scope.$$postDigest(drawStrip);
          };
          scope.getCardIcon = () => getIcon(scope.card?.icon);
          scope.getPillIcon = pill => getIcon(pill?.icon);
          element.on("$destroy", () => hideStripTooltip());

          // A maximizable card grows over its maximize target like a window; its body is emptied in the animation clone
          scope.maximizable = [true, "true"].includes(component.controller.maximize);
          if (scope.maximizable) {
            Maximize.initMaximize(scope, element, {contentSelector: ".awe-card-body"});
          }

          // The children go in the body, below the value; without children there is no body. The children with the
          // header actions style go to the header, after its heading, once the header is drawn (it is always drawn
          // while the card has them, so they are placed once)
          const body = element.find(".awe-card-body");
          const isHeaderAction = node => node.nodeType === node.ELEMENT_NODE && node.classList.contains(CARD_HEADER_ACTIONS);
          let headerActions = [];
          transclude((clone, cloneScope) => {
            const nodes = clone.toArray();
            headerActions = nodes.filter(isHeaderAction);
            const content = nodes.filter(node => !isHeaderAction(node));
            const filled = content.some(node => node.nodeType === node.ELEMENT_NODE ||
              (node.nodeType === node.TEXT_NODE && node.textContent.trim() !== ""));
            if (filled) {
              body.append(content);
            } else {
              body.remove();
            }
            if (!filled && headerActions.length === 0) {
              cloneScope.$destroy();
            }
          });
          scope.hasHeaderActions = headerActions.length > 0;
          const placeHeaderActions = () => {
            const header = element.children(".awe-card-header");
            if (header.length === 0) {
              return;
            }
            // After the heading or, without it, after the place it takes when it is shown (the comment of its ng-if,
            // the first one of the header), so a title that comes later stays before the actions
            const heading = header.children(".awe-card-heading");
            const anchor = heading.length > 0 ? heading[0]
              : header.contents().toArray().find(node => node.nodeType === node.COMMENT_NODE);
            if (anchor) {
              $(anchor).after(headerActions);
            } else {
              header.prepend(headerActions);
            }
            headerActions = [];
          };
          if (scope.hasHeaderActions) {
            scope.$$postDigest(placeHeaderActions);
          }

          // The fill and reload actions replace the model values (the generic API keeps only the selected ones)
          component.api.updateModelValues = data => {
            if (component.model) {
              Object.assign(component.model, data);
            }
          };

          // Render again when the data (fill, reload), the attributes (dependencies) or the language change
          scope.$watch(() => CARD_FIELDS.map(name => [component.controller[name], getRow()?.[name]]), render, true);
          component.listeners['languageChanged'] = scope.$on('languageChanged', () => {
            render();
          });
          render();
        }
      };
    }
  ]);
