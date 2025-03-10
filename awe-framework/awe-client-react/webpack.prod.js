const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');
const JSDocPlugin = require('jsdoc-webpack-plugin');

module.exports = merge(common, {
  mode: 'production',
  devtool: 'source-map',
  optimization: {
    minimize: true,
    splitChunks: {
      name: "main",
      cacheGroups: {
        commons: {
          test: /[\\/]node_modules[\\/]/,
          name: 'commons',
          chunks: 'all'
        }
      }
    }
  },
  plugins: [
    new JSDocPlugin({
      conf: 'jsdoc.conf.json',
      cwd: "./"
    })
  ]
});