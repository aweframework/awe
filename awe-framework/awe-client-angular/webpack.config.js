const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin');

const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const srcPath = path.resolve(__dirname, "src", "main", "resources", "js", "awe");
const libPath = path.resolve(__dirname, "src", "main", "resources", "js", "lib");

const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];

module.exports = {
  mode: process.env.NODE_ENV,
  devtool: "source-map",
  entry: {
    "bundle": path.join(dir, "awe.config.js"),
    "locals-es-ES": path.join(dir, "locals-es-ES.config.js"),
    "locals-en-GB": path.join(dir, "locals-en-GB.config.js"),
    "locals-eu-ES": path.join(dir, "locals-eu-ES.config.js"),
    "locals-fr-FR": path.join(dir, "locals-fr-FR.config.js")
  },
  output: {
    filename: "js/[name].js",
    path: path.join(__dirname, 'target', 'classes', 'static'),
    publicPath: "../"
  },
  optimization: {
    splitChunks: {
      name: "bundle",
      cacheGroups: {
        commons: {
          test: /[\\/]node_modules[\\/]/,
          name: 'commons',
          chunks: 'all'
        }
      }
    }
  },
  module : {
    rules : [
      { test: require.resolve('jquery'), use: [{loader: 'expose-loader', options: {exposes: ['jQuery', '$']}}]},
      { test: /\.jsx?$/, exclude: /node_modules/, use: [{loader: 'babel-loader'}]},
      { test : /\.css$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader"]},
      { test : /\.less$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader", {
          loader: "less-loader", options: { lessOptions: { plugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ] }, sourceMap: false }}]},
      { test : /\.(jpg|gif|png)$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './images/[hash][ext][query]' }},
      { test : /\.woff[2]*?(\?v=\d+\.\d+\.\d+)?$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './fonts/[hash][ext][query]', dataUrl: content => `data:application/font-woff;base64,${content.toString('base64')}` }},
      { test : /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: { filename: './fonts/[hash][ext][query]' }},
    ]
  },
  resolve: {
    extensions: [".js", ".css", ".less", "*"],
    alias: {
      "jquery": path.resolve(__dirname, "node_modules", "jquery", "dist", "jquery"),
      "bootstrap-tabdrop": path.resolve(libPath, "bootstrap-tabdrop", "src", "js", "bootstrap-tabdrop"),
      "HighchartsLocale": path.resolve(libPath, "highcharts", "i18n", "highcharts-lang"),
      "HighchartsThemes": path.resolve(libPath, "highcharts", "themes", "all"),
      "Tocify": path.resolve(libPath, "tocify", "jquery.tocify"),
      "PivotTable": path.resolve(libPath, "pivotTable", "pivotTable")
    },
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: "css/styles.css"
    }),
    new LodashModuleReplacementPlugin,
    new webpack.ProvidePlugin({
      "Highcharts": "highcharts/highstock",
      "HighchartsLocale": "HighchartsLocale",
      "window.constructor": "constructor"
    })
  ]
};
