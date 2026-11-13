// Type-level assertions for utilities/keyboard, checked by `npm run typecheck` (not run by Jest)
import {ANY, ANY_MODIFIERS, createKeyHandler, DIGIT_CODES, matchesShortcut, SHORTCUTS} from "../../../../src/utilities/keyboard";

declare const event: KeyboardEvent;

const matched: boolean = matchesShortcut(event, {key: ["ArrowUp", "ArrowDown"], alt: true, shift: ANY});
matchesShortcut(event, {code: "Digit1", ...ANY_MODIFIERS});
const handler: (event: KeyboardEvent) => boolean = createKeyHandler([
  {shortcut: {key: "Enter"}, handler: (keyEvent, shortcut) => keyEvent.preventDefault() ?? shortcut.key},
  {shortcuts: [{key: "a"}, {key: "b"}], when: (keyEvent) => keyEvent.key !== "", handler: () => undefined}
]);
const codes: readonly string[] = DIGIT_CODES;
const first: {id: string; keys: string; scope: string; description: string} = SHORTCUTS[0];

// @ts-expect-error a modifier is a boolean or ANY
matchesShortcut(event, {key: "a", alt: "yes"});
// @ts-expect-error the key is text or a list of texts
matchesShortcut(event, {key: 13});
// @ts-expect-error a binding needs a handler
createKeyHandler([{shortcut: {key: "a"}}]);
// @ts-expect-error the bindings are a list
createKeyHandler({shortcut: {key: "a"}, handler: () => undefined});
export {matched, handler, codes, first};
