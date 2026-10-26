/**
 * Typed contract of the screen data the server sends to the React client (#808).
 *
 * `contract.ts` is GENERATED from the Java model in awe-model; never edit it by hand. Regenerate it with
 * `mvn -Pcontract -pl awe-framework/awe-model process-classes` (JDK 21).
 *
 * This hand-written entry re-exports the generated types and adds the pieces the generator cannot express.
 */
import type { Column, Component, ScreenColumn as GeneratedScreenColumn, ScreenComponent } from './contract';

export * from './contract';

/**
 * Controller of a screen component.
 *
 * Deliberately loose: the server serialises the controller without `@JsonTypeInfo`, and its `component` string
 * ("grid", "text", ...) is not a discriminant the generator can use to pick the concrete subclass. The generated
 * `Component` gives the keys every controller has; each concrete controller adds its own keys (`columnModel`,
 * `buttonModel`, ...), read through the index signature and narrowed where they are used.
 */
export type ComponentController = Component & Record<string, unknown>;

/** A screen component whose controller is typed as the loose {@link ComponentController}. */
export interface TypedScreenComponent extends Omit<ScreenComponent, 'controller'> {
  controller?: ComponentController;
}

/**
 * A grid column as the server sends it in `columnModel`.
 *
 * The Java `ScreenColumn` marks its controller `@JsonUnwrapped`: the JSON has no `controller` key, the properties of the
 * column (`frozen`, `sortable`, `width`, `actions`, ...) sit flat next to `gridId`, `id` and `model`. The generated type
 * only knows the keys of the declared `Component`, so it is widened with `Column` and loses the `controller` key.
 */
export type ScreenColumn = Omit<GeneratedScreenColumn, 'controller'> & Column;
