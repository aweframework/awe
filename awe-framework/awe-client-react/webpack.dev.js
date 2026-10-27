const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
  mode: 'development',
  devtool: 'eval-cheap-module-source-map',
  optimization: {
    splitChunks: {
      cacheGroups: {
        primereact: {
          // The AWE theme of PrimeReact stays in css/main.css, the stylesheet the applications import
          test: /[\\/]node_modules[\\/](?!primereact[\\/]resources[\\/]themes[\\/])/,
          name: "primereact",
          chunks: "all",
          enforce: true,
        },
      },
    },
  },
});
