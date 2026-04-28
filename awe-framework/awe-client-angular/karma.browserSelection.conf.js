const path = require("path");

const tests = path.join(__dirname, "src", "test", "js", "karma", "tests.js");

module.exports = (config) => {
  config.set({
    basePath: path.join(__dirname),
    frameworks: ["jasmine"],
    reporters: ["spec"],
    browsers: ["ChromeHeadless"],
    singleRun: true,
    files: [tests],
    preprocessors: {
      [tests]: ["webpack", "sourcemap"]
    },
    webpack: {
      devtool: "inline-source-map",
      module: {
        rules: [{
          test: /\.jsx?$/,
          loader: "babel-loader",
          exclude: /node_modules|webpack/
        }]
      },
      resolve: {
        extensions: [".js", "*"],
        modules: [
          path.join(__dirname, "src"),
          "node_modules"
        ]
      }
    },
    specReporter: {
      suppressErrorSummary: false,
      suppressFailed: false,
      suppressPassed: false,
      suppressSkipped: true,
      showSpecTiming: true,
      failFast: false
    }
  });
};
