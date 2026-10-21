const os = require("os");
const path = require("path");

// The chart utilities of the React client are shared with this client (see webpack.config.js)
const reactSourcePath = path.join(__dirname, "..", "awe-client-react", "src");

// Jest is the canonical AngularJS frontend test runner. Maven and CI invoke
// `npm run test:coverage`, which writes GitLab JUnit, Sonar test execution,
// and LCOV reports under target/reports/jest.
module.exports = {
  // In CI the job shares the runner with other jobs: Jest's default of one worker per CPU gets workers killed (SIGKILL)
  // when memory is short, and saturates the CPU of a small runner (a 4 vCPU VM on a host shared with other VMs, with a
  // low-power 10 W laptop CPU, shows 10-16 % steal with 4 workers). CI runs half the CPUs as workers, at most 4 (the
  // previous fixed value) and at least 1. Even then a slow shared runner can push a slow suite (the LESS compilation of
  // styles/skeleton) past the 5 s default, so CI allows 15 s per test and hook. Local runs keep Jest's defaults, so a
  // slow test still shows up.
  ...(process.env.CI
    ? {maxWorkers: Math.min(4, Math.max(1, Math.floor(os.availableParallelism() / 2))), testTimeout: 15000}
    : {}),
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
