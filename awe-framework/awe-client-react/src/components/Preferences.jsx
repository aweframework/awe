import React, {useEffect, useId, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {Dropdown} from "primereact/dropdown";
import {OverlayPanel} from "primereact/overlaypanel";
import PropTypes from "prop-types";
import {DEFAULT_LOGIN_SETTINGS, updateSettings} from "../redux/actions/settings";
import {getIconCode, getIconNode} from "../utilities";
import {loginMode, readLoginMode, storeLoginLanguage, storeLoginMode} from "../utilities/signin";

/**
 * Language and colour mode selectors. The sign-in page shows them in its footer; any other screen shows a compact
 * version with a tag of the class awe-preferences (the top bar of the public home of an application). Both keep the
 * choice as the sign-in page does: the language and the theme mode of the settings, and the mode for the next visit.
 */
export const PREFERENCES_STYLE = "awe-preferences";

const MODE_OPTIONS = [
  {mode: "light", icon: "lucide:sun", label: "ENUM_THEME_MODE_LIGHT"},
  {mode: "dark", icon: "lucide:moon", label: "ENUM_THEME_MODE_DARK"},
  {mode: "system", icon: "lucide:monitor", label: "ENUM_THEME_MODE_SYSTEM"}
];

/**
 * Change of language of a selector: the language of the settings and, without a user session, the pending choice the
 * login saves as the language of the user
 * @return Change the language
 */
function useChooseLanguage() {
  const dispatch = useDispatch();
  const authenticated = useSelector(state => state.settings.authenticated);
  return language => {
    storeLoginLanguage(language, authenticated);
    dispatch(updateSettings({language}));
  };
}

/**
 * Language selector: the languages of the application in the AWE select. A change is the change of language of the
 * application (the language of the settings, which the view turns into the language of the texts)
 * @param {object} props Component properties
 * @param {string[]} props.languages Codes of the languages to choose from
 */
export function LanguageSelector({languages}) {
  const chooseLanguage = useChooseLanguage();
  const language = useSelector(state => state.settings.language);
  const {t} = useTranslation();
  const options = languages.map(code => ({value: code, label: t(`ENUM_LAN_${code.toUpperCase()}`)}));
  const valueTemplate = option => <>
    {getIconNode("lucide:globe", {className: "signin-language-icon", "aria-hidden": "true"})}
    <span className="signin-language-value">{option?.label}</span>
  </>;

  return <Dropdown
    className="signin-language p-inputtext-sm"
    panelClassName="signin-language-panel"
    value={language}
    options={options}
    onChange={event => chooseLanguage(event.value)}
    valueTemplate={valueTemplate}
    ariaLabel={t("PARAMETER_LANGUAGE")}
    aria-label={t("PARAMETER_LANGUAGE")}
  />;
}

LanguageSelector.propTypes = {
  languages: PropTypes.arrayOf(PropTypes.string).isRequired
};

/**
 * Colour mode selector: light, dark or system as a segmented radio group. A change is the theme mode of the settings
 * (drawn at once on the page by the theme appearance of the application), kept for the next visit
 */
export function ColorModeSelector() {
  const dispatch = useDispatch();
  const current = loginMode(useSelector(state => state.settings.themeMode));
  const {t} = useTranslation();
  const name = useId();
  const change = mode => {
    storeLoginMode(mode);
    dispatch(updateSettings({themeMode: mode}));
  };

  return <div className="signin-mode" role="radiogroup" aria-label={t("PARAMETER_THEME_MODE")}>
    {MODE_OPTIONS.map(({mode, icon, label}) => <label key={mode} title={t(label)}>
      <input type="radio" name={name} value={mode} checked={current === mode} onChange={() => change(mode)}
             aria-label={t(label)}/>
      <span>
        {getIconNode(icon, {"aria-hidden": "true"})}
        <span className="p-hidden-accessible">{t(label)}</span>
      </span>
    </label>)}
  </div>;
}

/**
 * The mode chosen before is drawn again on the next visit (once: a later change is a choice)
 * @param {boolean} enabled The page has a colour mode selector
 */
export function useRestoredThemeMode(enabled) {
  const dispatch = useDispatch();
  const themeMode = useSelector(state => state.settings.themeMode);
  useEffect(() => {
    const stored = enabled ? readLoginMode() : undefined;
    if (stored && stored !== themeMode) {
      dispatch(updateSettings({themeMode: stored}));
    }
  }, [enabled]);
}

const BUTTON_ICON_CLASS = "p-button-icon p-c p-button-icon-left";

/**
 * Ghost button that opens a small menu of options as the user menu of the header: one option per line with its icon,
 * the current one marked with a check
 * @param {object} props Component properties
 * @param {string} props.className Class of the button
 * @param {string} props.icon Icon of the button
 * @param {string} [props.label] Text of the button
 * @param {string} props.name Accessible name and title of the button
 * @param {{value: string, label: string, icon?: string}[]} props.options Options of the menu
 * @param {string} props.value Current option
 * @param {function} props.onSelect Called with the value of the chosen option
 */
function PreferenceMenu({className, icon, label, name, options, value, onSelect}) {
  const panel = useRef(null);
  return <>
    <Button className={`btn-ghost p-button-text awe-button-spaced awe-preferences-button ${className}`} type="button"
            icon={getIconCode(icon, BUTTON_ICON_CLASS)} label={label} title={name} aria-label={name}
            aria-haspopup="menu" onClick={event => panel.current.toggle(event)}/>
    <OverlayPanel ref={panel} dismissable className="info-dropdown awe-preferences-panel">
      <div role="menu" aria-label={name}>
        {options.map(option => <Button key={option.value} type="button" role="menuitemradio"
                                       className="p-button-text awe-preferences-option"
                                       aria-checked={option.value === value}
                                       onClick={() => {
                                         onSelect(option.value);
                                         panel.current.hide();
                                       }}>
          {option.icon && getIconCode(option.icon, BUTTON_ICON_CLASS)}
          <span className="p-button-label p-c">{option.label}</span>
          {option.value === value && getIconNode("lucide:check", {className: "awe-preferences-check", "aria-hidden": "true"})}
        </Button>)}
      </div>
    </OverlayPanel>
  </>;
}

PreferenceMenu.propTypes = {
  className: PropTypes.string,
  icon: PropTypes.string.isRequired,
  label: PropTypes.string,
  name: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired, label: PropTypes.string.isRequired, icon: PropTypes.string
  })).isRequired,
  value: PropTypes.string,
  onSelect: PropTypes.func.isRequired
};

