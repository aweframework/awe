const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');

const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");

module.exports = {
  mode: process.env.NODE_ENV,
  devtool : "source-map",
  entry : {
    "bundle-tools" : path.join(dir, "tools.config.js")
  },
  output : {
    filename : "js/[name].js",
    path: path.join(__dirname, 'target', 'classes', 'static'),
    publicPath : "../"
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
  externals: {
    aweApplication: 'aweApplication'
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", "*" ],
    alias : {
      "angular-filemanager" : "angular-filemanager-fkoester"
    }
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/file-manager-styles.css"
  })]
};
