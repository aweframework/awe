const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin');
const GeneratePackageJsonPlugin = require('generate-package-json-webpack-plugin');
const CopyPlugin = require("copy-webpack-plugin");
const PACKAGE = require('./package.json');
const {resolvePackageVersion} = require('./package-version.cjs');

const basePackage = {
  ...PACKAGE,
  "version": resolvePackageVersion(),
  "main": "./js/main.js",
  "engines": {
    "node": ">= 16"
  },
  "scripts": undefined,
  "devDependencies": undefined,
};
const nodeExternals = require('webpack-node-externals');

module.exports = {
  entry: {
    main: path.join(__dirname, "src", "index.js")
  },
  cache: {
    type: 'filesystem',
    buildDependencies: {
      config: [__filename]
    }
  },
  output: {
    filename: "js/[name].js",
    path: path.resolve(__dirname, "dist"),
    libraryTarget: 'commonjs2',
    publicPath: '../',
    clean: true
  },
  //target: 'node',
  externalsPresets: { node: true },
  externals: [nodeExternals()],
  module: {
    rules: [
      {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, use: {loader: 'babel-loader', options: {presets: ['@babel/preset-env', '@babel/preset-react']}}},
      {test: /\.css$/, use: [MiniCssExtractPlugin.loader, "css-loader"]},
      {test: /\.less$/, use: [MiniCssExtractPlugin.loader, "css-loader", "less-loader"]},
      {test: /\.(jpg|gif|png|svg)$/, type: 'asset/resource', generator: { filename: 'images/[hash][ext][query]'}},
      {test: /\.(ttf|eot|woff(2)?)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}},
    ]
  },
  resolve: {
    extensions: [".tsx", ".ts", ".jsx", ".js", ".css", ".less"]
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: "css/[name].css"
    }),
    new LodashModuleReplacementPlugin,
    new GeneratePackageJsonPlugin(basePackage, {
      // These runtime CSS packages are already declared in package.json, but GPJWP
      // can't resolve them reliably when they are discovered through CSS imports.
      // Excluding them from the module scan avoids noisy false-positive warnings,
      // while basePackage preserves them in the generated dist/package.json.
      excludeDependencies: ['primeicons', 'primeflex', 'font-awesome', 'material-icons']
    }),
    new CopyPlugin({
      patterns: [
        {from: "src/template.html", to: "[name][ext]"},
        {from: "src/*.stg", to: "[name][ext]"},
        {from: "src/templates.stg", to: "[name][ext]"},
        {from: "src/plugins", to: "plugins/[name][ext]"},
        {from: "src/static", to: "static/"},
        {from: "README.md", to: "[name][ext]"},
      ]
    })
  ]
};
