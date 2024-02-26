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
      {test: /\.(jpg|gif|png)$/, use: {loader: 'url-loader', options: {limit: 10240, name: './images/[hash].[ext]'}}},
      {
        test: /\.woff[2]*?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: {
          loader: "url-loader",
          options: {limit: 10000, mimetype: 'application/font-woff', name: './fonts/[hash].[ext]'}
        }
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        use: {loader: "file-loader", options: {name: "./fonts/[hash].[ext]"}}
      }
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", "*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/specific.css"
  })]
};