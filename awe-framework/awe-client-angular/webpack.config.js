const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin');

const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const srcPath = path.resolve(__dirname, "src", "main", "resources", "js", "awe");
const libPath = path.resolve(__dirname, "src", "main", "resources", "js", "lib");
// The chart utilities of the React client (ECharts option builder, events, SVG render) are shared with this client
const reactSourcePath = path.resolve(__dirname, "..", "awe-client-react", "src");

// Modern browsers only: no Internet Explorer nor legacy mobile browsers
const autoprefixerBrowsers = ['last 2 versions', '> 0.5%', 'not dead', 'not ie > 0', 'not op_mini all'];
const validModes = new Set(["development", "production"]);

const resolveMode = (env = {}, argv = {}) => {
  const cliMode = argv.mode;
  const envMode = env.NODE_ENV || env.mode || process.env.NODE_ENV;

  if (validModes.has(cliMode)) {
    return cliMode;
  }

  if (validModes.has(envMode)) {
    return envMode;
  }

  return "production";
};

module.exports = (env = {}, argv = {}) => {
  const mode = resolveMode(env, argv);
  const isProduction = mode === "production";

  return {
    mode,
    devtool: isProduction ? false : "source-map",
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
        { test: /\.jsx?$/, exclude: /node_modules/, use: [{loader: 'babel-loader', options: { envName: mode }}]},
        // The shared React sources written in TypeScript (#808): the Babel configuration of this module strips their types
        { test: /\.ts$/, include: [ reactSourcePath ], use: [{loader: 'babel-loader', options: {
            envName: mode, configFile: path.resolve(__dirname, ".babelrc"), babelrc: false }}]},
        { test : /\.css$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader"]},
        { test : /\.less$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader", {
            loader: "less-loader", options: { lessOptions: { plugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ] }, sourceMap: false }}]},
        { test : /\.(jpg|gif|png)$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './images/[hash][ext][query]' }},
        // Fonts are separate files (never inlined into styles.css): the browser downloads only the formats it uses
        { test : /\.(woff2?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: { filename: './fonts/[hash][ext][query]' }},
      ]
    },
    resolve: {
      extensions: [".js", ".ts", ".css", ".less", "*"],
      // The shared React sources import their packages (echarts, lodash) from this module, not from awe-client-react
      modules: [path.resolve(__dirname, "node_modules"), "node_modules"],
      alias: {
        "awe-client-react": reactSourcePath,
        "jquery": path.resolve(__dirname, "node_modules", "jquery", "dist", "jquery"),
        "bootstrap-tabdrop": path.resolve(libPath, "bootstrap-tabdrop", "src", "js", "bootstrap-tabdrop"),
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
        "window.constructor": "constructor"
      })
    ]
  };
};
