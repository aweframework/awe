import React, {useCallback, useEffect, useRef, useState} from "react";
import {createPortal} from "react-dom";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {SelectButton} from "primereact/selectbutton";
import PropTypes from "prop-types";
import "./AweThemePreview.less";
import {fetchAction, isFetchError, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {applyThemeAppearance} from "../utilities/themeAppearance";
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
} from "../utilities/themePreview";
import {useComponentState} from "../hooks/useComponentState";
import {runRefusal} from "../redux/thunks/refusal";

/**
 * Selected value of a criterion, trimmed
 * @param {string} id Criterion identifier
 * @return {string} Value, empty when there is none
 */
function useCriterionValue(id) {
  const {model} = useComponentState(id) || {};
  const selected = (model?.values || []).find((value) => value.selected);
  return String(selected?.value ?? "").trim();
}

/**
 * Keep the last valid value of a colour: an invalid one returns the previous valid value
 * @param {string|null} colour Colour, null when it is not valid
 * @param {string} initial Value until the first valid one
 * @return {string} Last valid colour
 */
function useLastValid(colour, initial) {
  const last = useRef(initial);
  if (colour) {
    last.current = colour;
  }
  return last.current;
}

/**
 * Theme preview (#949): a sample of the application drawn with the primary colour, surface and mode of the criteria
 * it names, while they change and before the theme is saved.
 *
 * The design tokens are resolved on the root element of a document, so the sample lives in an iframe of its own whose
 * root gets the appearance (as the application does on its own root) and whose head gets the stylesheets of the
 * application: the theme of the application is never touched. A named palette is an attribute; a custom hex needs its
 * generated palette, requested to the server after a short pause. An invalid or empty hex keeps the last valid colour.
 * In the system mode the sample follows the device, and a switch shows it in the light or the dark mode.
 */
function AweThemePreview(props) {
  const {id, primary, primaryHex, surface, surfaceHex, mode, label, style} = props;
  const {attributes = {}} = useComponentState(id) || {};
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const {token, density} = useSelector((state) => state.settings || {});

  const primaryColour = useLastValid(previewColour(useCriterionValue(primary), useCriterionValue(primaryHex)),
    PREVIEW_DEFAULT_PRIMARY);
  const surfaceColour = useLastValid(previewColour(useCriterionValue(surface), useCriterionValue(surfaceHex)),
    PREVIEW_DEFAULT_SURFACE);
  const themeMode = useCriterionValue(mode);
  const [deviceMode, setDeviceMode] = useState(PREVIEW_SYSTEM_MODE);
  const deviceFollowed = followsDevice(themeMode);
  const previewMode = deviceFollowed ? deviceMode : themeMode;

  // Colours drawn: a hex is drawn once its palette has arrived, so the sample never shows a half theme
  const [colours, setColours] = useState({primary: PREVIEW_DEFAULT_PRIMARY, surface: PREVIEW_DEFAULT_SURFACE,
    palette: NO_PALETTE});
  const palettes = useRef(new Map());
  useEffect(() => {
    const hexes = previewHexes(primaryColour, surfaceColour);
    const key = `${hexes.primaryHex}|${hexes.surfaceHex}`;
    const draw = (palette) => setColours({primary: primaryColour, surface: surfaceColour, palette});
    if (!hexes.primaryHex && !hexes.surfaceHex) {
      draw(NO_PALETTE);
      return undefined;
    }
    if (palettes.current.has(key)) {
      draw(palettes.current.get(key));
      return undefined;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      fetchAction("data", PREVIEW_PALETTE_QUERY, hexes, token, controller.signal)
        .then((response) => {
          if (isFetchError(response)) {
            runRefusal(response, dispatch);
            return;
          }
          const palette = previewPaletteOf(response);
          if (palette) {
            palettes.current.set(key, palette);
            draw(palette);
          }
        })
        // A cancelled or failed request keeps the last valid preview
        .catch(() => undefined);
    }, PREVIEW_PALETTE_DELAY);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [primaryColour, surfaceColour, token, dispatch]);

  // Document of the preview: ready after the iframe is mounted (and again if the browser replaces it on load)
  const frame = useRef(null);
  const prepared = useRef(null);
  const [frameDocument, setFrameDocument] = useState(null);
  const prepareFrame = useCallback(() => {
    const target = frame.current?.contentDocument;
    if (!target?.body || target === prepared.current) {
      return;
    }
    prepared.current = target;
    preparePreviewDocument(target);
    setFrameDocument(target);
  }, []);
  useEffect(prepareFrame, [prepareFrame]);

  useEffect(() => {
    if (frameDocument) {
      applyThemeAppearance(previewAppearance(colours, previewMode, density), frameDocument.documentElement);
    }
  }, [frameDocument, colours, previewMode, density]);

  // The iframe takes the height of its sample
  useEffect(() => {
    if (!frameDocument || typeof ResizeObserver === "undefined") {
      return undefined;
    }
    const observer = new ResizeObserver(() => {
      if (frame.current) {
        frame.current.style.height = `${frameDocument.documentElement.scrollHeight}px`;
      }
    });
    observer.observe(frameDocument.body);
    return () => observer.disconnect();
  }, [frameDocument]);

  if (attributes.visible === false) {
    return null;
  }

  const modeOptions = [PREVIEW_SYSTEM_MODE, ...PREVIEW_MODES]
    .map((value) => ({value, label: t(`ENUM_THEME_MODE_${value.toUpperCase()}`)}));
  return <div id={id} className={classNames("awe-theme-preview", style)}>
    {deviceFollowed &&
      <div className="field awe-theme-preview-mode" role="group" aria-label={t("SCREEN_TEXT_THEME_PREVIEW_MODE")}>
        <SelectButton value={deviceMode} options={modeOptions} allowEmpty={false}
                      onChange={(event) => setDeviceMode(event.value)}/>
      </div>}
    <iframe ref={frame} className="awe-theme-preview-frame" title={translateLabel(label, t)} tabIndex={-1}
            onLoad={prepareFrame}/>
    {frameDocument && createPortal(<ThemePreviewSample t={t}/>, frameDocument.body)}
  </div>;
}

