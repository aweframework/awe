const path = require("path");
const webpack = require("webpack");

const tests = path.join(__dirname, "src", "test", "js", "contracts", "tests.js");
const libPath = path.resolve(__dirname, "src", "main", "resources", "js", "lib");

module.exports = config => {
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
        extensions: [".js", ".css", ".less", "*"],
        alias: {
          jquery: path.resolve(__dirname, "node_modules", "jquery", "dist", "jquery"),
          "bootstrap-tabdrop": path.resolve(libPath, "bootstrap-tabdrop", "src", "js", "bootstrap-tabdrop"),
          HighchartsLocale: path.resolve(libPath, "highcharts", "i18n", "highcharts-lang"),
          HighchartsThemes: path.resolve(libPath, "highcharts", "themes", "all"),
          Tocify: path.resolve(libPath, "tocify", "jquery.tocify"),
          PivotTable: path.resolve(libPath, "pivotTable", "pivotTable")
        },
        modules: [
          path.join(__dirname, "src"),
          "node_modules"
        ]
      },
      plugins: [
        new webpack.ProvidePlugin({
          jQuery: "jquery",
          $: "jquery",
          "window.jQuery": "jquery",
          "window.$": "jquery",
          Highcharts: "highcharts/highstock",
          HighchartsLocale: "HighchartsLocale",
          "window.constructor": "constructor"
        })
      ]
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
