const fs = require("fs");
const path = require("path");
const template = require("lodash/template");

// The page template goes through the default loader of HtmlWebpackPlugin (lodash template with <%= %> only) in the
// applications: the Thymeleaf expressions of the splash logo have to reach Thymeleaf untouched
describe("awe-react-client/test/js/packaging/templateTest.js", () => {
  it("keeps the Thymeleaf splash logo expressions through the HtmlWebpackPlugin loader", () => {
    const source = fs.readFileSync(path.resolve(__dirname, "../../../src/template.html"), "utf8");

    const output = template(source, {interpolate: /<%=([\s\S]+?)%>/g, variable: "data"})({});

    expect(output).toBe(source);
    expect(output).toContain("th:style=\"${splashLogoUrl} ? |--awe-splash-logo: url('${splashLogoUrl}'); " +
      "--awe-splash-liquid: url('${splashLogoUrl}') center / contain no-repeat|\"");
  });
});
