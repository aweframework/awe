const path = require("path");
const fs = require("fs");

const jestConfig = require("../../../jest.config");
const packageConfig = require("../../../package.json");
const babelConfig = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "..", "..", ".babelrc"), "utf8"));
const pomXml = fs.readFileSync(path.join(__dirname, "..", "..", "..", "pom.xml"), "utf8");
const rootPomXml = fs.readFileSync(path.join(__dirname, "..", "..", "..", "..", "..", "pom.xml"), "utf8");
const gitlabCi = fs.readFileSync(path.join(__dirname, "..", "..", "..", "..", "..", ".gitlab-ci.yml"), "utf8");
const currentMavenDocs = fs.readFileSync(
  path.join(__dirname, "..", "..", "..", "..", "..", "website", "docs", "config-maven.md"),
  "utf8"
);
const versionedMavenDocs = fs.readFileSync(
  path.join(__dirname, "..", "..", "..", "..", "..", "website", "versioned_docs", "version-4.11.0", "config-maven.md"),
  "utf8"
);

function collapseWhitespace(content) {
  return content.replace(/\s+/g, " ");
}

describe("Jest foundation configuration", () => {
  it("keeps a compact canonical npm script surface for Jest", () => {
    expect(packageConfig.scripts.test).toBe("jest --config jest.config.js");
    expect(packageConfig.scripts["test:coverage"]).toBe("jest --config jest.config.js --coverage");
    expect(packageConfig.scripts["test:jest"]).toBeUndefined();

    const obsoleteScopedScripts = Object.keys(packageConfig.scripts).filter(scriptName => (
      scriptName.startsWith("test:jest:")
    ));

    expect(obsoleteScopedScripts).toEqual([]);
  });

  it("publishes stable GitLab JUnit, Sonar, and LCOV report locations", () => {
    expect(jestConfig.coverageDirectory).toBe(
      path.join(__dirname, "..", "..", "..", "target", "reports", "jest", "coverage")
    );
    expect(jestConfig.coverageReporters).toContain("lcovonly");
    expect(jestConfig.reporters[1][1]).toEqual(expect.objectContaining({
      outputDirectory: path.join(__dirname, "..", "..", "..", "target", "reports", "jest", "junit"),
      outputName: "javascriptUnitTests.xml"
    }));
    expect(jestConfig.reporters[2]).toEqual([
      "@casualbot/jest-sonar-reporter",
      expect.objectContaining({
        outputDirectory: path.join(__dirname, "..", "..", "..", "target", "reports", "jest", "sonar"),
        outputName: "javascriptUnitTests.xml",
        relativePaths: true
      })
    ]);
    expect(packageConfig.devDependencies["@casualbot/jest-sonar-reporter"]).toBeDefined();
  });

  it("makes Jest canonical for npm, Maven, CI, and Sonar reports", () => {
    const normalizedPom = collapseWhitespace(pomXml);
    const normalizedRootPom = collapseWhitespace(rootPomXml);

    expect(normalizedPom).toContain("<id>jest</id> <goals> <goal>npm</goal> </goals> <phase>test</phase>");
    expect(normalizedPom).toContain("<arguments>run test:coverage</arguments>");
    expect(pomXml).toContain("target/reports/jest/sonar/javascriptUnitTests.xml");
    expect(pomXml).toContain("target/reports/jest/coverage/lcov.info");
    expect(normalizedRootPom).toContain(
      "${project.basedir}/awe-framework/awe-client-angular/target/reports/jest/sonar/javascriptUnitTests.xml"
    );
    expect(gitlabCi).toContain("awe-framework/awe-client-angular/target/reports/jest/");
    expect(gitlabCi).toContain("awe-framework/awe-client-angular/target/reports/jest/junit/*.xml");
  });

  it("documents standard npm test and coverage commands without legacy Jest aliases", () => {
    [currentMavenDocs, versionedMavenDocs].forEach(mavenDocs => {
      expect(mavenDocs).toContain('"test": "jest --config jest.config.js"');
      expect(mavenDocs).toContain('"test:coverage": "jest --config jest.config.js --coverage"');
      expect(mavenDocs).not.toContain("test:jest");
      expect(mavenDocs).not.toContain("npm run test:jest");
    });
  });

  it("does not reintroduce Karma runner wiring or dependencies in the AngularJS client", () => {
    const dependencyNames = [
      ...Object.keys(packageConfig.dependencies || {}),
      ...Object.keys(packageConfig.devDependencies || {})
    ];

    expect(dependencyNames.filter(dependencyName => dependencyName.startsWith("karma"))).toEqual([]);
    expect(packageConfig.scripts.karma).toBeUndefined();
    expect(pomXml).not.toContain("<goal>karma</goal>");
    expect(pomXml).not.toContain("target/reports/karma/");
    expect(rootPomXml).not.toContain("target/reports/karma/");
  });

  it("discovers source-traceable Jest specs instead of relying on npm bucket scripts", () => {
    expect(jestConfig.testMatch).toEqual(expect.arrayContaining([
      "<rootDir>/src/test/jest/**/*.spec.js",
      "<rootDir>/src/test/jest/**/*.test.js"
    ]));
  });

  it("mirrors webpack aliases and maps non-JS resources for jsdom-safe suites", () => {
    expect(jestConfig.moduleNameMapper).toEqual(expect.objectContaining({
      "^jquery$": expect.stringContaining(path.join("node_modules", "jquery", "dist", "jquery")),
      "^bootstrap-tabdrop$": expect.stringContaining(path.join("src", "test", "jest", "mocks", "pluginMock.js")),
      "^highcharts/highstock$": expect.stringContaining(path.join("src", "test", "jest", "mocks", "highchartsMock.js")),
      "^highcharts/(.*)$": expect.stringContaining(path.join("src", "test", "jest", "mocks", "highchartsModuleMock.js")),
      "\\.(css|less)$": expect.stringContaining(path.join("src", "test", "jest", "mocks", "styleMock.js")),
      "\\.(jpg|jpeg|png|gif|svg|ttf|eot|woff|woff2)$": expect.stringContaining(
        path.join("src", "test", "jest", "mocks", "fileMock.js")
      )
    }));
  });

  it("uses a Jest Babel environment without Karma's Istanbul preprocessing", () => {
    expect(jestConfig.transform["^.+\\.js$"][1]).toEqual({ envName: "jest" });
    expect(babelConfig.env.jest).toEqual({
      compact: false,
      plugins: []
    });
  });

  it("keeps migrated browser-selection tests declared in their Jest spec", () => {
    const browserSelectionEntrypoint = fs.readFileSync(
      path.join(__dirname, "karma", "browserSelection.jest.spec.js"),
      "utf8"
    );

    expect(browserSelectionEntrypoint).toContain('describe("karma browser selection"');
    expect(browserSelectionEntrypoint).not.toContain("browserSelection.spec.js");
  });

  it("keeps Jest setup free of Jasmine compatibility bridge APIs", () => {
    expect(global.jasmine).toBeUndefined();
    expect(window.jasmine).toBeUndefined();
    expect(window.spyOn).toBeUndefined();
    expect(expect(true).toBeTrue).toBeUndefined();
    expect(expect(false).toBeFalse).toBeUndefined();
  });

  it("preserves runtime shims required by migrated AngularJS suites", () => {
    const highchartsMock = require("./mocks/highchartsMock.js");
    const highchartsModuleMock = require("./mocks/highchartsModuleMock.js");
    const input = global.$("<input />");
    const target = { nested: { value: "original" } };

    expect(highchartsMock.chart()).toEqual({ destroy: expect.any(Function), reflow: expect.any(Function) });
    expect(highchartsModuleMock(highchartsMock)).toBe(highchartsMock);

    global._.merge(target, { nested: { extra: true } });
    expect(target).toEqual({ nested: { value: "original", extra: true } });

    input.autoNumeric({ aSep: ".", aDec: ",", mDec: 2 });
    input.autoNumeric("set", 1200.21);
    expect(input.val()).toBe("1.200,21");
    expect(input.autoNumeric("get")).toBe("1200.21");
  });
});
