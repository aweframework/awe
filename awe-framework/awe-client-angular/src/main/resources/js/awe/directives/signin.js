import {aweApplication} from "../awe";
import {observeModeClasses} from "awe-client-react/utilities/themeAppearance";
import {subscribe} from "awe-client-react/utilities/loginEffects";
import {iconMarkup} from "../services/icon";
import {TestAttributes, TestIds} from "../data/testIds";

// Layouts of the sign-in page (awe.application.login.layout); the first one is the default
const LAYOUTS = ["split", "form-only", "card"];

// Type of an alert as the toast names it (controllers/message.js getMessageType), for the alert of the sign-in page
const ALERT_TYPES = {error: "danger", wrong: "danger", ok: "success"};

// settings.login of a server that does not send it: the framework defaults (awe.application.login.*)
const DEFAULT_LOGIN = {
  layout: "split",
  title: "SCREEN_TEXT_LOGIN_TITLE",
  subtitle: "SCREEN_TEXT_LOGIN_SUBTITLE",
  panelTitle: "SCREEN_TEXT_LOGIN_PANEL_TITLE",
  panelSubtitle: "SCREEN_TEXT_LOGIN_PANEL_SUBTITLE",
  poweredBy: true,
  languageSelector: true,
  modeSelector: true
};

const DARK_QUERY = "(prefers-color-scheme: dark)";

// Colour modes of the mode selector, with their icons. Before the sign-in there is no user to store the mode on, so
// the choice is kept in the local storage of the browser
const MODES = {light: "lucide:sun", dark: "lucide:moon", system: "lucide:monitor"};
const MODE_KEY = "awe-login-mode";

// Icon of the failed login alert (the danger message of the prototype)
const ALERT_ICON = "lucide:circle-x";

// The login pages, by the class of their form column (the directive name): the id of their image panel, the classes of
// their brand and form, and their submit button. The sign-in page also takes the title, the subtitle, the failed login
// alert and the "or continue with" divider; the change password and two-factor screens keep their own titles
const PAGES = {
  aweSignin: {panel: "page-signin-bg", brand: "signin-info", form: "signin-form", submit: "#ButLogIn", signIn: true},
  aweChangePassword: {panel: "page-change-password-bg", brand: "change-password-header", form: "change-password-form",
    submit: ".form-actions button[type=\"submit\"]"},
  aweTotp: {panel: "page-totp-bg", brand: "totp-header", form: "totp-form",
    submit: ".form-actions button[type=\"submit\"]"}
};

/**
 * Resolve an image URL against the base URI of the page (a CSS custom property resolves url() where it is used)
 * @param {string} url Image URL
 * @returns {string} Absolute URL
 */
const absoluteUrl = url => new URL(url, document.baseURI).href;

/**
 * Create an element with a class
 * @param {string} tag Tag name
 * @param {string} className Class
 * @returns {HTMLElement} Element
 */
const create = (tag, className) => Object.assign(document.createElement(tag), {className});

/**
 * Colour mode the user chose: the light or dark class of <html>, the system one without them
 * @param {HTMLElement} root <html>
 * @returns {string} light, dark or system
 */
const currentMode = root => ["dark", "light"].find(mode => root.classList.contains(mode)) || "system";

/**
 * Mode kept in the local storage of the browser
 * @returns {string|null} Mode, null when there is none or the storage is not available
 */
const storedMode = () => {
  try {
    const mode = window.localStorage.getItem(MODE_KEY);
    return Object.keys(MODES).includes(mode) ? mode : null;
  } catch {
    return null;
  }
};

/**
 * Keep a mode in the local storage of the browser (nothing when the storage is not available)
 * @param {string} mode Mode
 */
const storeMode = mode => {
  try {
    window.localStorage.setItem(MODE_KEY, mode);
  } catch {
    // The mode applies to this page only
  }
};

/**
 * Callback that does nothing: a selector that is not shown, or nothing to hide
 * @returns {undefined} Nothing
 */
const nothing = () => undefined;

/**
 * Call the callbacks of a colour mode change
 * @param {Array} modeListeners Callbacks
 */
const notifyModeChange = modeListeners => modeListeners.forEach(listener => listener());

