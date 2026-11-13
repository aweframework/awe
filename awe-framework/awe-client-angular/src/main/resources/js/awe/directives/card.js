import {aweApplication} from "../awe";
import {getIconTemplate} from "../services/component";
import {getIcon} from "../services/icon";
import {buildCard, CARD_FIELDS, cardSparklineMarkup} from "../services/card";
import {TestIds} from "../data/testIds";

const delta = `<span class="awe-card-delta awe-card-delta-{{card.delta.tone}}" data-testid="${TestIds.cardDelta}" data-trend="{{card.delta.trend}}" data-tone="{{card.delta.tone}}">
  <span class="awe-card-delta-arrow" aria-hidden="true" ng-bind="card.delta.arrow"></span><span class="awe-card-delta-text" ng-bind="card.delta.text"></span><span class="sr-only" ng-bind="card.delta.label"></span>
</span>`;

// Half circle of the gauge: the bar draws the percent of its length (pathLength 100)
const GAUGE_ARC = "M 8 50 A 42 42 0 0 1 92 50";

const template = `<section class="awe-card {{::controller.style}}" ng-attr-id="{{::cardId}}" ng-attr-aria-labelledby="{{card.title ? cardId + '-title' : undefined}}" data-testid="${TestIds.card}" ng-attr-data-testid-owner="{{::cardId}}" ng-show="card.visible" ui-dependency="dependencies" ng-cloak>
  <header class="awe-card-header" ng-if="card.title || card.subtitle || card.chip || card.icon || card.deltaInHeader">
    <div class="awe-card-heading" ng-if="card.title || card.subtitle">
      <h3 class="awe-card-title" ng-attr-id="{{::cardId}}-title" ng-if="card.title" ng-bind="card.title"></h3>
      <p class="awe-card-subtitle" ng-if="card.subtitle" ng-bind="card.subtitle"></p>
    </div>
    <span class="awe-card-chip awe-card-chip-{{card.chip.tone}}" data-testid="${TestIds.cardChip}" data-tone="{{card.chip.tone}}" ng-if="card.chip" ng-bind="card.chip.text"></span>
    <span class="awe-card-icon awe-card-icon-{{card.iconTone}}" aria-hidden="true" ng-if="card.icon">${getIconTemplate("awe-card-icon-glyph", "span", "getCardIcon()")}</span>
    ${delta.replace("<span ", '<span ng-if="card.deltaInHeader" ')}
  </header>
  <div class="awe-card-value" data-testid="${TestIds.cardValue}" ng-if="card.value"><span class="awe-card-number" ng-bind="card.value.number"></span><span class="awe-card-unit" ng-if="card.value.unit" ng-bind="card.value.unit"></span></div>
  <div class="awe-card-sparkline awe-card-sparkline-{{card.iconTone}}" data-testid="${TestIds.cardSparkline}" ng-if="card.sparkline" ng-bind-html="sparkline"></div>
  <div class="awe-card-body"></div>
  <div class="awe-card-progress awe-card-progress-{{card.iconTone}}" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{{card.progress.width}}" data-testid="${TestIds.cardProgress}" ng-if="card.progress && card.progressStyle !== 'gauge'"><span class="awe-card-progress-bar" ng-style="{width: card.progress.width + '%'}"></span></div>
  <div class="awe-card-progress awe-card-gauge awe-card-progress-{{card.iconTone}}" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{{card.progress.width}}" data-testid="${TestIds.cardProgress}" ng-if="card.progress && card.progressStyle === 'gauge'"><svg class="awe-card-gauge-arc" viewBox="0 0 100 56" aria-hidden="true" focusable="false"><path class="awe-card-gauge-track" d="${GAUGE_ARC}" pathLength="100"/><path class="awe-card-gauge-bar" d="${GAUGE_ARC}" pathLength="100" ng-attr-stroke-dasharray="{{card.progress.width}} 100"/></svg><span class="awe-card-gauge-value" ng-bind="card.progress.percent + ' %'"></span></div>
  <p class="awe-card-progress-text" ng-if="card.progress" ng-bind="card.progress.text"></p>
  <p class="awe-card-description" ng-if="card.description" ng-bind="card.description"></p>
  <footer class="awe-card-footer" ng-if="card.footer || card.deltaInFooter || card.caption">
    <span class="awe-card-footer-text" ng-if="card.footer" ng-bind="card.footer"></span>
    <span class="awe-card-footer-end" ng-if="card.deltaInFooter || card.caption">${delta.replace("<span ", '<span ng-if="card.deltaInFooter" ')}<span class="awe-card-caption" ng-if="card.caption" ng-bind="card.caption"></span></span>
  </footer>
</section>`;

// Card directive (#899): a KPI tile filled from the first row of its model over its attributes, with an optional body
// of child components, a sparkline and a target progress (bar or gauge). The view rules are shared with the React
// engine (services/card)
aweApplication.directive('aweCard',
  ['Component', 'Control', 'AweSettings', '$filter', '$sce',
    function (Component, Control, $settings, $filter, $sce) {
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
          const render = () => {
            scope.card = buildCard(component.controller, getRow(), {
              language: $settings.getLanguage(), translate
            });
            // The SVG of the sparkline is written by the shared renderer (escaped values), so it is trusted
            scope.sparkline = $sce.trustAsHtml(cardSparklineMarkup(scope.card.sparkline));
          };
          scope.getCardIcon = () => getIcon(scope.card?.icon);

          // The children go in the body, below the value; without children there is no body
          const body = element.find(".awe-card-body");
          transclude((clone, cloneScope) => {
            const filled = clone.toArray().some(node => node.nodeType === node.ELEMENT_NODE ||
              (node.nodeType === node.TEXT_NODE && node.textContent.trim() !== ""));
            if (filled) {
              body.append(clone);
            } else {
              cloneScope.$destroy();
              body.remove();
            }
          });

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
