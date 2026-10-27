import type {LoginLayout} from "../redux/actions/settings";

/**
 * Sign-in page branding (awe.application.login.*): the helpers that turn settings.login into the page.
 */

/** Id of the sign-in page in the sign-in screen XML */
export const SIGNIN_PAGE_ID = "page-signin";

// Login pages with their own id: the sign-in page and the expired password page of the React engine
const LOGIN_PAGE_IDS = [SIGNIN_PAGE_ID, "page-change-expired-password"];
// Image panels of the login pages without an id, shared with the AngularJS engine: the change password screens
// (change-password.xml, ChdPwdPub.xml) and the two-factor screens (check-2fa.xml, activate-2fa.xml)
const LOGIN_PANEL_IDS = ["page-change-password-bg", "page-totp-bg"];

/**
 * Check whether an element of a screen is a login page, which takes the frame of the sign-in page (S19): the page with
 * the id of a login page, or the page that holds the image panel of a login page
 * @param id Id of the element
 * @param elementList Children of the element
 * @return The element is a login page
 */
export function isLoginPage(id?: string, elementList?: {id?: unknown}[]): boolean {
  return LOGIN_PAGE_IDS.includes(id as string)
    || Boolean(elementList?.some(child => LOGIN_PANEL_IDS.includes(child?.id as string)));
}

/** Server action of the Login button of the sign-in screen XML */
export const LOGIN_ACTION = "login";

/**
 * Check whether the actions of a component launch a server action
 * @param actions Actions of the component
 * @param serverAction Server action
 * @return The actions launch the server action
 */
export function launchesServerAction(actions: unknown, serverAction: string): boolean {
  return Array.isArray(actions) && actions.some(action => action?.parameters?.serverAction === serverAction);
}

/**
 * Check whether an element is drawn inside the sign-in page
 * @param element Element
 * @return The element is inside the sign-in page
 */
export function inSigninPage(element: Element | null | undefined): boolean {
  return Boolean(element?.closest(`#${SIGNIN_PAGE_ID}`));
}

const LAYOUTS: LoginLayout[] = ["split", "form-only", "card"];
const DARK_QUERY = "(prefers-color-scheme: dark)";

/** Colour modes of the mode selector of the sign-in page */
export const LOGIN_MODES = ["light", "dark", "system"] as const;
export type LoginMode = typeof LOGIN_MODES[number];

/**
 * Key of the local storage that keeps the colour mode chosen on the sign-in page: before the login there is no user to
 * store it for (the mode of the user is stored by the server once signed in)
 */
export const LOGIN_MODE_KEY = "awe-login-mode";

/**
 * Colour mode of the theme mode of the settings: light or dark, else the system mode
 * @param themeMode Theme mode of the settings
 * @return Colour mode
 */
export function loginMode(themeMode?: string | null): LoginMode {
  return themeMode === "light" || themeMode === "dark" ? themeMode : "system";
}

/**
 * Colour mode chosen before on the sign-in page
 * @return Colour mode, or undefined when there is none (or the storage is not available)
 */
export function readLoginMode(): LoginMode | undefined {
  try {
    const mode = window.localStorage.getItem(LOGIN_MODE_KEY);
    return LOGIN_MODES.find(value => value === mode);
  } catch {
    return undefined;
  }
}

/**
 * Keep the colour mode chosen on the sign-in page for the next visit
 * @param mode Colour mode
 */
export function storeLoginMode(mode: LoginMode): void {
  try {
    window.localStorage.setItem(LOGIN_MODE_KEY, mode);
  } catch {
    // The storage is not available (private mode, disabled): the choice lasts until the page is left
  }
}

/**
 * Layout of the sign-in page: an unknown value is the split layout
 * @param layout Configured layout
 * @return Layout
 */
export function loginLayout(layout?: string): LoginLayout {
  return LAYOUTS.find(value => value === layout) ?? "split";
}

/**
 * Name of the canvas effect of the image panel (awe.application.login.background-effect): the name of an effect the
 * application registered. A blank value means no effect
 * @param effect Configured effect name
 * @return Trimmed name, or undefined when no effect is selected
 */
export function loginEffectName(effect?: string): string | undefined {
  const name = effect?.trim();
  return name ? name : undefined;
}

/**
 * Resolve an image against the base URI of the document. A CSS custom property resolves url() where it is used (the
 * stylesheet), so a relative image must be absolute before it is set
 * @param url Image URL (relative, absolute or data URI)
 * @param base Base URI
 * @return Absolute URL, or undefined when there is no image
 */
export function resolveImageUrl(url?: string, base: string = document.baseURI): string | undefined {
  if (!url?.trim()) {
    return undefined;
  }
  try {
    return new URL(url.trim(), base).href;
  } catch {
    return undefined;
  }
}

/**
 * CSS url() of an absolute URL
 * @param url Absolute URL
 * @return CSS url() value
 */
export function cssUrl(url: string): string {
  return `url("${url.replace(/["\\]/g, "\\$&").replace(/\n/g, "\\a ")}")`;
}

/**
 * Text of the sign-in page: a locale key is translated, any other value shows as it is
 * @param value Configured text
 * @param t Translate function
 * @param exists Check whether a locale key exists
 * @return Text, or undefined when there is no text
 */
export function loginText(
  value: string | undefined,
  t: (key: string) => string,
  exists: (key: string) => boolean
): string | undefined {
  if (!value?.trim()) {
    return undefined;
  }
  return exists(value) ? t(value) : value;
}

/**
 * Check whether the colour mode is dark: the mode class of the theme (light or dark), else the system preference
 * @param root Element with the mode class (<html>)
 * @return The colour mode is dark
 */
export function isDarkMode(root: HTMLElement = document.documentElement): boolean {
  if (root.classList.contains("dark")) {
    return true;
  }
  if (root.classList.contains("light") || typeof window.matchMedia !== "function") {
    return false;
  }
  return Boolean(window.matchMedia(DARK_QUERY)?.matches);
}

/**
 * Call back when the system colour preference changes
 * @param onChange Callback
 * @return Stop listening
 */
export function observeSystemMode(onChange: () => void): () => void {
  const query = typeof window.matchMedia === "function" ? window.matchMedia(DARK_QUERY) : null;
  query?.addEventListener?.("change", onChange);
  return () => query?.removeEventListener?.("change", onChange);
}