/**
 * Observe the colour mode classes of <html> while there are callbacks to call when they change
 * @param {HTMLElement} root <html>
 * @param {Array} modeListeners Callbacks when the colour mode changes
 * @returns {function} Stop observing, or nothing when there is nothing to follow
 */
const observeMode = (root, modeListeners) => modeListeners.length
  ? observeModeClasses(root, ["dark", "light"], () => notifyModeChange(modeListeners))
  : nothing;

/**
 * Translator of the texts of a login page: the elements whose text or attribute is translated, to translate them again
 * when the language changes
 * @param {object} $translate Translate service
 * @returns {{translated: function, translate: function}} Translator
 */
const createTranslator = $translate => {
  // Texts and attributes to translate again when the language changes
  const texts = [];
  const translateText = ({target, value, attribute}) => {
    const text = $translate.instant(typeof value === "function" ? value() : value);
    if (attribute) {
      target.setAttribute(attribute, text);
    } else {
      target.textContent = text;
    }
  };
  const translate = () => texts.forEach(translateText);

  /**
   * Translate the text or an attribute of an element, now and when the language changes
   * @param {HTMLElement} target Element
   * @param {string|function} value Locale key or literal, or a function that returns the current one
   * @param {string} [attribute] Attribute to translate (the text of the element by default)
   * @returns {HTMLElement} The element
   */
  const translated = (target, value, attribute) => {
    const text = {target, value, attribute};
    texts.push(text);
    translateText(text);
    return target;
  };
  return {translated, translate};
};

/**
 * Layout of a login page: the configured one, or the default one for a value that is not a layout
 * @param {object} login settings.login
 * @returns {string} Layout
 */
const resolveLayout = login => LAYOUTS.includes(login.layout) ? login.layout : LAYOUTS[0];

/**
 * Layout and background image of a login page
 * @param {HTMLElement} page Page root
 * @param {object} login settings.login
 * @param {string} layout Layout
 */
const applyLayout = (page, login, layout) => {
  page.setAttribute("data-awe-login-layout", layout);
  if (login.background) {
    page.style.setProperty("--awe-login-image", `url("${absoluteUrl(login.background).replace(/"/g, "%22")}")`);
  }
};

/**
 * Canvas effect of the image panel (S23): the effect the application registered under
 * awe.application.login.background-effect, inside the panel (the caption is drawn over it) instead of the image.
 * Decorative. AWE ships no effect: an unknown name draws nothing and leaves the image (the wrapper is inserted only
 * while the effect runs), an effect registered later starts when it registers, an effect that throws is reported and
 * leaves the page as it is, and the effect stops with the page
 * @param {object} args Arguments
 * @param {HTMLElement} args.page Page root
 * @param {HTMLElement} args.panel Image panel
 * @param {object} args.login settings.login
 * @param {string} args.layout Layout
 * @param {object} args.scope Scope of the page
 * @param {object} args.$log Log service
 */
const applyBackgroundEffect = ({page, panel, login, layout, scope, $log}) => {
  const effectName = typeof login.backgroundEffect === "string" ? login.backgroundEffect.trim() : "";
  if (!effectName || layout === "form-only") {
    return;
  }
  page.setAttribute("data-awe-login-effect", effectName);
  let wrapper = null;
  let stopEffect = null;
  const stop = () => {
    if (typeof stopEffect === "function") {
      stopEffect();
    }
    stopEffect = null;
    wrapper?.remove();
    wrapper = null;
    page.removeAttribute("data-awe-login-effect-running");
  };
  const start = effect => {
    stop();
    wrapper = create("div", "signin-effect");
    wrapper.setAttribute("aria-hidden", "true");
    const canvas = wrapper.appendChild(document.createElement("canvas"));
    panel.prepend(wrapper);
    try {
      stopEffect = effect(canvas);
    } catch (error) {
      $log.error(`The login background effect "${effectName}" failed`, error);
      stop();
      return;
    }
    page.setAttribute("data-awe-login-effect-running", "true");
  };
  const unsubscribe = subscribe(effectName, start);
  scope.$on("$destroy", () => {
    unsubscribe();
    stop();
  });
};

