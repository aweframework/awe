import { useSyncExternalStore } from "react";
import SizeRegistry from "../redux/registry/SizeRegistry";

export function useScreenSize() {
  return useSyncExternalStore(
    (listener) => SizeRegistry.subscribe(listener),
    () => SizeRegistry.getSize(),
    () => SizeRegistry.getSize(),
  );
}
