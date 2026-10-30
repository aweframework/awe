import {compatFamilyCases, describeLessOnly} from "./support/compatFamily";
import {popovers} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${popovers.title}`, () => {
  it.each(compatFamilyCases(popovers))("%s", (title, check) => check());
});

describeLessOnly("popovers", "popovers.css", [
  ".popover-colorful + .popover",
  ".popover-warning + .popover .popover-title",
  ".help.popover",
  ".help.popover.ng-hide-add",
  ".note-editor .note-popover .popover",
  ".ie8 .popover.top.in",
  ".editable-container.popover"
]);
