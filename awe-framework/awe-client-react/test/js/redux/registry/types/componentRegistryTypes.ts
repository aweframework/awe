// Type-level assertions for the ComponentRegistry, checked by `npm run typecheck` (not run by Jest)
import ComponentRegistry from "../../../../../src/redux/registry/ComponentRegistry";
import type {ComponentState} from "../../../../../src/utilities/mergeUtils";

// A component is registered by id with its base state, and read back
ComponentRegistry.register("Grd", {address: {view: "report", component: "Grd"}, attributes: {label: "Grid"}});
const base: ComponentState | null = ComponentRegistry.get("Grd");
// @ts-expect-error the id is text
ComponentRegistry.get(1);
// @ts-expect-error the base state is an object
ComponentRegistry.register("Grd", "state");

// The ids and the removed ids are text
const ids: string[] = ComponentRegistry.getAllIds();
const removed: string[] = ComponentRegistry.clear("report");
ComponentRegistry.unregister("Grd");
ComponentRegistry.clearAll();

// The statistics count components and views
const stats: {totalComponents: number; views: number; componentsByView: Record<string, number>} = ComponentRegistry.getStats();

export {base, ids, removed, stats};
