/**
 * Registry of the canvas effects of the login pages (S23). AWE ships no effect: an application writes its own effect
 * and registers it with a name, then selects it with awe.application.login.background-effect.
 *
 * The registry lives on globalThis (aweLoginEffects) because the bundle of the application and the one of the framework
 * are separate webpack builds: a map kept in this module would not be the same object in both. The React engine exports
 * registerLoginEffect (as registerWidget); an AngularJS application uses the global aweLoginEffects.register.
 */

/** Stops a running effect: cancels its frames, observers and listeners */
export type LoginEffectStop = () => void;

/**
 * A canvas effect of the image panel of the login page
 * @param canvas Canvas the effect draws on, filling the panel (or the card backdrop)
 * @returns Function that stops the effect, if the effect keeps running
 */
export type LoginEffect = (canvas: HTMLCanvasElement) => LoginEffectStop | void;

/** A page waiting for an effect of a name */
export type LoginEffectSubscriber = (effect: LoginEffect) => void;

/** Registry of the canvas effects of the login pages, shared by both engines */
export interface LoginEffectRegistry {
  /**
   * Register an effect under a name
   * @param name Name selected with awe.application.login.background-effect
   * @param effect Canvas effect
   */
  register(name: string, effect: LoginEffect): void;

  /**
   * Effect registered under a name
   * @param name Name of the effect
   * @returns Effect, or undefined when no effect has that name
   */
  get(name: string): LoginEffect | undefined;

  /**
   * Forget the effect of a name
   * @param name Name of the effect
   */
  unregister(name: string): void;

  /**
   * Wait for the effect of a name: the subscriber is called right away when the effect is already registered, and again
   * whenever the name is registered later
   * @param name Name of the effect
   * @param subscriber Callback with the effect
   * @returns Function that stops the subscription
   */
  subscribe(name: string, subscriber: LoginEffectSubscriber): () => void;
}

// The global object where both bundles keep the registry
const GLOBAL_KEY = "aweLoginEffects";

type LoginEffectGlobal = typeof globalThis & {[GLOBAL_KEY]?: LoginEffectRegistry};

/** Build an empty registry */
const createRegistry = (): LoginEffectRegistry => {
  const effects = new Map<string, LoginEffect>();
  const subscribers = new Map<string, Set<LoginEffectSubscriber>>();

  const notify = (name: string, effect: LoginEffect) => {
    [...(subscribers.get(name) ?? [])].forEach(subscriber => subscriber(effect));
  };

  return {
    register: (name, effect) => {
      if (effects.has(name)) {
        throw new Error(`Login effect with name "${name}" already exists.`);
      }
      effects.set(name, effect);
      notify(name, effect);
    },
    get: name => effects.get(name),
    unregister: name => {
      effects.delete(name);
    },
    subscribe: (name, subscriber) => {
      const registered = effects.get(name);
      if (registered) {
        subscriber(registered);
      }
      const waiting = subscribers.get(name) ?? new Set<LoginEffectSubscriber>();
      waiting.add(subscriber);
      subscribers.set(name, waiting);
      return () => {
        waiting.delete(subscriber);
        if (!waiting.size) {
          subscribers.delete(name);
        }
      };
    }
  };
};

/**
 * Registry of the canvas effects of the login pages: the one of the application bundle when it created it, else a new
 * one kept on globalThis
 * @returns Registry shared by both bundles
 */
export const loginEffectRegistry = (): LoginEffectRegistry => {
  const shared = (globalThis as LoginEffectGlobal)[GLOBAL_KEY];
  if (shared) {
    return shared;
  }
  const created = createRegistry();
  (globalThis as LoginEffectGlobal)[GLOBAL_KEY] = created;
  return created;
};

/**
 * Register a canvas effect of the login pages under a name
 * @param name Name selected with awe.application.login.background-effect
 * @param effect Canvas effect
 */
export const register = (name: string, effect: LoginEffect): void => loginEffectRegistry().register(name, effect);

/**
 * Effect registered under a name
 * @param name Name of the effect
 * @returns Effect, or undefined when no effect has that name
 */
export const get = (name: string): LoginEffect | undefined => loginEffectRegistry().get(name);

/**
 * Forget the effect of a name
 * @param name Name of the effect
 */
export const unregister = (name: string): void => loginEffectRegistry().unregister(name);

/**
 * Wait for the effect of a name (right away when it is registered, and when it is registered later)
 * @param name Name of the effect
 * @param subscriber Callback with the effect
 * @returns Function that stops the subscription
 */
export const subscribe = (name: string, subscriber: LoginEffectSubscriber): (() => void) =>
  loginEffectRegistry().subscribe(name, subscriber);

// The AngularJS engine and the bundles of the applications reach the same registry through the global object
loginEffectRegistry();
