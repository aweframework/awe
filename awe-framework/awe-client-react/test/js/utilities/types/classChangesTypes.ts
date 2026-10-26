// Type-level assertions for utilities/classChanges, checked by `npm run typecheck` (not run by Jest)
import {clearClassChanges, rememberClassChange} from "../../../../src/utilities/classChanges";

rememberClassChange(".a", "b", "add");
rememberClassChange(".a", "b", "remove");
rememberClassChange(".a", "b", "toggle");
const cleared: void = clearClassChanges();

// @ts-expect-error only add, remove and toggle are class change methods
rememberClassChange(".a", "b", "replace");
// @ts-expect-error the selector is text
rememberClassChange(1, "b", "add");
// @ts-expect-error the method is required
rememberClassChange(".a", "b");
export {cleared};
