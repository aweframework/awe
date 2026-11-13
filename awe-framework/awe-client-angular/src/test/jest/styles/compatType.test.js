import {compatFamilyCases} from "./support/compatFamily";
import {type} from "./support/baseFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${type.title}`, () => {
  it.each(compatFamilyCases(type))("%s", (title, check) => check());
});