/**
 * Title and subtitle of the sign-in form (the other login pages keep their own title)
 * @param {object} args Arguments
 * @param {HTMLElement} args.form Form of the page
 * @param {boolean} args.signIn The page is the sign-in page
 * @param {object} args.login settings.login
 * @param {function} args.translated Translate the text or an attribute of an element
 * @returns {HTMLElement|null} Title of the form, null when it has none
 */
const applyTitle = ({form, signIn, login, translated}) => {
  let title = signIn ? form?.querySelector(":scope > .signin-text") : null;
  if (title && login.title) {
    title.replaceChildren(translated(document.createElement("span"), login.title));
  } else {
    title?.remove();
    title = null;
  }
  if (signIn && form && login.subtitle) {
    const subtitle = translated(create("p", "signin-subtitle"), login.subtitle);
    form.insertBefore(subtitle, title ? title.nextSibling : form.firstChild);
  }
  return title;
};

/**
 * Caption of the image panel: its title and its one line
 * @param {object} args Arguments
 * @param {HTMLElement} args.panel Image panel
 * @param {object} args.login settings.login
 * @param {function} args.translated Translate the text or an attribute of an element
 */
const applyCaption = ({panel, login, translated}) => {
  if (login.panelTitle || login.panelSubtitle) {
    const caption = create("div", "signin-caption");
    if (login.panelTitle) {
      caption.appendChild(translated(create("p", "signin-caption-title"), login.panelTitle));
    }
    if (login.panelSubtitle) {
      caption.appendChild(translated(create("p", "signin-caption-text"), login.panelSubtitle));
    }
    panel.appendChild(caption);
  }
};

/**
 * Language selector: a select-only combobox with the languages of settings.login in a listbox (no native select
 * popup). Choosing a language changes it as the language selector of the application does
 * @param {object} args Arguments
 * @param {number} args.scopeId Scope id (unique ids of the page)
 * @param {string[]} args.languages Language codes
 * @param {object} args.$settings Settings service
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {function} args.translate Translate the texts of the page again
 * @returns {{selector: HTMLElement, sync: function}} Selector, and the sync of the language it shows
 */
const createLanguageSelector = ({scopeId, languages, $settings, translated, translate}) => {
  const listId = `signin-languages-${scopeId}`;
  let current = $settings.getLanguage() || languages[0];
  let active = -1;
  const selector = create("div", "signin-language");
  const trigger = translated(create("div", "signin-language-trigger"), "PARAMETER_LANGUAGE", "aria-label");
  Object.entries({"role": "combobox", "tabindex": "0", "aria-haspopup": "listbox", "aria-expanded": "false",
    "aria-controls": listId}).forEach(([name, value]) => trigger.setAttribute(name, value));
  trigger.innerHTML = iconMarkup("globe");
  trigger.appendChild(translated(create("span", "signin-language-value"), () => `ENUM_LAN_${current.toUpperCase()}`));
  trigger.insertAdjacentHTML("beforeend", iconMarkup("chevron-down"));

  const list = translated(Object.assign(create("ul", "signin-language-list"), {id: listId, hidden: true}),
    "PARAMETER_LANGUAGE", "aria-label");
  list.setAttribute("role", "listbox");
  const options = languages.map((language, index) => {
    const option = translated(Object.assign(create("li", "signin-language-option"), {id: `${listId}-${index}`}),
      `ENUM_LAN_${language.toUpperCase()}`);
    option.setAttribute("role", "option");
    // The focus stays on the combobox
    option.addEventListener("mousedown", event => event.preventDefault());
    option.addEventListener("click", () => {
      choose(index);
      trigger.focus();
    });
    return option;
  });
  list.append(...options);

  const showSelected = () => options.forEach((option, index) =>
    option.setAttribute("aria-selected", String(languages[index] === current)));
  const activate = index => {
    active = Math.max(0, Math.min(index, options.length - 1));
    options.forEach((option, optionIndex) => option.classList.toggle("is-active", optionIndex === active));
    trigger.setAttribute("aria-activedescendant", options[active].id);
  };
  const open = (index = Math.max(0, languages.indexOf(current))) => {
    list.hidden = false;
    selector.classList.add("is-open");
    trigger.setAttribute("aria-expanded", "true");
    activate(index);
  };
  const close = () => {
    list.hidden = true;
    selector.classList.remove("is-open");
    trigger.setAttribute("aria-expanded", "false");
    trigger.removeAttribute("aria-activedescendant");
    options.forEach(option => option.classList.remove("is-active"));
  };
  const choose = index => {
    close();
    if (languages[index] !== current) {
      current = languages[index];
      showSelected();
      translate();
      $settings.changeLanguage(current);
    }
  };

  trigger.addEventListener("click", () => (list.hidden ? open() : close()));
  trigger.addEventListener("keydown", event => {
    const last = options.length - 1;
    if (event.key === "Tab") {
      if (!list.hidden) {
        choose(active);
      }
      return;
    }
    const keys = list.hidden ? {
      "ArrowDown": () => open(), "ArrowUp": () => open(), "Enter": () => open(), " ": () => open(),
      "Home": () => open(0), "End": () => open(last)
    } : {
      "ArrowDown": () => activate(active + 1), "ArrowUp": () => activate(active - 1),
      "Home": () => activate(0), "End": () => activate(last),
      "Enter": () => choose(active), " ": () => choose(active), "Escape": close
    };
    if (event.key in keys) {
      event.preventDefault();
      keys[event.key]();
    }
  });
  selector.addEventListener("focusout", event => {
    if (!selector.contains(event.relatedTarget)) {
      close();
    }
  });

  // The language changed (here or elsewhere)
  const sync = () => {
    current = $settings.getLanguage() || current;
    showSelected();
  };
  showSelected();
  selector.append(trigger, list);
  return {selector, sync};
};

