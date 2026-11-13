import {compatFamilyCases} from "./support/compatFamily";
import {dropdowns} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${dropdowns.title}`, () => {
  it.each(compatFamilyCases(dropdowns))("%s", (title, check) => check());
});
