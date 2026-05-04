import {
  addEventListenerTimeout,
} from "../utilities";
import i18n from "../i18n/i18n";
import {acceptAction} from "../redux/actions/actions";
import {useLocation} from "react-router";
import {useDispatch, useSelector} from "react-redux";
import {
  backAction,
  changeLanguageAction,
  changeThemeAction, getFileAction, logoutAction, redirectAction,
  reloadScreenAction,
  screenAction
} from "../redux/thunks/screen";

/**
 * Screen service (Functional version)
 * @category Services
 */
const useScreenService = () => {

  const dispatch = useDispatch();
  const { settings } = useSelector((state) => ({settings: state.settings}));
  const { pathname, search, hash } = useLocation();
  const currentLocation = `${pathname}${search}${hash}`;

  /**
   * Go to screen
   * @param {Action} action Action received
   */
  const screen = (action) => dispatch(screenAction(action, pathname, currentLocation));

  /**
   * Reload current screen
   * @param {Action} action Action received
   */
  const reloadScreen = (action) => dispatch(reloadScreenAction(action, currentLocation));

  /**
   * Go to previous screen
   * @param {Action} action Action received
   */
  const back = (action) => dispatch(backAction(action));

  /**
   * Change application language
   * @param {Action} action Action received
   */
  const changeLanguage = (action) => dispatch(changeLanguageAction(action));

  /**
   * Reload application language
   * @param {Action} action Action received
   */
  const reloadLanguage = (action) => {
    i18n.reloadResources(settings.language);
    dispatch(acceptAction(action));
  };

  /**
   * Change application theme
   * @param {Action} action Action received
   */
  const changeTheme = (action, props) => dispatch(changeThemeAction(action));

  /**
   * Wait x milliseconds
   * @param {Action} action Action received
   */
  const wait = (action) => {
    setTimeout(() => dispatch(acceptAction(action)), action.parameters.target || 1);
  };

  /**
   * Retrieve a file
   * @param {Action} action Action received
   */
  const getFile = (action) => dispatch(getFileAction(action));

  /**
   * Change CSS class
   * @param {Action} action Action received
   * @param {Object} method Method to use
   */
  const changeClass = (action, method) => {
    const tag = document.querySelector(action.target);
    const targetClass = action.parameters[settings.targetActionKey] || "";
    const onAccept = () => dispatch(acceptAction(action));

    if (tag) {
      targetClass.split(" ").forEach(cssClass => tag.classList[method](cssClass));
      addEventListenerTimeout(tag, "transitionend", onAccept, onAccept, 500);
    } else {
      console.warn(`Warning, target node '${action.target}' is not defined`);
      onAccept();
    }
  };

  /**
   * Add a CSS class to a tag
   * @param {Action} action Action received
   */
  const addClass = (action) => changeClass(action, "add");

  /**
   * Remove CSS class from a tag
   * @param {Action} action Action received
   */
  const removeClass = (action) => changeClass(action, "remove");

  /**
   * Toggle CSS class from a tag
   * @param {Action} action Action received
   */
  const toggleClass = (action) => changeClass(action, "toggle");

  /**
   * Print the current screen
   * @param {Action} action Action received
   */
  const screenPrint = (action) => {
    window.print();
    dispatch(acceptAction(action));
  };

  /**
   * Close current window
   * @param {Action} action Action received
   */
  const closeWindow = (action) => {
    window.close();
    dispatch(acceptAction(action));
  };

  /**
   * Logout from the application
   */
  const logout = () => dispatch(logoutAction());

  /**
   * Redirect to a new URL
   * @param {object} action Action received
   */
  const redirect = (action) => dispatch(redirectAction(action));


  const getActions = () => ({
    "screen": screen,
    "reload": reloadScreen,
    "back": back,
    "change-language": changeLanguage,
    "reload-language": reloadLanguage,
    "change-theme": changeTheme,
    "wait": wait,
    "get-file": getFile,
    "add-class": addClass,
    "remove-class": removeClass,
    "toggle-class": toggleClass,
    "screen-print": screenPrint,
    "close-window": closeWindow,
    "logout": logout,
    "redirect": redirect,
  });

  return {
    getActions,
  };
};

export default useScreenService;
