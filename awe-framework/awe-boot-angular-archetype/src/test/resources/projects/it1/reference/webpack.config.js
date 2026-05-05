const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];
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
    devtool : isProduction ? false : "source-map",
    entry : {
      "specific" : path.join(dir, "app.config.js")
    },
    output : {
      filename : "js/[name].js",
      path: path.join(__dirname, 'target', 'classes', 'static'),
      publicPath : "../"
    },
    module : {
      rules : [
        { test: /\.jsx?$/, exclude: /node_modules/, use: [{loader: 'babel-loader', options: { envName: mode }}]},
        { test : /\.css$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader"]},
        { test : /\.less$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader", {
            loader: "less-loader", options: { lessOptions: { plugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ] }, sourceMap: false }}]},
        { test : /\.(jpg|gif|png)$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './images/[hash][ext][query]' }},
        { test : /\.woff[2]*?(\?v=\d+\.\d+\.\d+)?$/, type: 'asset', parser: { dataUrlCondition: { maxSize: 100000 } }, generator: { filename: './fonts/[hash][ext][query]', dataUrl: content => `data:application/font-woff;base64,${content.toString('base64')}` }},
        { test : /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: { filename: './fonts/[hash][ext][query]' }},
      ]
    },
    resolve : {
      extensions : [ ".js", ".css", ".less", "*" ]
    },
    plugins : [ new MiniCssExtractPlugin({
      filename: "css/specific.css"
    })]
  };
};
