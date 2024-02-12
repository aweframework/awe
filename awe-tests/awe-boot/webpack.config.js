const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const LessPluginAutoPrefix = require('less-plugin-autoprefix');
const dir = path.join(__dirname, "src", "main", "resources", "webpack");
const styleDir = path.resolve(__dirname, "src", "main", "resources", "less");
const autoprefixerBrowsers = ['last 2 versions', '> 1%', 'opera 12.1', 'bb 10', 'android 4', 'IE 10'];

module.exports = {
  mode: process.env.NODE_ENV,
  devtool : "source-map",
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
      { test: /\.jsx?$/, exclude: /node_modules/, use: [{loader: 'babel-loader'}]},
      { test : /\.css$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader"]},
      { test : /\.less$/, include : [ styleDir ], use : [MiniCssExtractPlugin.loader, "css-loader", {
          loader: "less-loader", options: { lessPlugins: [ new LessPluginAutoPrefix({browsers: autoprefixerBrowsers}) ], sourceMap: true}}]},
      { test : /\.(jpg|gif|png)$/, use: [{loader: 'url-loader', options: {limit: 100000, name: './images/[hash].[ext]'}}]},
      { test : /\.woff[2]*?(\?v=\d+\.\d+\.\d+)?$/, use: [{loader: 'url-loader', options: {limit: 100000, mimetype: 'application/font-woff', name: './fonts/[hash].[ext]'}}]},
      { test : /\.(ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/, use: [{loader: 'file-loader', options: {name: './fonts/[hash].[ext]'}}]},
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", "*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/specific.css",
    disable: false,
    allChunks: true
  })]
};