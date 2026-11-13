import {compatFamilyCases} from "./support/compatFamily";
import {print} from "./support/baseFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${print.title}`, () => {
  it.each(compatFamilyCases(print))("%s", (title, check) => check());
});
