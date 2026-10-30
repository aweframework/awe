import {compatFamilyCases} from "./support/compatFamily";
import {scaffolding} from "./support/baseFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${scaffolding.title}`, () => {
  it.each(compatFamilyCases(scaffolding))("%s", (title, check) => check());
});
