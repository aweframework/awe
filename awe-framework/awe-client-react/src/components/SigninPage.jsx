import React, {createContext, useCallback, useContext, useEffect, useId, useMemo, useRef, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {Dropdown} from "primereact/dropdown";
import PropTypes from "prop-types";
import {DEFAULT_LOGIN_SETTINGS, updateSettings} from "../redux/actions/settings";
import {removeSigninAlert} from "../redux/actions/messages";
import {selectServerActionComponent, selectServerActionRunning} from "../redux/selectors/stateSelectors";
import {getAllComponents} from "../redux/selectors/componentSelectors";
import {ButtonTypes} from "../redux/actions/components";
import {useDarkMode} from "../hooks/useDarkMode";
import {getIconNode} from "../utilities";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";
import {alertType} from "../utilities/testPassThrough";
import {get, subscribe} from "../utilities/loginEffects";
import {
  cssUrl,
  launchesServerAction as launches,
  LOGIN_ACTION,
  loginEffectName,
  loginLayout,
  loginMode,
  loginText,
  readLoginMode,
  resolveImageUrl,
  SIGNIN_PAGE_ID,
  storeLoginMode
} from "../utilities/signin";

/**
 * Sign-in page branded through settings.login (awe.application.login.*). It wraps the page of the sign-in screen XML
 * (id page-signin) and adds what the XML cannot read from the properties: the layout, the background image, the logo,
 * the form title and subtitle, the caption of the image and the footer. The parts that go inside the XML (the logo in
 * signin-info, the title in signin-form) reach the tags of the page through a context, so an overridden sign-in screen
 * with the same classes is branded too.
 *
 * With awe.application.login.background-effect the background image (and the brand logo of the panel) is a canvas
 * effect the application registered under that name (loginEffects.ts), in the split and card layouts. AWE ships no
 * effect: an unknown name draws nothing and leaves the image, and an effect registered later starts when it registers.
 *
 * The other login pages (S19: the change password, expired password and two-factor screens) take the same frame: the
 * layout, the images, the caption, the footer, the fields and the buttons. Their column, brand and form take the classes
 * of the sign-in screen XML, so they are drawn by the same rules. The title, the subtitle, the failed login alert and
 * the single sign-on block stay on the sign-in page; the other pages keep their own title and their toasts.
 */
const SigninContext = createContext(null);

const hasClass = (style, name) => typeof style === "string" && style.split(/\s+/).includes(name);

// Server actions of the single sign-on buttons of the sign-in screen XML: Microsoft Entra ID and SSO
const MICROSOFT_ACTION = "login-azure";
const SSO_ACTION = "login-sso";
// A single sign-on button of the page is visible
const selectSsoVisible = state => Object.values(getAllComponents(state)).some(component =>
  component?.attributes?.visible && [MICROSOFT_ACTION, SSO_ACTION].some(name => launches(component.actions, name)));
const selectSigningIn = state => selectServerActionRunning(state, LOGIN_ACTION);
const selectAlert = state => state.messages?.signinAlert;
// The submit button of the page whose server action runs (the change password and two-factor pages)
const selectSubmitRunning = state => {
  const id = selectServerActionComponent(state);
  return id && getAllComponents(state)[id]?.attributes?.buttonType === ButtonTypes.BUTTON_SUBMIT ? id : undefined;
};
const noSelection = () => undefined;

// Classes of the column, the brand and the form of the other login pages, which take those of the sign-in screen XML
const COLUMNS = ["change-password-container", "totp-container"];
const BRANDS = ["signin-info", "change-password-header", "totp-header"];
const FORMS = ["signin-form", "change-password-form", "totp-form"];
const hasAnyClass = (style, names) => names.some(name => hasClass(style, name));

/**
 * Texts a login page shows, translated: the title and the subtitle of the sign-in form and the caption of the image
 * panel (the other login pages keep their own title)
 * @param {object} login settings.login
 * @param {boolean} signIn The page is the sign-in page
 * @param {function} text Translate a configured text (a locale key or a literal)
 * @returns {{layout: string, title: string|undefined, subtitle: string|undefined, panelTitle: string|undefined,
 * panelSubtitle: string|undefined, footerText: string|undefined}} Layout and texts of the page
 */
const loginTexts = (login, signIn, text) => {
  const layout = loginLayout(login.layout);
  return {
    layout,
    title: signIn ? text(login.title) : undefined,
    subtitle: signIn ? text(login.subtitle) : undefined,
    panelTitle: layout === "form-only" ? undefined : text(login.panelTitle),
    panelSubtitle: layout === "form-only" ? undefined : text(login.panelSubtitle),
    footerText: text(login.footerText)
  };
};

/**
 * Selectors of the footer of a login page (a single language has nothing to choose, as the AngularJS engine)
 * @param {object} login settings.login
 * @returns {{languages: string[], languageSelector: boolean, modeSelector: boolean}} Languages of the language
 * selector, and whether the language and the colour mode selectors are shown
 */
const loginSelectors = login => ({
  languages: login.languages ?? [],
  languageSelector: login.languageSelector !== false && login.languages?.length > 1,
  modeSelector: login.modeSelector !== false
});

/**
 * Alert of a failed login at the top of the form (prototype .message--danger.login__alert): the title of the message
 * of the server in bold, then its text
 */
function SigninAlert({alert}) {
  const {severity, summary, detail} = alert;
  // The type of the message as the toast of the same message names it
  return <div className="signin-alert signin-alert--danger" role="alert"
              {...testHook(TestIds.alert, {type: alertType(severity)})}>
    {getIconNode("lucide:circle-x", {className: "signin-alert-icon", "aria-hidden": "true"})}
    <span>
      {summary && <strong {...testHook(TestIds.alertTitle)}>{summary}</strong>}{summary && detail && " "}
      {detail && <span className="signin-alert-text" {...testHook(TestIds.alertMessage)}>{detail}</span>}
    </span>
  </div>;
}

SigninAlert.propTypes = {
  alert: PropTypes.shape({severity: PropTypes.string, summary: PropTypes.string, detail: PropTypes.string}).isRequired
};

/**
 * Parts of the sign-in page that a tag of the page shows besides its content
 * @param {string} style Classes of the tag
 * @returns {{className?: string, attributes?: object, before?: React.ReactNode, after?: React.ReactNode}|null} Parts,
 * or null when none
 */
export function useSigninPart(style) {
  const signin = useContext(SigninContext);
  if (!signin) {
    return null;
  }
  if (hasAnyClass(style, COLUMNS)) {
    return {className: "signin-container"};
  }
  if (hasAnyClass(style, BRANDS)) {
    return signin.logo || !hasClass(style, "signin-info") ? {
      className: classNames("signin-info", {"signin-info--logo": signin.logo}),
      after: signin.logo && <img className="signin-logo" src={signin.logo} alt=""/>
    } : null;
  }
  if (hasAnyClass(style, FORMS)) {
    // The form is busy while its submit runs; the alert of a failed login hides when the user edits a field
    return {
      className: "signin-form",
      attributes: {"aria-busy": String(signin.busy), onInput: signin.alert ? signin.hideAlert : undefined},
      before: (signin.title || signin.subtitle || signin.alert) && <>
        {signin.title && <h2 className="signin-title">{signin.title}</h2>}
        {signin.subtitle && <p className="signin-subtitle">{signin.subtitle}</p>}
        {signin.alert && <SigninAlert alert={signin.alert}/>}
      </>
    };
  }
  return null;
}

/**
 * Check whether the submit of a login page runs (the login on the sign-in page): its fields are disabled meanwhile
 * @returns {boolean} The submit runs
 */
export function useSigningIn() {
  return Boolean(useContext(SigninContext)?.busy);
}

/**
 * Look of the loading state of a submit button: the spinner instead of its icon, disabled
 * @param {boolean} loading The button is loading
 * @returns {{className: string, disabled: boolean, icon?: React.ReactNode}} Look of the button
 */
const submitLook = loading => ({
  className: classNames("signin-submit", {"is-loading": loading}),
  disabled: loading,
  icon: loading ? getIconNode("lucide:loader-circle", {
    className: "p-button-icon p-button-icon-left awe-icon-spin signin-spin", "aria-hidden": "true"
  }) : undefined
});

/**
 * Look of a button of the sign-in page as the prototype, after the server action it launches: the Login button shows
 * the loading state while the login runs and the "or continue with" divider after it when a single sign-on button is
 * visible; the Microsoft Entra ID button is the official "Sign in with Microsoft" button; the SSO button has a key. On
 * the other login pages the submit button shows the loading state (with its own label) while its server action runs
 * and the other buttons are neutral outlined, as the SSO button
 * @param {object[]} actions Actions of the button
 * @param {object} attributes Attributes of the button
 * @param {function} t Translate function
 * @param {string} id Id of the button
 * @returns {{className?: string, variant?: string, icon?: React.ReactNode, defaultIcon?: string, label?: string,
 * disabled?: boolean, after?: React.ReactNode}|null} Look of the button (variant replaces the class of its button type,
 * defaultIcon is the icon value when the button has none), or null outside the sign-in page
 */
export function useSigninButton(actions, attributes, t, id) {
  const signin = useContext(SigninContext);
  if (!signin) {
    return null;
  }
  if (!signin.signIn) {
    return attributes?.buttonType === ButtonTypes.BUTTON_SUBMIT
      ? submitLook(signin.busyButton === id)
      : {className: "signin-secondary", variant: ""};
  }
  if (launches(actions, LOGIN_ACTION)) {
    const loading = signin.signingIn;
    return {
      ...submitLook(loading),
      label: loading ? t("SCREEN_TEXT_SIGNING_IN") : undefined,
      after: attributes?.visible && signin.ssoVisible
        && <p className="signin-divider"><span>{t("SCREEN_TEXT_LOGIN_OR")}</span></p>
    };
  }
  if (launches(actions, MICROSOFT_ACTION)) {
    return {
      // An official button with its own colours, not an outlined one
      className: "signin-microsoft",
      variant: "",
      icon: <span className="signin-microsoft-logo" aria-hidden="true"/>,
      label: t("SCREEN_TEXT_SIGN_IN_MICROSOFT")
    };
  }
  if (launches(actions, SSO_ACTION)) {
    // Neutral outlined as the Microsoft button (the theme border and text, not the primary colour)
    return {className: "signin-sso", variant: "", defaultIcon: "lucide:key-round"};
  }
  return null;
}

/**
 * Check whether a component is drawn inside the sign-in page (the password criterion adds the show / hide button and
 * the Caps Lock warning of the prototype there only)
 * @returns {boolean} The component is inside the sign-in page
 */
export function useInSigninPage() {
  return useContext(SigninContext) !== null;
}

/**
 * Alert of a failed login on the sign-in page: the password is cleared and focused when a new one shows
 * @returns {object|undefined} Alert (its id changes with each failed login), or undefined when none is shown
 */
export function useSigninAlert() {
  return useContext(SigninContext)?.alert;
}

const MODE_OPTIONS = [
  {mode: "light", icon: "lucide:sun", label: "ENUM_THEME_MODE_LIGHT"},
  {mode: "dark", icon: "lucide:moon", label: "ENUM_THEME_MODE_DARK"},
  {mode: "system", icon: "lucide:monitor", label: "ENUM_THEME_MODE_SYSTEM"}
];

/**
 * Language selector of the footer: the languages of the application in the AWE select. A change is the change of
 * language of the application (the language of the settings, which the view turns into the language of the texts)
 */
function SigninLanguage({languages}) {
  const dispatch = useDispatch();
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
    onChange={event => dispatch(updateSettings({language: event.value}))}
    valueTemplate={valueTemplate}
    ariaLabel={t("PARAMETER_LANGUAGE")}
    aria-label={t("PARAMETER_LANGUAGE")}
  />;
}

SigninLanguage.propTypes = {
  languages: PropTypes.arrayOf(PropTypes.string).isRequired
};

/**
 * Colour mode selector of the footer: light, dark or system as a segmented radio group. A change is the theme mode of
 * the settings (drawn at once on the page by the theme appearance of the application), kept for the next visit
 */
function SigninMode() {
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

// Canvas effect of awe.application.login.background-effect (the application registers it in its own bundle)

/**
 * Canvas effect of the login pages (S23): the canvas the registered effect draws on, placed where the background image
 * goes (signin.css places it as the panel of split and the backdrop of card). Decorative: hidden from assistive
 * technology. The wrapper is inserted only while the effect runs, so an unknown name leaves the image of the panel
 * uncovered, and the effect is started when it is registered (now or later). The effect stops when the page goes away;
 * an effect that throws is reported and leaves the page and its image as they are
 * @param {object} props Component properties
 * @param {string} props.name Name of the effect (awe.application.login.background-effect)
 * @param {Function} props.onRunningChange Called with whether the effect is drawing
 */
function SigninEffect({name, onRunningChange}) {
  const canvas = useRef(null);
  const [effect, setEffect] = useState(() => get(name));
  const [failed, setFailed] = useState(false);

  // An effect registered after the page is drawn starts when it registers (the effect is a function: keep it as it is)
  useEffect(() => subscribe(name, registered => {
    setFailed(false);
    setEffect(() => registered);
  }), [name]);

  useEffect(() => {
    if (!effect || failed || !canvas.current) {
      onRunningChange(false);
      return undefined;
    }
    let stop;
    try {
      stop = effect(canvas.current);
    } catch (error) {
      console.error(`The login background effect "${name}" failed`, error);
      setFailed(true);
      onRunningChange(false);
      return undefined;
    }
    onRunningChange(true);
    return () => {
      if (typeof stop === "function") {
        stop();
      }
      onRunningChange(false);
    };
  }, [effect, failed, name, onRunningChange]);

  if (!effect || failed) {
    return null;
  }
  return <div className="signin-effect" aria-hidden="true"><canvas ref={canvas}/></div>;
}

SigninEffect.propTypes = {
  name: PropTypes.string.isRequired,
  onRunningChange: PropTypes.func.isRequired
};

/**
 * Caption of the image panel of a login page: its title and its one line
 * @param {object} props Component properties
 * @param {string} [props.title] Title of the caption
 * @param {string} [props.text] Text of the caption
 * @returns {React.ReactNode} Caption, null when it has nothing to show
 */
function SigninCaption({title, text}) {
  if (!title && !text) {
    return null;
  }
  return <div className="signin-caption">
    {title && <p className="signin-caption-title">{title}</p>}
    {text && <p className="signin-caption-text">{text}</p>}
  </div>;
}

SigninCaption.propTypes = {
  title: PropTypes.string,
  text: PropTypes.string
};

/**
 * Footer of a login page: the footer text over "Powered by AWE 5" on the left, the language and colour mode selectors
 * on the right (the language one only when there are languages to choose from)
 * @param {object} props Component properties
 * @param {string} [props.footerText] Footer text
 * @param {boolean} props.poweredBy Show "Powered by AWE 5"
 * @param {boolean} props.languageSelector Show the language selector
 * @param {boolean} props.modeSelector Show the colour mode selector
 * @param {string[]} props.languages Languages of the language selector
 * @returns {React.ReactNode} Footer, null when it has nothing to show
 */
function SigninFooter({footerText, poweredBy, languageSelector, modeSelector, languages}) {
  const {t} = useTranslation();
  const legal = Boolean(footerText) || poweredBy;
  const prefs = languageSelector || modeSelector;
  if (!legal && !prefs) {
    return null;
  }
  return <footer className="signin-footer">
    {legal && <div className="signin-legal">
      {footerText && <span className="signin-footer-text">{footerText}</span>}
      {poweredBy && <span className="signin-powered">
        <span className="signin-powered-mark" aria-hidden="true"/>{t("SCREEN_TEXT_POWERED_BY")} <b>AWE</b> 5
      </span>}
    </div>}
    {prefs && <div className="signin-prefs">
      {languageSelector && <SigninLanguage languages={languages}/>}
      {modeSelector && <SigninMode/>}
    </div>}
  </footer>;
}

SigninFooter.propTypes = {
  footerText: PropTypes.string,
  poweredBy: PropTypes.bool.isRequired,
  languageSelector: PropTypes.bool.isRequired,
  modeSelector: PropTypes.bool.isRequired,
  languages: PropTypes.arrayOf(PropTypes.string).isRequired
};

/**
 * Frame of a login page: the page root with the layout, the background image and the canvas effect, the caption of the
 * image panel and the footer, around the markup of the screen XML
 * @param {object} props Component properties
 * @param {string} props.id Id of the page
 * @param {string} [props.className] Classes of the page
 * @param {string} props.layout Layout of the page
 * @param {string} [props.background] Background image
 * @param {string} [props.effect] Canvas effect of the image panel
 * @param {string} [props.panelTitle] Title of the caption of the image panel
 * @param {string} [props.panelSubtitle] Text of the caption of the image panel
 * @param {string} [props.footerText] Footer text
 * @param {boolean} props.poweredBy Show "Powered by AWE 5"
 * @param {boolean} props.languageSelector Show the language selector
 * @param {boolean} props.modeSelector Show the colour mode selector
 * @param {string[]} props.languages Languages of the language selector
 * @param {React.ReactNode} [props.children] Markup of the page
 * @returns {React.ReactNode} Page
 */
function SigninFrame({id, className, layout, background, effect, panelTitle, panelSubtitle, footerText, poweredBy,
  languageSelector, modeSelector, languages, children}) {
  const [effectRunning, setEffectRunning] = useState(false);

  return (
    <div id={id} className={classNames(className, "signin-page")} data-awe-login-layout={layout}
         data-awe-login-effect={effect} data-awe-login-effect-running={effectRunning ? "true" : undefined}
         style={background ? {"--awe-login-image": cssUrl(background)} : undefined}>
      {children}
      {effect && <SigninEffect name={effect} onRunningChange={setEffectRunning}/>}
      <SigninCaption title={panelTitle} text={panelSubtitle}/>
      <SigninFooter footerText={footerText} poweredBy={poweredBy} languageSelector={languageSelector}
                    modeSelector={modeSelector} languages={languages}/>
    </div>
  );
}

SigninFrame.propTypes = {
  id: PropTypes.string.isRequired,
  className: PropTypes.string,
  layout: PropTypes.string.isRequired,
  background: PropTypes.string,
  effect: PropTypes.string,
  panelTitle: PropTypes.string,
  panelSubtitle: PropTypes.string,
  footerText: PropTypes.string,
  poweredBy: PropTypes.bool.isRequired,
  languageSelector: PropTypes.bool.isRequired,
  modeSelector: PropTypes.bool.isRequired,
  languages: PropTypes.arrayOf(PropTypes.string).isRequired,
  children: PropTypes.node
};

function SigninPage({id, className, children}) {
  // The sign-in page, or another login page with its frame
  const signIn = id === SIGNIN_PAGE_ID;
  const login = useSelector(state => state.settings.login) || DEFAULT_LOGIN_SETTINGS;
  const {t, i18n} = useTranslation();
  const dark = useDarkMode();
  const {layout, title, subtitle, panelTitle, panelSubtitle, footerText} =
    loginTexts(login, signIn, value => loginText(value, t, key => i18n.exists(key)));
  const background = resolveImageUrl(login.background);
  const logo = resolveImageUrl(dark ? login.logoDark || login.logo : login.logo);
  const effect = layout === "form-only" ? undefined : loginEffectName(login.backgroundEffect);
  const poweredBy = login.poweredBy !== false;
  const {languages, languageSelector, modeSelector} = loginSelectors(login);

  const dispatch = useDispatch();
  const signingIn = useSelector(signIn ? selectSigningIn : noSelection) ?? false;
  const busyButton = useSelector(signIn ? noSelection : selectSubmitRunning);
  const busy = signIn ? signingIn : Boolean(busyButton);
  const ssoVisible = useSelector(signIn ? selectSsoVisible : noSelection) ?? false;
  const alert = useSelector(signIn ? selectAlert : noSelection);
  const hideAlert = useCallback(() => dispatch(removeSigninAlert()), [dispatch]);
  const context = useMemo(() => ({signIn, logo, title, subtitle, busy, busyButton, signingIn, ssoVisible, alert, hideAlert}),
    [signIn, logo, title, subtitle, busy, busyButton, signingIn, ssoVisible, alert, hideAlert]);

  // A new login hides the alert of the previous one, and it does not outlive the sign-in page
  useEffect(() => {
    if (signingIn) {
      hideAlert();
    }
  }, [signingIn, hideAlert]);
  useEffect(() => signIn ? hideAlert : undefined, [signIn, hideAlert]);

  // The mode chosen before on the sign-in page is drawn again on the next visit (once: a later change is a choice)
  const themeMode = useSelector(state => state.settings.themeMode);
  useEffect(() => {
    const stored = modeSelector ? readLoginMode() : undefined;
    if (stored && stored !== themeMode) {
      dispatch(updateSettings({themeMode: stored}));
    }
  }, [modeSelector]);

  return (
    <SigninContext.Provider value={context}>
      <SigninFrame id={id} className={className} layout={layout} background={background} effect={effect}
                   panelTitle={panelTitle} panelSubtitle={panelSubtitle} footerText={footerText}
                   poweredBy={poweredBy} languageSelector={languageSelector} modeSelector={modeSelector}
                   languages={languages}>
        {children}
      </SigninFrame>
    </SigninContext.Provider>
  );
}

SigninPage.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
  id: PropTypes.string,
};

export default SigninPage;