/**
 * Colour mode selector: light, dark and system as a segmented radio group. Choosing a mode renders it at once with the
 * theme appearance of the engine (the mode classes of <html>) and keeps it for the next visit
 * @param {object} args Arguments
 * @param {number} args.scopeId Scope id (unique ids of the page)
 * @param {HTMLElement} args.root <html>
 * @param {object} args.$settings Settings service
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {Array} args.modeListeners Callbacks when the colour mode changes
 * @returns {{group: HTMLElement, sync: function}} Selector, and the sync of the mode it shows
 */
const createModeSelector = ({scopeId, root, $settings, translated, modeListeners}) => {
  const group = translated(create("div", "signin-mode"), "PARAMETER_THEME_MODE", "aria-label");
  group.setAttribute("role", "radiogroup");
  const radios = Object.entries(MODES).map(([mode, icon]) => {
    const key = `ENUM_THEME_MODE_${mode.toUpperCase()}`;
    const label = translated(create("label", "signin-mode-option"), key, "title");
    const radio = Object.assign(document.createElement("input"), {type: "radio", name: `signin-mode-${scopeId}`,
      value: mode});
    const segment = create("span", "signin-mode-segment");
    segment.innerHTML = iconMarkup(icon);
    segment.appendChild(translated(create("span", "sr-only"), key));
    label.append(radio, segment);
    group.appendChild(label);
    return radio;
  });
  const sync = () => {
    const mode = currentMode(root);
    radios.forEach(radio => radio.checked = radio.value === mode);
  };
  const change = radio => {
    if (radio.checked) {
      $settings.update({themeMode: radio.value});
      storeMode(radio.value);
      sync();
    }
  };
  radios.forEach(radio => radio.addEventListener("change", () => change(radio)));

  // The mode chosen on a previous visit
  const mode = storedMode();
  if (mode && mode !== currentMode(root)) {
    $settings.update({themeMode: mode});
  }
  sync();
  modeListeners.push(sync);
  return {group, sync};
};

/**
 * Selectors of the footer: the language selector (only when there are languages to choose from) and the colour mode
 * selector, in a column of preferences
 * @param {object} args Arguments
 * @param {string[]} args.languages Languages of the language selector (none: no language selector)
 * @param {boolean} args.modeSelector Show the colour mode selector
 * @param {number} args.scopeId Scope id (unique ids of the page)
 * @param {HTMLElement} args.root <html>
 * @param {object} args.$settings Settings service
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {function} args.translate Translate the texts of the page again
 * @param {Array} args.modeListeners Callbacks when the colour mode changes
 * @returns {{prefs: HTMLElement|null, syncLanguage: function}} Column of preferences (null when there is no selector)
 * and the sync of the language shown by the language selector
 */
