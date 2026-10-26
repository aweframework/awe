// Type-level assertions for utilities/color, checked by `npm run typecheck` (not run by Jest)
import {fromColor, toColor} from "../../../../src/utilities/color";

const hex: string = toColor("ff0000");
const bare: string = fromColor("#ff0000");
toColor(null);
fromColor(undefined);

// @ts-expect-error a color is text
toColor(15);
// @ts-expect-error a color is text
fromColor({});
export {hex, bare};
