import {
  addEventListenerTimeout,
  fetchFile,
  generateServerAction,
  getComponentValue,
  getRestUrl
} from "../utilities";
import i18n from "../i18n/i18n";
import {getFirstDefinedAndNotNullValue} from "../utilities/general";

let downloadIdentifier = 0;

/**
 * Screen service
 * @category Services
 */
class ScreenService {

  /**
   * Create the service
   */
  constructor() {
  }

  getActions() {
    return {
      "screen": this.screen.bind(this),
      "reload": this.reload.bind(this),
      "back": this.back.bind(this),
      "change-language": this.changeLanguage.bind(this),
      "reload-language": this.reloadLanguage.bind(this),
      "change-theme": this.changeTheme.bind(this),
      "wait": this.wait.bind(this),
      "get-file": this.getFile.bind(this),
      "add-class": this.addClass.bind(this),
      "remove-class": this.removeClass.bind(this),
      "toggle-class": this.toggleClass.bind(this),
      "print": this.screenPrint.bind(this),
      "close-window": this.closeWindow.bind(this),
      "logout": this.logout.bind(this)
    }
  }

  /**
   * Retrieve parameters and send them to the server
   * @param {object} action Action received
   * @param {object} props Properties
   */
  screen(action, props) {
    // Retrieve action parameters
    const {context, reload = false, parameters = {}} = action;

    // If token has been received, update it
    if ("token" in parameters) {
      props.updateSettings({"token": parameters.token});
    }

    // Define target screen
    let target = context ? `/${context}/` : "";
    target += getFirstDefinedAndNotNullValue(parameters.screen, parameters.target, action.target);

    // Location is not the same
    if (target !== location.pathname || reload) {
      props.history.push(target);
      props.acceptAction(action);
    } else if (props.settings.reloadCurrentScreen) {
      // Location is the same: reload
      this.reload(action, props);
    } else {
      props.acceptAction(action);
    }
  }

  /**
   * Reload the current state
   * @param {object} action Action received
   * @param {object} props Properties
   */
  reload(action, props) {
    // Reload
    props.history.go(0);

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Return to the previous screen
   * @param {object} action Action received
   * @param {object} props Properties
   */
  back(action, props) {
    // Reload
    props.history.goBack();

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Change the language of the interface
   * @param {object} action Action received
   * @param {object} props Properties
   */
  changeLanguage(action, props) {
    const {language, target} = action.parameters;
    let targetLanguage;

    if (target) {
      targetLanguage = getComponentValue(props.components[target]);
    }

    // If language has been received, update it
    if (language || targetLanguage) {
      props.updateSettings({language: language || targetLanguage});
    }

    // Finish screen action
    props.acceptAction(action);
  }

  /**
   * Reload the language resources
   * @param {object} action Action received
   * @param {object} props Properties
   */
  reloadLanguage(action, props) {
    const {settings} = props;

    i18n.reloadResources(settings.language);

    // Finish screen action
    props.acceptAction(action);
  }

  /**
   * Change the theme of the interface
   * @param {object} action Action received
   * @param {object} props Properties
   */
  changeTheme(action, props) {
    const {theme, target} = action.parameters;
    let targetTheme;

    if (target) {
      targetTheme = getComponentValue(props.components[target]);
    }

    // If language has been received, update it
    if (theme || targetTheme) {
      props.updateSettings({theme: theme || targetTheme});
    }

    // Finish screen action
    props.acceptAction(action);
  }

  /**
   * Wait x milliseconds
   * @param {object} action Action received
   * @param {object} props Properties
   */
  wait(action, props) {
    // Retrieve action parameters
    let time = action.parameters.target || 1;
    setTimeout(() => props.acceptAction(action), time);
  }

  /**
   * Get file from server
   * @param {object} action get file from server
   * @param {object} props Properties
   */
  getFile(action, props) {
    // Variable definition
    const {token} = props.settings;

    // Generate url parameter
    fetchFile(getRestUrl("file", "download"), {...action.parameters, d: downloadIdentifier++}, token)
      .then(() => props.acceptAction(action));
  }

  /**
   * Add Class
   * @param {object} action Action received
   * @param {object} props Properties
   */
  addClass(action, props) {
    this.changeClass(action, "add", props);
  }

  /**
   * Remove Class
   * @param {object} action Action received
   * @param {object} props Properties
   */
  removeClass(action, props) {
    this.changeClass(action, "remove", props);
  }

  /**
   * Toggle Class
   * @param {object} action Action received
   * @param {object} props Properties
   */
  toggleClass(action, props) {
    this.changeClass(action, "toggle", props);
  }

  /**
   * Add/Remove a class to a tag
   * @param {object} action Action received
   * @param {object} method What to do with the class
   * @param {object} props Properties
   */
  changeClass(action, method, props) {
    // Variable definition
    const {acceptAction, settings} = props;
    let tagSelector = action.target;
    let parameters = action.parameters;
    let targetClass = parameters[settings.targetActionKey] || "";

    // Add/remove the class/classes
    let tag = document.querySelector(tagSelector);
    if (tag) {
      targetClass.split(" ").forEach(cssClass => tag.classList[method](cssClass));

      // Close action on transition end or timeout
      addEventListenerTimeout(tag, "transitionend", () => acceptAction(action),
        () => acceptAction(action), 500);
    } else {
      console.warn("Warning, target node '" + tagSelector + "' is not defined");
      acceptAction(action);
    }
  }

  /**
   * Print the current screen
   * @param {object} action Action received
   * @param {object} props Properties
   */
  screenPrint(action, props) {
    window.print();

    // Close action
    props.acceptAction(action);
  }

  /**
   * Close the current window
   * @param {object} action Action received
   * @param {object} props Properties
   */
  closeWindow(action, props) {
    // Call window close
    window.close();

    // Close action
    props.acceptAction(action);
  }

  /**
   * Destroy all views
   * @param {object} action Action received
   * @param {object} props Properties
   */
  logout(action, props) {
    // Close following actions
    props.deleteStack();

    // Send action confirm
    props.addActionsTop([
      {type: "disconnectWebsocket"},
      generateServerAction({}, "logout", "", {}, false, false, props.settings)
    ]);
  }
}

export default ScreenService;
