import { useSyncExternalStore } from "react";
import MenuRegistry from "../redux/registry/MenuRegistry";

export function useMenuOptions() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.getOptions(),
    () => MenuRegistry.getOptions(),
  );
}

export function useMenuBreadcrumbs() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.getBreadcrumbs(),
    () => MenuRegistry.getBreadcrumbs(),
  );
}
