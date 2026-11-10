import {updateViewComponentsWithDependencies} from "./components";
import MenuRegistry from "../registry/MenuRegistry";
import {getBreadcrumbLevels, originOf} from "../../utilities/breadcrumbs";
import {updateMessages} from "../actions/messages";
import ViewRegistry from "../registry/ViewRegistry";
import {acceptAction, addActionsTop, deleteStack} from "../actions/actions";
import type {ActionItem} from "../actions/actions";
import {runRefusal} from "./refusal";
import {clearComponents} from "../actions/components";
import type {FetchError} from "../../utilities";
import {
  fetchFile,
  fetchJson,
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
  fixDefaultModel,
  getSpecificAttributes,
  inspectComponentStructure,
  parseValidationRules,
  rebuildStructureNodes
} from "../../utilities/components";
import {getGridDefaultSearchSeed} from "../../utilities/gridDefaultSearch";
import type {SavedSearchesGridAttributes} from "../../utilities/gridDefaultSearch";
import type {ComponentModelData, ControllerData, StructureNode} from "../../utilities/components";
import type {ComponentState} from "../../utilities/mergeUtils";
import {produce} from "immer";
import {getFormValues} from "../selectors/form";
import {updateSettings} from "../actions/settings";
import type {Settings} from "../actions/settings";
import {getAllComponents} from "../selectors/componentSelectors";
import {ComponentType, getFirstDefinedAndNotNullValue} from "../../utilities/general";
import {clearClassChanges} from "../../utilities/classChanges";
import {pickThemeAppearance} from "../../utilities/themeAppearance";
import {navigationActions} from "../actions/navigation";
import type {AppThunk, MenuOption} from "../types";
import type {ClientAction, Translate} from "./types";

/** A component of the screen data, as the server sends it */
export interface ScreenDataComponent {
  id: string;
  controller: ControllerData;
  model: ComponentModelData & {defaultValues?: unknown};
}

/** The screen data the server answers for a screen */
interface ScreenResponse {
  structure: StructureNode;
  components: ScreenDataComponent[];
  messages: Record<string, unknown>;
  screen: Record<string, unknown>;
  actions?: ActionItem[];
}

/** An action that goes to a screen: the target can also come in the parameters */
interface ScreenClientAction extends ClientAction {
  context?: string;
  reload?: boolean;
}

let downloadIdentifier = 0;
let screenReloadIdentifier = 0;
let themeAppearanceRequest = 0;

function getScreenReloadState(isFresh = false): { screenReloadToken: number; freshScreen?: boolean } {
  return { screenReloadToken: ++screenReloadIdentifier, ...(isFresh ? { freshScreen: true } : {}) };
}

function resolveScreenTarget(screen: string, pathname: string, context?: string): string {
  if (screen.startsWith("/")) {
    return screen;
  }

  return context ? `/${context}/${screen}` : `${pathname.split("/").slice(0, -1).join("/")}/${screen}`;
}

function getScreenNavigationOptions(isCurrentScreenReload: boolean, isRelativeRoute: boolean, isFresh: boolean): Record<string, unknown> {
  if (isCurrentScreenReload) {
    return { replace: true, relative: false, state: getScreenReloadState(isFresh) };
  }

  return { relative: isRelativeRoute ? "path" : false, ...(isFresh ? { state: { freshScreen: true } } : {}) };
}

/**
 * Build the state of a component of the screen data (its controller and model) as the screen stores it
 * @param {string} view View of the component
 * @param {object} component Screen component ({id, controller, model})
 * @param {object} settings Application settings
 * @param {string[]} source Structure path of the component (its context)
 * @returns {object} Component state
 */
