const path = require("path");

// The chart utilities of the React client are shared with this client (see webpack.config.js)
const reactSourcePath = path.join(__dirname, "..", "awe-client-react", "src");

// Jest is the canonical AngularJS frontend test runner. Maven and CI invoke
// `npm run test:coverage`, which writes GitLab JUnit, Sonar test execution,
// and LCOV reports under target/reports/jest.
module.exports = {
  // In CI the job shares the runner with other jobs and may have little memory: Jest's default of one worker per CPU
  // gets workers killed (SIGKILL), so CI runs 4 workers. Local runs keep Jest's default.
  ...(process.env.CI ? {maxWorkers: 4} : {}),
  testEnvironment: "jsdom",
  cacheDirectory: path.join(__dirname, "target", ".cache", "jest"),
  setupFilesAfterEnv: [path.join(__dirname, "src", "test", "jest", "setup.js")],
  testMatch: [
    "<rootDir>/src/test/jest/**/*.spec.js",
    "<rootDir>/src/test/jest/**/*.test.js"
  ],
  // The Babel configuration of this module also compiles the shared React sources and the ECharts modules
  transform: {
    "^.+\\.js$": ["babel-jest", { envName: "jest", configFile: path.join(__dirname, ".babelrc"), babelrc: false }]
  },
  // ECharts is loaded from its CommonJS bundle (moduleNameMapper below): only its locale modules are ES modules to compile
  transformIgnorePatterns: ["/node_modules/(?!echarts/i18n/)"],
  // The shared React sources import their packages (echarts, lodash) from this module, not from awe-client-react
  moduleDirectories: [path.join(__dirname, "node_modules"), "node_modules"],
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
    "^awe-client-react/(.*)$": path.join(reactSourcePath, "$1"),
    "^echarts/core$": path.join(__dirname, "node_modules", "echarts", "dist", "echarts.js"),
    "^echarts/(charts|components|renderers)$": path.join(__dirname, "src", "test", "jest", "mocks", "echartsExtensions.js"),
    ".*pivotTable/i18n/pivot\\..*$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^bootstrap-datepicker/js/locales/.*$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^Tocify$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "^PivotTable$": path.join(__dirname, "src", "test", "jest", "mocks", "pluginMock.js"),
    "\\.(css|less)$": path.join(__dirname, "src", "test", "jest", "mocks", "styleMock.js"),
    "\\.(jpg|jpeg|png|gif|svg|ttf|eot|woff|woff2)$": path.join(__dirname, "src", "test", "jest", "mocks", "fileMock.js")
  }
};
