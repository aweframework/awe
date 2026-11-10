import {aweApplication} from "../awe";
import {applyThemeAppearance} from "awe-client-react/utilities/themeAppearance";
import {
  followsDevice,
  NO_PALETTE,
  PREVIEW_DEFAULT_PRIMARY,
  PREVIEW_DEFAULT_SURFACE,
  PREVIEW_MODES,
  PREVIEW_PALETTE_DELAY,
  PREVIEW_PALETTE_QUERY,
  PREVIEW_SYSTEM_MODE,
  preparePreviewDocument,
  previewAppearance,
  previewColour,
  previewHexes,
  previewPaletteOf
} from "awe-client-react/utilities/themePreview";

// The mode switch is the segmented control of a button radio criterion
const template = `<div ng-attr-id="{{::controller.id}}" class="awe-theme-preview {{::controller.style}}" ng-show="controller.visible" ui-dependency="dependencies" ng-cloak>
  <div class="awe-theme-preview-mode" ng-if="deviceFollowed">
    <div role="radiogroup" aria-label="{{'SCREEN_TEXT_THEME_PREVIEW_MODE' | translateMultiple}}" class="btn-group button-radio-group">
      <label class="btn btn-awe btn-{{::size}}" ng-repeat="option in modeOptions track by option.value" ng-attr-option-id="{{::option.value}}" ng-class="{'active btn-primary': deviceMode === option.value}">
        <input type="radio" class="form-control px" ng-attr-name="{{::radioName}}" ng-attr-value="{{::option.value}}" ng-attr-aria-label="{{option.label | translateMultiple}}" ng-checked="deviceMode === option.value" ng-click="chooseMode(option.value)"/>
        <i></i>
        <span class="lbl">{{option.label | translateMultiple}}</span>
      </label>
    </div>
  </div>
  <iframe class="awe-theme-preview-frame" title="{{controller.label | translateMultiple}}" tabindex="-1"></iframe>
</div>`;

/**
 * Element of the sample
 * @param {Document} target Document of the preview
 * @param {string} tag Tag name
 * @param {string} className Class of the element
 * @param {Array<Node|string>} children Children (a string is a text)
 * @returns {HTMLElement} Element
 */
const sampleElement = (target, tag, className, children = []) => {
  const element = target.createElement(tag);
  element.className = className;
  element.append(...children);
  return element;
};

/**
 * Sample of the application, with the content and the classes of the React engine (ThemePreviewSample): a page with a
 * raised card, a title, text with a link, a tag, a focused field and the primary and secondary actions. It is a picture
 * of the theme: nothing in it can be focused or used
 * @param {Document} target Document of the preview
 * @param {function} translate Translate a text
 * @returns {HTMLElement} Sample
 */
const buildSample = (target, translate) => {
  const element = (...args) => sampleElement(target, ...args);
  const sample = element("div", "awe-theme-preview-sample", [
    element("section", "awe-theme-preview-card", [
      element("header", "awe-theme-preview-card-header", [
        element("span", "awe-theme-preview-title", [translate("SCREEN_TEXT_THEME_PREVIEW_TITLE")]),
        element("span", "awe-theme-preview-tag", [translate("PARAMETER_ACTIVE")])
      ]),
      element("p", "awe-theme-preview-text", [`${translate("SCREEN_TEXT_THEME_PREVIEW_TEXT")} `,
        element("span", "awe-theme-preview-link", [translate("SCREEN_TEXT_THEME_PREVIEW_LINK")])]),
      element("div", "awe-theme-preview-field", [
        element("span", "awe-theme-preview-label", [translate("SCREEN_TEXT_THEME_PREVIEW_FIELD")]),
        element("span", "awe-theme-preview-input", [translate("SCREEN_TEXT_THEME_PREVIEW_PLACEHOLDER")])
      ]),
      element("footer", "awe-theme-preview-actions", [
        element("span", "awe-theme-preview-button awe-theme-preview-button-secondary", [translate("BUTTON_CANCEL")]),
        element("span", "awe-theme-preview-button awe-theme-preview-button-primary", [translate("BUTTON_SAVE_CHANGES")])
      ])
    ])
  ]);
  sample.setAttribute("inert", "");
  return sample;
};

/**
 * Selected value of a criterion, trimmed
 * @param {object} model Criterion model
 * @returns {string} Value, empty when there is none
 */
const criterionValue = model => {
  const selected = Array.isArray(model?.selected) ? model.selected[0] : model?.selected;
  return String(selected ?? "").trim();
};

/**
 * Theme preview directive (#949): a sample of the application drawn with the primary colour, surface and mode of the
 * criteria it names, while they change and before the theme is saved, as the React engine does (AweThemePreview).
 *
 * The design tokens are resolved on the root element of a document, so the sample lives in an iframe of its own whose
 * root gets the appearance (as the application does on its own root) and whose head gets the stylesheets of the
 * application: the theme of the application is never touched. A named palette is an attribute; a custom hex needs its
 * generated palette, requested to the server after a short pause. An invalid or empty hex keeps the last valid colour.
 * In the system mode the sample follows the device, and a switch shows it in the light or the dark mode.
 */
