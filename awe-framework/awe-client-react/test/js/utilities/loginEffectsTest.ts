import {get, loginEffectRegistry, register, subscribe, unregister} from "../../../src/utilities/loginEffects";
import type {LoginEffectRegistry} from "../../../src/utilities/loginEffects";

// The registry kept on globalThis, as the other bundle of the application sees it
const sharedRegistry = () =>
  (globalThis as typeof globalThis & {aweLoginEffects?: LoginEffectRegistry}).aweLoginEffects;

// The registry of the canvas effects of the login pages (S23). AWE ships no effect: an application registers the ones
// it writes in its own bundle and selects one with awe.application.login.background-effect. The registry lives on
// globalThis because the bundle of the application and the one of the framework are separate webpack builds
describe("awe-react-client/test/js/utilities/loginEffectsTest.ts", () => {
  const effect = jest.fn(() => jest.fn());
  const other = jest.fn(() => jest.fn());

  afterEach(() => {
    unregister("brand-wave");
    unregister("shared-wave");
    unregister("app-wave");
  });

  it("returns the effect registered under a name", () => {
    register("brand-wave", effect);

    expect(get("brand-wave")).toBe(effect);
  });

  it("returns no effect for a name nobody registered", () => {
    expect(get("plasma-wave")).toBeUndefined();
  });

  it("refuses to register a name twice, as registerWidget does", () => {
    register("brand-wave", effect);

    expect(() => register("brand-wave", other)).toThrow(/Login effect with name "brand-wave" already exists/);
    expect(get("brand-wave")).toBe(effect);
  });

  it("forgets an unregistered effect, and the name can be registered again", () => {
    register("brand-wave", effect);
    unregister("brand-wave");

    expect(get("brand-wave")).toBeUndefined();

    register("brand-wave", other);
    expect(get("brand-wave")).toBe(other);
  });

  it("tells a page waiting for a name when the effect is registered later", () => {
    const waiting = jest.fn();
    subscribe("brand-wave", waiting);
    expect(waiting).not.toHaveBeenCalled();

    register("brand-wave", effect);

    expect(waiting).toHaveBeenCalledTimes(1);
    expect(waiting).toHaveBeenCalledWith(effect);
  });

  it("tells a page that subscribes after the effect, right away", () => {
    register("brand-wave", effect);
    const waiting = jest.fn();

    subscribe("brand-wave", waiting);

    expect(waiting).toHaveBeenCalledWith(effect);
  });

  it("stops telling a page that unsubscribes", () => {
    const waiting = jest.fn();
    const unsubscribe = subscribe("brand-wave", waiting);
    unsubscribe();

    register("brand-wave", effect);

    expect(waiting).not.toHaveBeenCalled();
  });

  it("keeps the registry on globalThis so another bundle shares the effects of the application", () => {
    const registry = sharedRegistry()!;

    expect(registry).toBe(loginEffectRegistry());
    (["register", "get", "unregister", "subscribe"] as const).forEach(name =>
      expect(typeof registry[name]).toBe("function"));
    register("brand-wave", effect);

    jest.resetModules();
    const applicationBundle = jest.requireActual<typeof import("../../../src/utilities/loginEffects")>(
      "../../../src/utilities/loginEffects");

    expect(sharedRegistry()).toBe(registry);
    expect(applicationBundle.get("brand-wave")).toBe(effect);
    applicationBundle.register("shared-wave", other);
    expect(get("shared-wave")).toBe(other);
  });

  it("registers and looks up effects through the global object, as an AngularJS application does", () => {
    const registry = sharedRegistry()!;

    registry.register("app-wave", effect);

    expect(registry.get("app-wave")).toBe(effect);
    expect(get("app-wave")).toBe(effect);
    registry.unregister("app-wave");
    expect(get("app-wave")).toBeUndefined();
  });
});
