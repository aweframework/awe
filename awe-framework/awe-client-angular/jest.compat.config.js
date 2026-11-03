const path = require("path");
const base = require("./jest.config");

const REPORTS = path.join(__dirname, "target", "reports", "jest-compat");

// The equivalence guards of the Bootstrap 3 to Tailwind compat migration (website/docs/guides/tailwind-compat-guide.md).
// They compile the whole LESS and Tailwind builds, so `npm test` leaves them out and CI runs them (`npm run test:compat`)
// on develop, master and the nightly schedule only. Their reports go to target/reports/jest-compat, so they do not
// overwrite the ones of `npm test`.
module.exports = {
  ...base,
  // A slow shared runner can push the tests that compile the whole LESS (designTokens, skeleton) far past the 5 s
  // default (over 15 s seen in CI), so CI allows 30 s per test and hook. These suites leave a lot of memory behind, so CI
  // also restarts a worker that keeps more than 1 GB after a test file (a long run of them exhausted the heap of a
  // worker). Local runs keep Jest's defaults, so a slow test still shows up.
  ...(process.env.CI ? {testTimeout: 30000, workerIdleMemoryLimit: "1GB"} : {}),
  testMatch: ["<rootDir>/src/test/jest/styles/**/*.test.js"],
  testPathIgnorePatterns: ["/node_modules/"],
  coverageDirectory: path.join(REPORTS, "coverage"),
  reporters: [
    "default",
    [
      "jest-junit",
      {
        outputDirectory: path.join(REPORTS, "junit"),
        outputName: "javascriptUnitTests.xml",
        suiteName: "awe-client-angular-jest-compat"
      }
    ],
    [
      "@casualbot/jest-sonar-reporter",
      {
        outputDirectory: path.join(REPORTS, "sonar"),
        outputName: "javascriptUnitTests.xml",
        relativePaths: true
      }
    ]
  ]
};
