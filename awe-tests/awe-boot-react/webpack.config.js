const path = require("path");
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ThymeLeafPlugin = require('awe-react-client/plugins/thymeleaf-plugin');

module.exports = {
  entry : {
    "bundle" : path.resolve(__dirname, "src", "js", "main.js")
  },
  cache: {
    type: 'filesystem',
    buildDependencies: {
      config: [__filename]
    }
  },
  output : {
    filename : "js/[name].js",
    path: path.resolve(__dirname, 'target', 'classes', 'static'),
  },
  module : {
    rules : [
      {test: /\.js$/, exclude: /node_modules/, enforce: 'pre', use: ['source-map-loader']},
      {test: /\.(tsx|ts|jsx|js)$/, exclude: /node_modules/, use: 'babel-loader'},
      {test: /\.css$/, use: [MiniCssExtractPlugin.loader, "css-loader", {loader: "postcss-loader", options: {postcssOptions: {config: path.resolve(__dirname, 'postcss.config.js')}}}]},
      {test: /\.less$/, use: [MiniCssExtractPlugin.loader, "css-loader", {loader: "postcss-loader", options: {postcssOptions: {config: path.resolve(__dirname, 'postcss.config.js')}}}, "less-loader"]},
      {test: /\.(jpg|gif|png|svg)$/, type: 'asset/resource', generator: { filename: 'images/[hash][ext][query]'}},
      {test: /\.(ttf|eot|woff(2)?)(\?v=\d+\.\d+\.\d+)?$/, type: 'asset/resource', generator: {filename: "fonts/[hash][ext][query]"}},
    ]
  },
  resolve : {
    symlinks: false,
    modules: [
      path.resolve(__dirname, 'node_modules'),
      'node_modules'
    ],
    extensions : [ ".tsx", ".ts", ".jsx", ".js", ".css", ".less" ]
  },
  plugins : [ new MiniCssExtractPlugin({
    filename: "css/[name].css"
  }),
    new HtmlWebpackPlugin({
      template: require.resolve('awe-react-client/template.html'),
      filename: '../templates/index.html',
      inject: "body",
      publicPath: './'
    }),
    new ThymeLeafPlugin({ htmlWebpackPlugin: HtmlWebpackPlugin }),
    new CopyPlugin({
      patterns: [
        {from: path.resolve("node_modules/awe-react-client/static")},
        {from: "*.stg", context: path.resolve(__dirname, "node_modules/awe-react-client"),
          to: path.resolve(__dirname, 'target', 'classes', 'templates', "awe"), toType: 'dir'}
      ]
    })
  ]
};
