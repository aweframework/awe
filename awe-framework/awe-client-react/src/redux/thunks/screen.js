import {updateViewComponentsWithDependencies} from "./components";
import MenuRegistry from "../registry/MenuRegistry";
import {updateMessages} from "../actions/messages";
import ViewRegistry from "../registry/ViewRegistry";
import {acceptAction, addActionsTop, deleteStack} from "../actions/actions";
import {clearComponents} from "../actions/components";
import {
  fetchFile,
  fetchScreen,
  generateMessageAction,
  getComponentValue,
  getContextPath,
  getRestUrl,
  translateLabel
} from "../../utilities";
import {
  fixController,
  fixModel,
  getSpecificAttributes,
  inspectComponentStructure,
  parseValidationRules
} from "../../utilities/components";
import {produce} from "immer";
import {getFormValues} from "../selectors/form";
import {updateSettings} from "../actions/settings";
import {getAllComponents} from "../selectors/componentSelectors";
import {getFirstDefinedAndNotNullValue} from "../../utilities/general";
import {navigationActions} from "../actions/navigation";

let downloadIdentifier = 0;
let screenReloadIdentifier = 0;

function getScreenReloadState() {
  return { screenReloadToken: ++screenReloadIdentifier };
}

function resolveScreenTarget(screen, pathname, context) {
  if (screen.startsWith("/")) {
    return screen;
  }

  return context ? `/${context}/${screen}` : `${pathname.split("/").slice(0, -1).join("/")}/${screen}`;
}

function getScreenNavigationOptions(isCurrentScreenReload, isRelativeRoute) {
  if (isCurrentScreenReload) {
    return { replace: true, relative: false, state: getScreenReloadState() };
  }

  return { relative: isRelativeRoute ? "path" : false };
}

export const loadScreen = (view, option, t) => async (dispatch, getState) => {
  try {
    const state = getState();
    const { settings } = state;
    const token = settings.token;

    const manageScreenError = (option, error = {}) => {
      console.error(`Error retrieving screen structure: ${option}`, error);
      const { status } = error;
      switch (status) {
        case 401:
        case 403:
          dispatch(addActionsTop([
            { type: "screen", target: "" },
            { ...generateMessageAction("warning", translateLabel('ERROR_TITLE_SESSION_EXPIRED', t), translateLabel('ERROR_MESSAGE_SESSION_EXPIRED', t)), async: true }
          ]));
          break;
        default:
          dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_SCREEN_GENERATION_ERROR', t), translateLabel(error.message, t))]));
          break;
      }
    };

    // Set screen as loading
    ViewRegistry.setCurrentView(view);
    ViewRegistry.updateView(view, { loading: true });

    const response = await fetchScreen(option, {}, token, getFormValues(state));
    if (!response.structure) {
      manageScreenError(option, response);
      return;
    }

    const componentStructure = inspectComponentStructure(response.structure, [], {});

    // Clear previous view components before loading the new ones.
    dispatch({ ...clearComponents(view), settings });

    // Register components with their dependencies.
    dispatch(updateViewComponentsWithDependencies(view, response.components.reduce((list = {}, component = {}) => {
      const isGrid = "columnModel" in component.controller;
      const address = { view, component: component.id };
      const model = fixModel(component.model, isGrid);
      const controller = fixController(component.controller, isGrid, settings);
      const specificAttributes = getSpecificAttributes(component.controller, isGrid, settings);
      const validationRules = parseValidationRules(controller.validation, address);
      return {
        ...list,
        [component.id]: {
          uid: component.id,
          address: { ...address },
          model: { ...model },
          storedModel: { ...model },
          attributes: { ...controller },
          specificAttributes: { ...specificAttributes },
          storedAttributes: { ...controller },
          validationRules: { ...validationRules },
          storedValidationRules: { ...validationRules },
          actions: component.controller.actions || [],
          dependencies: component.controller.dependencies || [],
          contextMenu: component.controller.contextMenu || [],
          context: { view: address.view, source: [...(componentStructure[component.id] || [])] }
        }
      };
    }, {})));

    const menu = response.components.find((component) => component.id === "MainMenu");
    if (menu) {
      MenuRegistry.setOptions(menu.controller.options);
    }
    dispatch(updateMessages(view, response.messages));
    ViewRegistry.setView(view, { ...response.screen, structure: produce(response.structure, draft => draft), loading: false });
    if (response.actions?.length) {
      // Dispatch server-provided actions (e.g. initial messages) after the screen is fully registered.
      dispatch(addActionsTop(response.actions));
    }

    if (settings.debug === "INFO" || settings.debug === "DEBUG") {
      const safeSize = (value) => {
        try {
          return JSON.stringify(value).length;
        } catch (_e) {
          return -1;
        }
      };
      const stateSize = safeSize(getState());
      const componentsCount = Object.keys(getState().components || {}).length;
      const structureSize = safeSize(response.structure);
      const menuSize = safeSize(menu?.controller?.options || []);
      console.info("[Metrics] sizes(bytes)", {
        reduxState: stateSize,
        componentsCount,
        viewStructure: structureSize,
        menuOptions: menuSize
      });
    }
  } catch (error) {
    dispatch(addActionsTop([
      generateMessageAction("error", "Error", error.message || "Failed to load screen")
    ]));
  }
};