const createPreferences = ({languages, modeSelector, scopeId, root, $settings, translated, translate,
  modeListeners}) => {
  const language = languages.length
    ? createLanguageSelector({scopeId, languages, $settings, translated, translate})
    : null;
  const mode = modeSelector ? createModeSelector({scopeId, root, $settings, translated, modeListeners}) : null;
  if (!language && !mode) {
    return {prefs: null, syncLanguage: nothing};
  }
  const prefs = create("div", "signin-prefs");
  if (language) {
    prefs.appendChild(language.selector);
  }
  if (mode) {
    prefs.appendChild(mode.group);
  }
  return {prefs, syncLanguage: language ? language.sync : nothing};
};

/**
 * Texts of the left column of the footer: the footer text over "Powered by AWE 5"
 * @param {object} login settings.login
 * @param {function} translated Translate the text or an attribute of an element
 * @returns {HTMLElement} Column of texts
 */
const createLegalColumn = (login, translated) => {
  const legalColumn = create("div", "signin-legal");
  if (login.footerText) {
    legalColumn.appendChild(translated(create("span", "signin-footer-text"), login.footerText));
  }
  if (login.poweredBy) {
    const powered = create("span", "signin-powered");
    const awe = document.createElement("b");
    awe.textContent = "AWE";
    powered.append(translated(document.createElement("span"), "SCREEN_TEXT_POWERED_BY"), " ", awe, " 5");
    legalColumn.appendChild(powered);
  }
  return legalColumn;
};

/**
 * Languages of the language selector of a login page (a single language has nothing to choose from)
 * @param {object} login settings.login
 * @returns {string[]} Languages
 */
const languagesOf = login => login.languageSelector !== false && login.languages?.length > 1 ? login.languages : [];

/**
 * Footer of a login page: the footer text over "Powered by AWE 5" on the left, the language and colour mode selectors
 * on the right
 * @param {object} args Arguments
 * @param {HTMLElement} args.page Page root
 * @param {object} args.login settings.login
 * @param {number} args.scopeId Scope id (unique ids of the page)
 * @param {HTMLElement} args.root <html>
 * @param {object} args.$settings Settings service
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {function} args.translate Translate the texts of the page again
 * @param {Array} args.modeListeners Callbacks when the colour mode changes
 * @returns {function} Sync of the language shown by the language selector
 */
const applyFooter = ({page, login, scopeId, root, $settings, translated, translate, modeListeners}) => {
  const legal = Boolean(login.footerText || login.poweredBy);
  const languages = languagesOf(login);
  const modeSelector = login.modeSelector !== false;
  if (!legal && !languages.length && !modeSelector) {
    return nothing;
  }
  const footer = create("footer", "signin-footer");
  if (legal) {
    footer.appendChild(createLegalColumn(login, translated));
  }
  const {prefs, syncLanguage} = createPreferences({languages, modeSelector, scopeId, root, $settings, translated,
    translate, modeListeners});
  if (prefs) {
    footer.appendChild(prefs);
  }
  page.appendChild(footer);
  return syncLanguage;
};

/**
 * Password inputs of a login page: the eye button that shows or hides each one, and the Caps Lock warning under the
 * field while Caps Lock is on. The first one is the input a failed login clears and focuses
 * @param {object} args Arguments
 * @param {number} args.scopeId Scope id (unique ids of the page)
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {function} args.translate Translate the texts of the page again
 * @returns {{enhance: function, has: function, first: function}} Password inputs: enhance one, check whether one is
 * already enhanced, and the first one
 */
