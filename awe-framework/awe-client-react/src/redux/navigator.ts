import type {ThunkExtra} from "./types";

type NavigateFunction = ThunkExtra["navigate"];

/** The router reference thunks navigate with: `attach` receives the router `navigate` function once it exists */
export interface Navigator {
  navigate: NavigateFunction;
  attach: (navigateFn: NavigateFunction) => void;
}

/**
 * Create a navigator: the router reference that thunks use to navigate.
 * It is created together with the store, so it exists before the first dispatch. The router `navigate` function is
 * only available inside the router tree, so a navigation requested before it is attached is queued and run, in order,
 * as soon as `attach` receives it.
 * @returns Navigator
 */
export const createNavigator = (): Navigator => {
  let routerNavigate: NavigateFunction | null = null;
  let pending: Parameters<NavigateFunction>[] = [];

  return {
    navigate: (target, options) => {
      if (routerNavigate) {
        routerNavigate(target, options);
      } else {
        pending.push([target, options]);
      }
    },
    attach: (navigateFn) => {
      routerNavigate = navigateFn;
      const queued = pending;
      pending = [];
      queued.forEach(([target, options]) => navigateFn(target, options));
    }
  };
};
