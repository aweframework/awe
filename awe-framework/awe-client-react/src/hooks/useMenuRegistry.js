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

export function useMenuOrigin() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.getOrigin(),
    () => MenuRegistry.getOrigin(),
  );
}

export function useMenuExpandRequest() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.getExpandRequest(),
    () => MenuRegistry.getExpandRequest(),
  );
}

export function useMenuHasExpander() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.hasExpander(),
    () => MenuRegistry.hasExpander(),
  );
}

export function useMenuLocation() {
  return useSyncExternalStore(
    (listener) => MenuRegistry.subscribe(listener),
    () => MenuRegistry.getLocation(),
    () => MenuRegistry.getLocation(),
  );
}