const createPasswordEnhancer = ({scopeId, translated, translate}) => {
  // First password input (the one a failed login clears and focuses) and the password inputs already enhanced
  let first = null;
  const inputs = new WeakSet();
  let count = 0;

  /**
   * Password: the eye button that shows or hides it, and the Caps Lock warning under the field while Caps Lock is on
   * @param {HTMLInputElement} input Password input
   */
  const enhance = input => {
    inputs.add(input);
    first = first || input;
    count++;
    let shown = false;
    const toggle = translated(create("button", "signin-password-toggle"),
      () => (shown ? "SCREEN_TEXT_HIDE_PASSWORD" : "SCREEN_TEXT_SHOW_PASSWORD"), "aria-label");
    toggle.type = "button";
    if (input.id) {
      toggle.setAttribute("aria-controls", input.id);
    }
    const showState = () => {
      toggle.setAttribute("aria-pressed", String(shown));
      toggle.innerHTML = iconMarkup(shown ? "eye-slash" : "eye");
    };
    toggle.addEventListener("click", () => {
      shown = !shown;
      input.type = shown ? "text" : "password";
      showState();
      translate();
    });
    showState();
    input.after(toggle);

    const capsId = `${input.id || `signin-password-${scopeId}-${count}`}-caps`;
    const caps = Object.assign(create("p", "signin-caps"), {id: capsId, hidden: true});
    caps.innerHTML = iconMarkup("exclamation-triangle");
    caps.appendChild(translated(document.createElement("span"), "SCREEN_TEXT_CAPS_LOCK_ON"));
    input.parentElement.after(caps);
    input.setAttribute("aria-describedby", [input.getAttribute("aria-describedby"), caps.id].filter(Boolean).join(" "));
    const checkCaps = event => {
      if (typeof event.getModifierState === "function") {
        caps.hidden = !event.getModifierState("CapsLock");
      }
    };
    input.addEventListener("keydown", checkCaps);
    input.addEventListener("keyup", checkCaps);
    input.addEventListener("blur", () => caps.hidden = true);
  };

  return {enhance, has: input => inputs.has(input), first: () => first};
};

/**
 * Password fields of a login page: the criteria may render their inputs after the page, so they are looked for on each
 * digest
 * @param {object} args Arguments
 * @param {object} args.scope Scope of the page
 * @param {HTMLElement} args.form Form of the page
 * @param {object} args.passwords Password enhancer
 */
const applyPasswords = ({scope, form, passwords}) => {
  if (!form) {
    return;
  }
  scope.$watch(() => [...form.querySelectorAll("input[type=\"password\"]")]
    .filter(input => !passwords.has(input)).length, pending => {
    if (pending) {
      form.querySelectorAll("input[type=\"password\"]")
        .forEach(input => passwords.has(input) || passwords.enhance(input));
    }
  });
};

/**
 * Failed login alert and "or continue with" divider of the sign-in page. The alert shows the message of the answer of
 * the login button (the warning or error of the login action) in a danger alert above the fields instead of the toast:
 * the password is cleared and focused, and the alert hides when the user edits a field or signs in again. If the answer
 * leaves the page with the alert shown (an expired password opens the change password screen) the message goes on as a
 * toast. The divider goes right after the login button (the stylesheet shows it only while an SSO button is visible),
 * and the buttons may also render after the page
 * @param {object} args Arguments
 * @param {object} args.scope Scope of the page
 * @param {HTMLElement} args.form Form of the page
 * @param {HTMLElement} args.title Title of the form (the alert goes under the subtitle, or under the title)
 * @param {object} args.passwords Password enhancer (the first password is the one to clear and focus)
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {object} args.$translate Translate service
 * @param {object} args.$actionController Action controller
 * @returns {function} Hides the alert
 */
