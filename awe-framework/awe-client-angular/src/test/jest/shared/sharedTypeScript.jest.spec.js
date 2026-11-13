import { translateLabel } from "awe-client-react/utilities/labels";

// The React sources shared with this client (the chart utilities and what they import) are being converted to
// TypeScript (#808): this client must compile them like the React client does.
describe("awe-client-angular/src/test/jest/shared/sharedTypeScript.jest.spec.js", () => {
  it("imports a shared React utility written in TypeScript", () => {
    expect(translateLabel("HELLO {0}", (label) => label.toLowerCase(), "Pablo")).toBe("hello Pablo");
  });
});
