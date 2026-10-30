import {compatFamilyCases, describeLessOnly} from "./support/compatFamily";
import {tooltip} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${tooltip.title}`, () => {
  it.each(compatFamilyCases(tooltip))("%s", (title, check) => check());
});

describeLessOnly("tooltip", "tooltip.css", [
  ".tooltip-warning + .tooltip .tooltip-inner",
  ".slider .tooltip.top",
  ".grid-cell-tooltip.tooltip",
  ".grid-cell-tooltip.tooltip .tooltip-inner"
]);