const applyAlert = ({scope, form, title, passwords, translated, $translate, $actionController}) => {
  const alert = Object.assign(create("div", "signin-alert"), {hidden: true});
  alert.setAttribute("role", "alert");
  const alertAnchor = form.querySelector(":scope > .signin-subtitle") || title;
  form.insertBefore(alert, alertAnchor ? alertAnchor.nextSibling : form.firstChild);
  let failure = null;
  const hideAlert = () => {
    alert.hidden = true;
    failure = null;
  };
  const showAlert = message => {
    const content = document.createElement("span");
    if (message.title) {
      const strong = Object.assign(document.createElement("strong"), {textContent: $translate.instant(message.title)});
      strong.setAttribute(TestAttributes.testId, TestIds.alertTitle);
      content.append(strong, " ");
    }
    if (message.message) {
      const text = Object.assign(document.createElement("span"), {textContent: $translate.instant(message.message)});
      text.setAttribute(TestAttributes.testId, TestIds.alertMessage);
      content.appendChild(text);
    }
    // The alert carries the contract of the alert of the toast (the message actions draw no toast for it): its type is
    // named as the toast names it (controllers/message.js getMessageType)
    alert.setAttribute(TestAttributes.testId, TestIds.alert);
    alert.setAttribute(TestAttributes.type, ALERT_TYPES[message.type] || message.type);
    alert.innerHTML = iconMarkup(ALERT_ICON);
    alert.appendChild(content);
    alert.hidden = false;
    failure = message;
  };
  form.addEventListener("input", hideAlert);
  scope.$on("claim-message", (event, message) => {
    if (message.target?.component !== "ButLogIn" || !["warning", "error", "wrong"].includes(message.type)) {
      return;
    }
    event.preventDefault();
    // The change event updates the model of the field
    const passwordInput = passwords.first();
    if (passwordInput) {
      passwordInput.value = "";
      passwordInput.dispatchEvent(new Event("change"));
    }
    showAlert(message);
    passwordInput?.focus();
  });
  scope.$on("$destroy", () => {
    if (failure) {
      $actionController.sendMessage({view: failure.view || "base"}, failure.type, failure.title, failure.message);
    }
  });

  // "or continue with" divider right after the login button (the stylesheet shows it only while an SSO button is
  // visible). The buttons may also render after the page
  const stopButtonWatch = scope.$watch(() => form.querySelector(".form-actions #ButLogIn"), button => {
    if (button) {
      stopButtonWatch();
      const divider = create("p", "signin-divider");
      divider.appendChild(translated(document.createElement("span"), "SCREEN_TEXT_LOGIN_OR"));
      button.closest(".form-actions > *").after(divider);
    }
  });
  return hideAlert;
};

/**
 * Loading state of the submit button of a login page: while the server action of the button runs the engine marks the
 * button as loading, and the end-load action of the answer (an error too) ends it. Meanwhile the button shows a spinner
 * and "Signing in…" (the sign-in page) or its own label (the other login pages), and the button and the fields are
 * disabled
 * @param {object} args Arguments
 * @param {object} args.scope Scope of the page
 * @param {HTMLElement} args.form Form of the page
 * @param {object} args.config Login page (PAGES)
 * @param {function} args.translated Translate the text or an attribute of an element
 * @param {function} args.hideAlert Hides the failed login alert
 */
const applyBusyState = ({scope, form, config, translated, hideAlert}) => {
  if (!form) {
    return;
  }
  const busyContent = create("span", "signin-busy");
  busyContent.innerHTML = iconMarkup("spinner fa-spin");
  const busyText = busyContent.appendChild(document.createElement("span"));
  if (config.signIn) {
    translated(busyText, "SCREEN_TEXT_SIGNING_IN");
  }
  let disabledFields = null;
  let buttonDisabled = false;
  const showBusy = (button, busy) => {
    if (busy === Boolean(disabledFields)) {
      return;
    }
    button.classList.toggle("is-loading", busy);
    if (busy) {
      hideAlert();
      if (!config.signIn) {
        busyText.textContent = button.querySelector(".button-text")?.textContent.trim() || "";
      }
      buttonDisabled = button.disabled;
      button.disabled = true;
      button.appendChild(busyContent);
      disabledFields = [...form.querySelectorAll("input, .signin-password-toggle")].filter(field => !field.disabled);
      disabledFields.forEach(field => field.disabled = true);
      form.setAttribute("aria-busy", "true");
    } else {
      // The engine enables the button again when its actions end
      button.disabled = buttonDisabled;
      busyContent.remove();
      disabledFields.forEach(field => field.disabled = false);
      disabledFields = null;
      form.removeAttribute("aria-busy");
    }
  };
  scope.$on("controllerChange", (_event, {address, controller} = {}) => {
    const button = form.querySelector(config.submit);
    if (button?.id && address?.component === button.id && controller && "loading" in controller) {
      showBusy(button, Boolean(controller.loading));
    }
  });
};

/**
 * Logo of the brand: the dark one while the resolved colour mode is dark (the dark class of <html>, or the system
 * preference when neither the light nor the dark class is set)
 * @param {object} args Arguments
 * @param {HTMLElement} args.brand Brand column of the page
 * @param {object} args.login settings.login
 * @param {HTMLElement} args.root <html>
 * @param {object} args.darkQuery System colour preference, if the browser has it
 * @param {Array} args.modeListeners Callbacks when the colour mode changes
 * @param {object} args.scope Scope of the page
 */