/**
 * Compact language selector: a ghost button with the globe and the short code of the language that opens the list
 * @param {object} props Component properties
 * @param {string[]} props.languages Codes of the languages to choose from
 */
function CompactLanguage({languages}) {
  const chooseLanguage = useChooseLanguage();
  const language = useSelector(state => state.settings.language);
  const {t} = useTranslation();
  const options = languages.map(code => ({value: code, label: t(`ENUM_LAN_${code.toUpperCase()}`)}));
  const code = (language ?? "").split("-")[0].toUpperCase();
  return <PreferenceMenu className="awe-preferences-language" icon="lucide:globe" label={code}
                         name={t("PARAMETER_LANGUAGE")} options={options} value={language}
                         onSelect={chooseLanguage}/>;
}

CompactLanguage.propTypes = {
  languages: PropTypes.arrayOf(PropTypes.string).isRequired
};

/** Compact colour mode selector: a ghost button with the icon of the current mode that opens the three modes */
function CompactMode() {
  const dispatch = useDispatch();
  const current = loginMode(useSelector(state => state.settings.themeMode));
  const {t} = useTranslation();
  const options = MODE_OPTIONS.map(({mode, icon, label}) => ({value: mode, icon, label: t(label)}));
  const change = mode => {
    storeLoginMode(mode);
    dispatch(updateSettings({themeMode: mode}));
  };
  return <PreferenceMenu className="awe-preferences-mode" name={t("PARAMETER_THEME_MODE")} options={options}
                         icon={MODE_OPTIONS.find(({mode}) => mode === current).icon} value={current}
                         onSelect={change}/>;
}

/**
 * Selectors of a tag with the class awe-preferences, in their compact version: the language one (only when the
 * application has more than one language, as the sign-in page) and the colour mode one
 */
export function Preferences() {
  const login = useSelector(state => state.settings.login) || DEFAULT_LOGIN_SETTINGS;
  const languages = login.languages ?? [];
  useRestoredThemeMode(true);
  return <>
    {login.languageSelector !== false && languages.length > 1 && <CompactLanguage languages={languages}/>}
    {login.modeSelector !== false && <CompactMode/>}
  </>;
}
