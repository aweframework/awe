/**
 * Create a navigator: the router reference that thunks use to navigate.
 * It is created together with the store, so it exists before the first dispatch. The router `navigate` function is
 * only available inside the router tree, so a navigation requested before it is attached is queued and run, in order,
 * as soon as `attach` receives it.
 * @returns {{navigate: function, attach: function}} Navigator
 */
export const createNavigator = () => {
  let routerNavigate = null;
  let pending = [];

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
