const path = require("path");

// Jest is the canonical AngularJS frontend test runner. Maven and CI invoke
// `npm run test:coverage`, which writes GitLab JUnit, Sonar test execution,
// and LCOV reports under target/reports/jest.
module.exports = {
  testEnvironment: "jsdom",
  cacheDirectory: path.join(__dirname, "target", ".cache", "jest"),
  setupFilesAfterEnv: [path.join(__dirname, "src", "test", "jest", "setup.js")],
  testMatch: [
    "<rootDir>/src/test/jest/**/*.spec.js",
    "<rootDir>/src/test/jest/**/*.test.js"
  ],
  transform: {
    "^.+\\.js$": ["babel-jest", { envName: "jest" }]
  },
  collectCoverageFrom: [
    "src/main/resources/js/**/*.js",
    "!src/main/resources/js/lib/**"
  ],
  coverageDirectory: path.join(__dirname, "target", "reports", "jest", "coverage"),
  coverageReporters: ["html", "lcovonly", "text-summary"],
  reporters: [
    "default",
    [
      "jest-junit",
      {
        outputDirectory: path.join(__dirname, "target", "reports", "jest", "junit"),
        outputName: "javascriptUnitTests.xml",
        suiteName: "awe-client-angular-jest"
      }
    ],
    [
      "@casualbot/jest-sonar-reporter",
      {
        outputDirectory: path.join(__dirname, "target", "reports", "jest", "sonar"),
        outputName: "javascriptUnitTests.xml",
        relativePaths: true
      }
    ]
  ],
  moduleFileExtensions: ["js", "json"],
  moduleNameMapper: {
    "^jquery$": path.join(__dirname, "node_modules", "jquery", "dist", "jquery"),
    "^jquery-ui$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^bootstrap-tabdrop$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^highcharts/highstock$": path.join(__dirname, "src", "test", "jest", "mocks", "highchartsMock.js"),
    "^highcharts/(.*)$": path.join(__dirname, "src", "test", "jest", "mocks", "highchartsModuleMock.js"),
    "^HighchartsLocale$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^HighchartsThemes$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    ".*highcharts/i18n/highcharts\\..*$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    ".*pivotTable/i18n/pivot\\..*$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^bootstrap-datepicker/js/locales/.*$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^Tocify$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^PivotTable$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "\\.(css|less)$": path.join(__dirname, "src", "test", "jest", "mocks", "styleMock.js"),
    "\\.(jpg|jpeg|png|gif|svg|ttf|eot|woff|woff2)$": path.join(__dirname, "src", "test", "jest", "mocks", "fileMock.js")
  }
};
