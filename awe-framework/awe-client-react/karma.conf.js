const path = require("path");
const tests = path.join(__dirname, "test", "js", "tests.js");

// Fix webpack for karma
module.exports = (config) => {
  const grepArgIndex = process.argv.indexOf("--grep");
  const grepArg = process.argv.find((arg) => arg.startsWith("--grep="));
  const grepValue = grepArg
    ? grepArg.split("=").slice(1).join("=")
    : (grepArgIndex !== -1 ? process.argv[grepArgIndex + 1] : null);

  config.set({
    basePath: path.join(__dirname),
    frameworks: ['jasmine'],
    reporters: ['spec', 'sonarqubeUnit', 'coverage-istanbul', 'junit'],
    //concurrency: 1,
    browserConsoleLogOptions: {level: 'info', format: '%b %T: %m', terminal: true},
    reportSlowerThan: 500,
    singleRun: true,
    files: [tests],
    client: {
      args: grepValue ? [`--grep=${grepValue}`] : []
    },
    preprocessors: {
      [tests]: ['webpack', 'sourcemap']
    },
    browsers: ["ChromeHeadlessCI", "FirefoxHeadlessCI"],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: "ChromeHeadless",
        flags: ["--no-sandbox", '--disable-gpu', '--remote-debugging-port=9222', "--disable-dev-shm-usage", '--disable-software-rasterizer']
      },
      FirefoxHeadlessCI: {
        base: "Firefox",
        flags: ["-headless"]
      }
    },
    webpack: {
      devtool: 'inline-source-map',
      mode: 'development',
      module: {
        rules: [
          {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, parser: {import: 'auto'}, use: {loader: 'babel-loader', options: {presets: ['@babel/preset-env', '@babel/preset-react']}}},
          {test: /\.css$/, use: ["css-loader"]},
          {test: /\.less$/, use: ["css-loader", "less-loader"]}
        ]
      },
      resolve: {
        extensions: [".js", ".jsx", ".css", ".less", "*"]
      },
    },
    specReporter: {
      suppressErrorSummary: false, // do not print error summary
      suppressFailed: false,       // do not print information about failed tests
      suppressPassed: false,       // do not print information about passed tests
      suppressSkipped: true,       // do not print information about skipped tests
      showSpecTiming: true,        // print the time elapsed for each spec
      failFast: false              // test would finish with error when a first fail occurs.
    },
    coverageReporter: {
      includeAllSources: true,
      reporters: [
        {
          type: 'lcov',
          dir:   path.join(__dirname, "target", "reports", "karma", "coverage"),
        }
      ]
    },
    coverageIstanbulReporter: {
      dir: path.join(__dirname, "target", "reports", "karma", "coverage"),
      // reports can be any that are listed here: https://github.com/istanbuljs/istanbuljs/tree/aae256fb8b9a3d19414dcf069c592e88712c32c6/packages/istanbul-reports/lib
      reports: ['html', 'lcovonly', 'text-summary'],

      // Combines coverage information from multiple browsers into one report rather than outputting a report
      // for each browser.
      combineBrowserReports: true,

      // if using webpack and pre-loaders, work around webpack breaking the source path
      fixWebpackSourcePaths: true,

      // Omit files with no statements, no functions and no branches from the report
      skipFilesWithNoCoverage: false,

      // Most reporters accept additional config options. You can pass these through the `report-config` option
      'report-config': {
        // all options available at: https://github.com/istanbuljs/istanbuljs/blob/aae256fb8b9a3d19414dcf069c592e88712c32c6/packages/istanbul-reports/lib/html/index.js#L135-L137
        html: {
          // outputs the report in ./coverage/html
          subdir: 'html'
        }
      },
      verbose: true // output config used by istanbul for debugging
    },
    sonarQubeUnitReporter: {
      sonarQubeVersion: 'LATEST',
      outputFile: path.join("target", "reports", "karma", "junit", "javascriptUnitTests.xml"),
      overrideTestDescription: false,
      testFilePattern: '.js*',
      useBrowserName: false
    },
    junitReporter: {
      outputDir: path.join("target", "reports", "junit"),
      useBrowserName: false, // add browser name to report and classes names
    }
  });
};