const applyLogo = ({brand, login, root, darkQuery, modeListeners, scope}) => {
  if (!brand || (!login.logo && !login.logoDark)) {
    return;
  }
  const logo = Object.assign(create("img", "signin-logo"), {alt: ""});
  const updateLogo = () => {
    const dark = root.classList.contains("dark") || (!root.classList.contains("light") && !!darkQuery?.matches);
    const source = dark && login.logoDark ? login.logoDark : login.logo;
    if (source) {
      logo.setAttribute("src", absoluteUrl(source));
      brand.appendChild(logo);
    } else {
      logo.remove();
    }
  };
  updateLogo();
  modeListeners.push(updateLogo);
  darkQuery?.addEventListener("change", updateLogo);
  scope.$on("$destroy", () => darkQuery?.removeEventListener("change", updateLogo));
};

/**
 * Login page directive: applies settings.login (awe.application.login.*) to a login page, as the class of its form
 * column: awe-signin (signin.xml, signin-local.xml and the sign-in screens of the applications), awe-change-password
 * (change-password.xml, ChdPwdPub.xml, change-expired-password.xml) and awe-totp (check-2fa.xml, activate-2fa.xml). It
 * sets the layout and the background image on the page root and adds the logo, the caption of the panel, the footer
 * (with the language and colour mode selectors), the eye button and Caps Lock warning of the password fields and the
 * loading state of the submit button, and the canvas effect of the panel the application registered
 * (backgroundEffect). The sign-in page
 * also takes the title and the subtitle, the "or continue with" divider before the SSO buttons and the alert of a
 * failed login. The texts are locale keys or literals
 * @param {object} config Login page (PAGES)
 * @returns {Array} Directive definition, with its injections
 */
const loginPage = config => ['AweSettings', '$translate', '$rootScope', '$window', 'ActionController', '$log',
  function ($settings, $translate, $rootScope, $window, $actionController, $log) {
    return {
      restrict: 'C',
      link: function (scope, element) {
        const column = element[0];
        const page = column.parentElement;
        const panel = page?.querySelector(`:scope > #${config.panel}`);
        if (!panel) {
          return;
        }
        const login = $settings.get("login") || DEFAULT_LOGIN;
        const form = column.querySelector(`:scope > .${config.form}`);
        const brand = column.querySelector(`:scope > .${config.brand}`);
        const root = document.documentElement;
        const darkQuery = $window.matchMedia ? $window.matchMedia(DARK_QUERY) : null;
        // Callbacks when the colour mode changes
        const modeListeners = [];
        const {translated, translate} = createTranslator($translate);
        const passwords = createPasswordEnhancer({scopeId: scope.$id, translated, translate});

        // Layout, background image and canvas effect of the image panel
        const layout = resolveLayout(login);
        applyLayout(page, login, layout);
        applyBackgroundEffect({page, panel, login, layout, scope, $log});

        // Title, subtitle and caption of the page
        const title = applyTitle({form, signIn: config.signIn, login, translated});
        applyCaption({panel, login, translated});

        // Footer, with the language and colour mode selectors
        const syncLanguage = applyFooter({page, login, scopeId: scope.$id, root, $settings, translated, translate,
          modeListeners});

        // Password fields and the alert of a failed login (the sign-in page only)
        applyPasswords({scope, form, passwords});
        const hideAlert = config.signIn && form
          ? applyAlert({scope, form, title, passwords, translated, $translate, $actionController})
          : nothing;

        // Loading state of the submit button, and the logo of the brand
        applyBusyState({scope, form, config, translated, hideAlert});
        applyLogo({brand, login, root, darkQuery, modeListeners, scope});

        const stopModeObserver = observeMode(root, modeListeners);
        const stopTranslate = $rootScope.$on("$translateChangeSuccess", () => {
          syncLanguage();
          translate();
        });
        scope.$on("$destroy", () => {
          stopTranslate();
          stopModeObserver();
        });
      }
    };
  }];

Object.entries(PAGES).forEach(([name, config]) => aweApplication.directive(name, loginPage(config)));