export function buildComponent(
  view: string,
  component: ScreenDataComponent,
  settings: Settings,
  source: string[] = []
): ComponentState {
  const isGrid = "columnModel" in component.controller;
  const address = { view, component: component.id };
  const model = fixModel(component.model, isGrid);
  const type = component.controller.component;
  const ownOptions = type === ComponentType.COMPONENT_CHECKBOX || type === ComponentType.COMPONENT_RADIO;
  const defaultModel = fixDefaultModel(model, component.model.defaultValues, isGrid, ownOptions);
  const controller = fixController(component.controller, isGrid, settings);
  // A grid opens with the column filters and the sort of the default search the server applied to its initial load (#945)
  const seed = isGrid ? getGridDefaultSearchSeed(controller as SavedSearchesGridAttributes) : null;
  const attributes = {...controller, ...seed?.attributes};
  const specificAttributes = {...getSpecificAttributes(component.controller, isGrid, settings), ...seed?.specificAttributes};
  const validationRules = parseValidationRules(controller.validation as string | undefined, address);
  return {
    uid: component.id,
    address: { ...address },
    model: { ...model },
    storedModel: { ...model },
    defaultModel: { ...defaultModel },
    attributes: { ...attributes },
    specificAttributes: { ...specificAttributes },
    storedAttributes: { ...controller },
    validationRules: { ...validationRules },
    storedValidationRules: { ...validationRules },
    actions: component.controller.actions || [],
    dependencies: component.controller.dependencies || [],
    contextMenu: component.controller.contextMenu || [],
    context: { view, source: [...source] }
  };
}

// A fresh screen (opened from a menu option) is flagged so the server keeps the criteria of the screen it leaves but
// does not give its values to the new screen
export const loadScreen = (view: string, option: string, t: Translate, fresh = false): AppThunk<Promise<void>> => async (dispatch, getState) => {
  try {
    const state = getState();
    const { settings } = state;
    const token = settings.token;

    // A refused or expired answer of the screen data runs the actions its status demands (#935, S5, S6, S7), from
    // the rule every client request shares; any other failure keeps the message it always had
    const manageScreenError = (option: string, error: Partial<FetchError> = {}): void => {
      console.error(`Error retrieving screen structure: ${option}`, error);
      if (runRefusal(error, dispatch)) {
        return;
      }
      dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_SCREEN_GENERATION_ERROR', t), translateLabel(error.message, t))]));
    };

    // Set screen as loading
    ViewRegistry.setCurrentView(view);
    ViewRegistry.updateView(view, { loading: true });

    // An error answer has no structure, but the message and the status of the error. The request has always sent an
    // empty object where the view goes
    const response = await fetchScreen<ScreenResponse>(
      option, {} as unknown as string, token, { ...getFormValues(state), ...(fresh ? { freshScreen: true } : {}) }
    ) as ScreenResponse & Partial<FetchError>;
    if (!response.structure) {
      manageScreenError(option, response);
      return;
    }

    const componentStructure = inspectComponentStructure(response.structure, [], {});

    // Forget the class changes of the previous screen and clear its components before loading the new ones.
    clearClassChanges();
    dispatch({ ...clearComponents(view), settings });

    // Register components with their dependencies.
    dispatch(updateViewComponentsWithDependencies(view, response.components.reduce((list = {}, component = {} as ScreenDataComponent) => ({
      ...list,
      [component.id]: buildComponent(view, component, settings, componentStructure[component.id] || [])
    }), {})));

    const menu = response.components.find((component) => component.id === "MainMenu");
    if (menu) {
      MenuRegistry.setOptions(menu.controller.options as MenuOption[] | undefined);
    }
    dispatch(updateMessages(view, response.messages));
    // The structure is a skeleton: each component node gets its attributes back from its controller
    const controllers = Object.fromEntries(response.components.map(component => [component.id, component.controller]));
    const structure = rebuildStructureNodes(response.structure, controllers);
    ViewRegistry.setView(view, { ...response.screen, structure: produce(structure, draft => draft), loading: false });
    if (response.actions?.length) {
      // Dispatch server-provided actions (e.g. initial messages) after the screen is fully registered.
      dispatch(addActionsTop(response.actions));
    }

    if (settings.debug === "INFO" || settings.debug === "DEBUG") {
      const safeSize = (value: unknown): number => {
        try {
          return JSON.stringify(value).length;
        } catch {
          return -1;
        }
      };
      const stateSize = safeSize(getState());
      const componentsCount = Object.keys(getState().components || {}).length;
      const structureSize = safeSize(response.structure);
      const menuSize = safeSize(menu?.controller?.options || []);
      // Debug-level screen metrics, only emitted when the debug setting asks for them
      // eslint-disable-next-line no-console
      console.info("[Metrics] sizes(bytes)", {
        reduxState: stateSize,
        componentsCount,
        viewStructure: structureSize,
        menuOptions: menuSize
      });
    }
  } catch (error) {
    dispatch(addActionsTop([
      generateMessageAction("error", "Error", (error as Error).message || "Failed to load screen")
    ]));
  }
};