export function screenAction(action, pathname, currentLocation = pathname) {
  return (dispatch, getState) => {
    const { settings } = getState();
    const { context, reload = false, parameters = {} } = action;

    if ("token" in parameters) {
      dispatch(updateSettings({ token: parameters.token }));
    }

    const screen = getFirstDefinedAndNotNullValue(parameters.screen, parameters.target, action.target);
    const isRelativeRoute = !screen.startsWith("/");
    const target = resolveScreenTarget(screen, pathname, context);
    const isCurrentScreenReload = reload && target === pathname;

    if (target !== pathname || reload) {
      const navigationTarget = isCurrentScreenReload ? currentLocation : target;
      dispatch(navigationActions.navigateTo(navigationTarget, getScreenNavigationOptions(isCurrentScreenReload, isRelativeRoute)));
      dispatch(acceptAction(action));
    } else if (settings.reloadCurrentScreen) {
      dispatch(reloadScreenAction(action, currentLocation));
    } else {
      dispatch(acceptAction(action));
    }
  };
}

export function reloadScreenAction(action, pathname) {
  return (dispatch) => {
    dispatch(navigationActions.navigateTo(pathname, { replace: true, state: getScreenReloadState() }));
    dispatch(acceptAction(action));
  };
}

export function backAction(action) {
  return (dispatch) => {
    dispatch(navigationActions.navigateTo(-1));
    dispatch(acceptAction(action));
  };
}

export const changeLanguageAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { language, target } = action.parameters;
    const targetLanguage = target ? getComponentValue(components[target]) : null;

    if (language || targetLanguage) {
      dispatch(updateSettings({ language: language || targetLanguage }));
    }
    dispatch(acceptAction(action));
  };
};

/**
 * Change application theme
 * @param {Action} action Action received
 */
export const changeThemeAction = (action) => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { theme, target } = action.parameters;
    const targetTheme = target ? getComponentValue(components[target]) : null;

    if (theme || targetTheme) {
      dispatch(updateSettings({ theme: theme || targetTheme }));
    }
    dispatch(acceptAction(action));
  };
};

/**
 * Retrieve a file
 * @param {Action} action Action received
 */
export const getFileAction = (action) => {
  return (dispatch, getState) => {
    const { settings } = getState();
    fetchFile(getRestUrl("file", "download"), { ...action.parameters, d: downloadIdentifier++ }, settings)
      .then(() => dispatch(acceptAction(action)));
  };
};

/**
 * Logout from the application
 */
export const logoutAction = () => {
  return (dispatch) => {
    dispatch(deleteStack());
    dispatch(addActionsTop([
      { type: "disconnectWebsocket" },
    ]));

    // Launch a logout server action
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `${getContextPath()}${getRestUrl("action", "logout")}`;

    document.body.appendChild(form);
    form.submit();
  };
};

/**
 * Redirect to a new URL
 * @param {object} action Action received
 */
export const redirectAction = (action) => {
  return (dispatch) => {
    const { target, parameters = {} } = action;
    const { newWindow = false } = parameters;

    if (newWindow) {
      // Open url in new window
      window.open(target, "_blank");
    } else {
      // Redirect browser
      window.open(target, "_self");
    }

    // Close action
    dispatch(acceptAction(action));
  };
};
