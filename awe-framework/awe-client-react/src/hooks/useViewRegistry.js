import { useSyncExternalStore } from "react";
import ViewRegistry from "../redux/registry/ViewRegistry";

export function useView(viewName) {
  return useSyncExternalStore(
    (listener) => ViewRegistry.subscribe(listener),
    () => ViewRegistry.get(viewName),
    () => ViewRegistry.get(viewName),
  );
}

export function useCurrentViewName() {
  return useSyncExternalStore(
    (listener) => ViewRegistry.subscribe(listener),
    () => ViewRegistry.getCurrentView(),
    () => ViewRegistry.getCurrentView(),
  );
}
