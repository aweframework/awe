import {compatFamilyCases} from "./support/compatFamily";
import {thumbnails} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${thumbnails.title}`, () => {
  it.each(compatFamilyCases(thumbnails))("%s", (title, check) => check());
});
