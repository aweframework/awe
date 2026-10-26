// Type-level assertions for the Menu, Size, SpecificDependency and View registries, checked by `npm run typecheck`
// (not run by Jest)
import MenuRegistry from "../../../../../src/redux/registry/MenuRegistry";
import SizeRegistry from "../../../../../src/redux/registry/SizeRegistry";
import SpecificDependencyRegistry from "../../../../../src/redux/registry/SpecificDependencyRegistry";
import ViewRegistry from "../../../../../src/redux/registry/ViewRegistry";
import type {MenuOption, SizeState, ViewData} from "../../../../../src/redux/types";

// The menu registry keeps the options (with their allowed flag) and the breadcrumbs of the current option
MenuRegistry.setOptions([{name: "home", visible: true, options: [{name: "child"}]}]);
const options: MenuOption[] = MenuRegistry.getOptions();
const breadcrumbs: {items: unknown[]; option: string | null} = MenuRegistry.getBreadcrumbs();
MenuRegistry.setBreadcrumbs("home", [{name: "home"}]);
// @ts-expect-error the options are a list
MenuRegistry.setOptions("home");
// @ts-expect-error the option key is text
MenuRegistry.setBreadcrumbs(1, []);
const unsubscribeMenu: () => void = MenuRegistry.subscribe(() => undefined);
// @ts-expect-error the listener is a function
MenuRegistry.subscribe("listener");
MenuRegistry.clear();

// The size registry merges the measures
SizeRegistry.setSize({width: 10});
const size: SizeState = SizeRegistry.getSize();
// @ts-expect-error the measures are an object
SizeRegistry.setSize("10");
const unsubscribeSize: () => void = SizeRegistry.subscribe(() => undefined);

// The specific dependency registry keeps a handler by component type
SpecificDependencyRegistry.register("grid", (dependency, value, launch) => {
  const isLaunched: boolean = launch;
  return isLaunched && value ? {updateAttributes: dependency} : null;
});
SpecificDependencyRegistry.register("text", () => undefined);
// @ts-expect-error the handler is a function
SpecificDependencyRegistry.register("grid", "handler");
// @ts-expect-error the component type is text
SpecificDependencyRegistry.register(1, () => null);
const found = SpecificDependencyRegistry.get("grid");
const action = found?.({}, "value", true);
SpecificDependencyRegistry.unregister("grid");
SpecificDependencyRegistry.clear();

// The view registry keeps the data of every view and the name of the current one
const view: ViewData | undefined = ViewRegistry.get("base");
const current: string = ViewRegistry.getCurrentView();
ViewRegistry.setCurrentView("report");
ViewRegistry.setView("report", {loading: false});
ViewRegistry.updateView("report", {loading: true});
// @ts-expect-error the data of a view is an object
ViewRegistry.setView("report", "data");
// @ts-expect-error the name of a view is text
ViewRegistry.get(1);
ViewRegistry.clearView("report");
ViewRegistry.clearAll();
const unsubscribeView: () => void = ViewRegistry.subscribe(() => undefined);

export {options, breadcrumbs, unsubscribeMenu, size, unsubscribeSize, action, view, current, unsubscribeView};
