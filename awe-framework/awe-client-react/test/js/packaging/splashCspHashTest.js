const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

// The page templates carry one inline script (the colour mode of the splash): the migration guide gives its hash for a
// Content-Security-Policy, so every template has to carry the same script and the guides the hash of that script.
// It is written as the minifier of the React build leaves it, so the browser gets those exact bytes in both engines
const root = path.resolve(__dirname, "../../../../..");
const read = file => fs.readFileSync(path.join(root, file), "utf8");
const script = file => read(file).match(/<script>(try\{var m=localStorage\.getItem\("awe-theme-mode"\)[^<]*)<\/script>/)[1];
const hash = source => crypto.createHash("sha256").update(source, "utf8").digest("base64");

describe("awe-react-client/test/js/packaging/splashCspHashTest.js", () => {
  it("documents the Content-Security-Policy hash of the inline script of both page templates", () => {
    const react = script("awe-framework/awe-client-react/src/template.html");
    const angular = script("awe-framework/awe-client-angular/src/main/resources/index.html");

    expect(angular).toBe(react);
    expect(read("website/docs/guides/v5-migration.md")).toContain(`sha256-${hash(react)}`);
    expect(read("website/i18n/es/docusaurus-plugin-content-docs/current/guides/v5-migration.md"))
      .toContain(`sha256-${hash(react)}`);
  });
});