/**
 * Sample of the application: a page with a raised card, a title, text with a link, a tag, a focused field and the
 * primary and secondary actions. It is a picture of the theme: nothing in it can be focused or used
 */
function ThemePreviewSample({t}) {
  return <div className="awe-theme-preview-sample" inert>
    <section className="awe-theme-preview-card">
      <header className="awe-theme-preview-card-header">
        <span className="awe-theme-preview-title">{t("SCREEN_TEXT_THEME_PREVIEW_TITLE")}</span>
        <span className="awe-theme-preview-tag">{t("PARAMETER_ACTIVE")}</span>
      </header>
      <p className="awe-theme-preview-text">
        {t("SCREEN_TEXT_THEME_PREVIEW_TEXT")} <span className="awe-theme-preview-link">
          {t("SCREEN_TEXT_THEME_PREVIEW_LINK")}</span>
      </p>
      <div className="awe-theme-preview-field">
        <span className="awe-theme-preview-label">{t("SCREEN_TEXT_THEME_PREVIEW_FIELD")}</span>
        <span className="awe-theme-preview-input">{t("SCREEN_TEXT_THEME_PREVIEW_PLACEHOLDER")}</span>
      </div>
      <footer className="awe-theme-preview-actions">
        <span className="awe-theme-preview-button awe-theme-preview-button-secondary">{t("BUTTON_CANCEL")}</span>
        <span className="awe-theme-preview-button awe-theme-preview-button-primary">{t("BUTTON_SAVE_CHANGES")}</span>
      </footer>
    </section>
  </div>;
}

ThemePreviewSample.propTypes = {
  t: PropTypes.func.isRequired
};

AweThemePreview.propTypes = {
  id: PropTypes.string,
  primary: PropTypes.string,
  primaryHex: PropTypes.string,
  surface: PropTypes.string,
  surfaceHex: PropTypes.string,
  mode: PropTypes.string,
  label: PropTypes.string,
  style: PropTypes.string
};

export default AweThemePreview;
