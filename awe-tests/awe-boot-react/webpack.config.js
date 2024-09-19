const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const dir = path.join(__dirname, "src", "main", "resources", "webpack");

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
      {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, use: 'babel-loader'},
      {test: /\.(le|c)ss$/, use: [MiniCssExtractPlugin.loader, "css-loader", "postcss-loader", "less-loader"]},
      {test: /\.(jpg|gif|png)$/, type: 'asset/resource', generator: { filename: 'images/[hash][ext][query]'}},
      {test: /\.woff[2]*?(\?v=[0-9]\.[0-9]\.[0-9])?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}},
      {test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}}
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", "*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/specific.css"
  })]
};