const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
  mode: 'production',
  devtool: 'source-map',
  performance: {
    hints: false
  },
  optimization: {
    minimize: true,
    splitChunks: {
      name: "main",
      cacheGroups: {
        commons: {
          // The AWE theme of PrimeReact stays in css/main.css, the stylesheet the applications import
          test: /[\\/]node_modules[\\/](?!primereact[\\/]resources[\\/]themes[\\/])/,
          name: 'commons',
          chunks: 'all'
        }
      }
    }
  }
});