aweApplication.directive('aweThemePreview',
  ['Component', 'Control', 'Ajax', 'AweSettings', 'AweUtilities', '$translate', '$timeout', '$q',
    function (Component, Control, Ajax, $settings, Utilities, $translate, $timeout, $q) {
      return {
        restrict: 'E',
        replace: true,
        template,
        scope: {
          themePreviewId: '@'
        },
        link: function (scope, elem) {
          const component = new Component(scope, scope.themePreviewId);
          if (!component.asComponent()) {
            return false;
          }

          const controller = component.controller;
          const frame = elem.find("iframe")[0];
          const criterion = id => criterionValue(Control.getAddressModel({view: component.address.view, component: id}));
          const translate = key => $translate.instant(key);

          scope.size = $settings.get("defaultComponentSize");
          scope.radioName = `${controller.id}-mode`;
          scope.modeOptions = [PREVIEW_SYSTEM_MODE, ...PREVIEW_MODES]
            .map(value => ({value, label: `ENUM_THEME_MODE_${value.toUpperCase()}`}));
          scope.deviceMode = PREVIEW_SYSTEM_MODE;
          scope.deviceFollowed = true;

          // Last valid colours of the criteria, and the colours drawn: a hex is drawn once its palette has arrived, so
          // the sample never shows a half theme
          let primaryColour = PREVIEW_DEFAULT_PRIMARY;
          let surfaceColour = PREVIEW_DEFAULT_SURFACE;
          let colours = {primary: PREVIEW_DEFAULT_PRIMARY, surface: PREVIEW_DEFAULT_SURFACE, palette: NO_PALETTE};
          let themeMode = "";
          const palettes = new Map();
          let pending = null;

          // Document of the preview: ready once the iframe is in the page (and again if the browser replaces it on load)
          let frameDocument = null;
          let observer = null;
          const drawSample = () => {
            if (frameDocument) {
              frameDocument.body.replaceChildren(buildSample(frameDocument, translate));
            }
          };
          const render = () => {
            if (frameDocument) {
              const mode = scope.deviceFollowed ? scope.deviceMode : themeMode;
              applyThemeAppearance(previewAppearance(colours, mode, $settings.get("density")),
                frameDocument.documentElement);
            }
          };
          const prepareFrame = () => {
            const target = frame.contentDocument;
            if (!target?.body || target === frameDocument) {
              return;
            }
            frameDocument = target;
            preparePreviewDocument(target);
            drawSample();
            render();
            // The iframe takes the height of its sample
            observer?.disconnect();
            if (typeof window.ResizeObserver !== "undefined") {
              observer = new window.ResizeObserver(() => {
                frame.style.height = `${target.documentElement.scrollHeight}px`;
              });
              observer.observe(target.body);
            }
          };
          frame.addEventListener("load", prepareFrame);

          const cancelPending = () => {
            if (pending) {
              $timeout.cancel(pending.timer);
              pending.canceller?.resolve();
              pending = null;
            }
          };

          /**
           * Draw the last valid colours: at once for the named palettes and the palettes already generated, after a
           * pause and the answer of the server for a new hex. A newer colour cancels the pending request
           */
          const drawColours = () => {
            cancelPending();
            const primary = primaryColour;
            const surface = surfaceColour;
            const hexes = previewHexes(primary, surface);
            const key = `${hexes.primaryHex}|${hexes.surfaceHex}`;
            const draw = palette => {
              colours = {primary, surface, palette};
              render();
            };
            if (!hexes.primaryHex && !hexes.surfaceHex) {
              draw(NO_PALETTE);
              return;
            }
            if (palettes.has(key)) {
              draw(palettes.get(key));
              return;
            }
            const request = {canceller: $q.defer()};
            request.timer = $timeout(() => {
              const parameters = {...hexes};
              parameters[$settings.get("serverActionKey")] = "data";
              parameters[$settings.get("targetActionKey")] = PREVIEW_PALETTE_QUERY;
              Ajax.httpRequest({
                method: "POST",
                url: Ajax.getActionUrl("data", PREVIEW_PALETTE_QUERY),
                data: parameters,
                timeout: request.canceller.promise
              }).then(response => {
                const palette = previewPaletteOf(Utilities.decodeData(response.data,
                  $settings.get("encodeTransmission")));
                if (palette) {
                  palettes.set(key, palette);
                  draw(palette);
                }
              })
                // A cancelled or failed request keeps the last valid preview
                .catch(() => undefined);
            }, PREVIEW_PALETTE_DELAY, false);
            pending = request;
          };

          /**
           * Show the preview in a mode while the theme follows the device
           * @param {string} mode System, light or dark
           */
          scope.chooseMode = mode => {
            scope.deviceMode = mode;
            render();
          };

          // Follow the criteria and the density of the application. The colours are drawn again only when their last
          // valid value changes, so an invalid hex neither cancels nor launches a request
          let started = false;
          scope.$watch(() => [criterion(controller.primary), criterion(controller.primaryHex),
            criterion(controller.surface), criterion(controller.surfaceHex), criterion(controller.mode),
            $settings.get("density")], ([primary, primaryHex, surface, surfaceHex, mode]) => {
            prepareFrame();
            const nextPrimary = previewColour(primary, primaryHex) || primaryColour;
            const nextSurface = previewColour(surface, surfaceHex) || surfaceColour;
            themeMode = mode;
            scope.deviceFollowed = followsDevice(mode);
            if (!started || nextPrimary !== primaryColour || nextSurface !== surfaceColour) {
              started = true;
              primaryColour = nextPrimary;
              surfaceColour = nextSurface;
              drawColours();
            }
            render();
          }, true);

          scope.$on("languageChanged", drawSample);
          scope.$on("$destroy", () => {
            cancelPending();
            observer?.disconnect();
            frame.removeEventListener("load", prepareFrame);
          });
        }
      };
    }
  ]);
