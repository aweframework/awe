const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ThymeLeafPlugin = require('awe-react-client/plugins/thymeleaf-plugin');

module.exports = {
  entry : {
    "bundle" : path.resolve(__dirname, "src", "js", "main.js")
  },
  output : {
    clean: true,
    filename : "js/[name].js",
    path: path.resolve(__dirname, 'target', 'classes', 'static'),
  },
  module : {
    rules : [
      {test: /\.js$/, enforce: 'pre', use: ['source-map-loader']},
      {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, use: 'babel-loader'},
      {test: /\.(le|c)ss$/, use: [MiniCssExtractPlugin.loader, "css-loader", "postcss-loader", "less-loader"]},
      {test: /\.(jpg|gif|png|svg)$/, type: 'asset/resource', generator: { filename: 'images/[hash][ext][query]'}},
      {test: /\.(ttf|eot|woff(2)?)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}},
    ]
  },
  resolve : {
    extensions : [ ".js", ".css", ".less", ".*" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/[name].css"
  }),
    new HtmlWebpackPlugin({
      template: require.resolve('awe-react-client/template.html'),
      filename: 'index.html',
      inject: "body",
    }),
    new ThymeLeafPlugin(),
    new CopyPlugin({
      patterns: [
        {from: path.resolve("node_modules/awe-react-client/static")}
      ]
    })
  ]
};