export function screenAction(action: ScreenClientAction, pathname: string, currentLocation: string = pathname): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    const { context, reload = false, parameters = {} } = action;

    if ("token" in parameters) {
      dispatch(updateSettings({ token: parameters.token as string }));
    }

    const screen = getFirstDefinedAndNotNullValue(parameters.screen, parameters.target, action.target) as string;
    const isRelativeRoute = !screen.startsWith("/");
    const target = resolveScreenTarget(screen, pathname, context);
    const isCurrentScreenReload = reload && target === pathname;

    // The breadcrumb of the screen opened keeps the screens the user came from, unless the menu launches it
    if (action.origin === "menu") {
      MenuRegistry.setOrigin([]);
    } else if (target !== pathname) {
      const report = ViewRegistry.get("report");
      const levels = getBreadcrumbLevels(MenuRegistry.getOrigin(), MenuRegistry.getBreadcrumbs(), report, currentLocation);
      MenuRegistry.setOrigin(originOf(levels, currentLocation, report?.option as string | undefined));
    }

    if (target !== pathname || reload) {
      const navigationTarget = isCurrentScreenReload ? currentLocation : target;
      dispatch(navigationActions.navigateTo(navigationTarget, getScreenNavigationOptions(isCurrentScreenReload, isRelativeRoute, action.origin === "menu")));
      dispatch(acceptAction(action));
    } else if (settings.reloadCurrentScreen) {
      dispatch(reloadScreenAction(action, currentLocation));
    } else {
      dispatch(acceptAction(action));
    }
  };
}

export function reloadScreenAction(action: ClientAction, pathname: string): AppThunk {
  return (dispatch) => {
    dispatch(navigationActions.navigateTo(pathname, { replace: true, state: getScreenReloadState(action.origin === "menu") }));
    dispatch(acceptAction(action));
  };
}

export function backAction(action: ClientAction): AppThunk {
  return (dispatch) => {
    dispatch(navigationActions.navigateTo(-1));
    dispatch(acceptAction(action));
  };
}

export const changeLanguageAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { language, target } = action.parameters as { language?: string; target?: string };
    const targetLanguage = target ? getComponentValue(components[target]) as string : null;

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
export const changeThemeAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const components = getAllComponents(getState());
    const { theme, target } = action.parameters as { theme?: string; target?: string };
    const targetTheme = target ? getComponentValue(components[target]) as string : null;

    if (theme || targetTheme) {
      dispatch(updateSettings({ theme: theme || targetTheme as string }));
    }
    dispatch(acceptAction(action));
    if (theme || targetTheme) {
      dispatch(refreshThemeAppearanceAction());
    }
  };
};

/**
 * Read the theme appearance (primary, surface, mode, density and palette) of the session again from the server, after
 * the theme, the user or the mode of the user changed. Only the response of the last request is kept
 * @returns {function} Thunk (returns the request promise)
 */
export const refreshThemeAppearanceAction = (): AppThunk<Promise<void>> => {
  return (dispatch, getState) => {
    const request = ++themeAppearanceRequest;
    const { settings } = getState();
    // A request that cannot start rejects too, so it never breaks the action that asked for it
    return new Promise<object>((resolve) => resolve(fetchJson<object>("POST", "/settings/appearance", {}, settings.token)))
      .then((serverSettings) => {
        if (request === themeAppearanceRequest) {
          const appearance = pickThemeAppearance(serverSettings);
          if (Object.keys(appearance).length) {
            dispatch(updateSettings(appearance));
          }
        }
      })
      .catch((reason) => console.error("Error reading the theme appearance:", reason));
  };
};

/**
 * Retrieve a file
 * @param {Action} action Action received
 */
export const getFileAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    const { settings } = getState();
    // The whole settings go where the token is expected, as they always did
    fetchFile(getRestUrl("file", "download"), { ...action.parameters, d: downloadIdentifier++ }, settings as unknown as string)
      .then(() => dispatch(acceptAction(action)))
      .catch((reason) => console.error("Error downloading file:", reason));
  };
};

/**
 * Logout from the application
 */
export const logoutAction = (): AppThunk => {
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
export const redirectAction = (action: ClientAction): AppThunk => {
  return (dispatch) => {
    const { target, parameters = {} } = action;
    const { newWindow = false } = parameters as { newWindow?: boolean };

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
