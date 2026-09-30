const {merge} = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {
  mode: 'development',
  devtool: 'eval-cheap-module-source-map',
  optimization: {
    splitChunks: {
      cacheGroups: {
        primereact: {
          test: /[\\/]node_modules[\\/]/,
          name: "primereact",
          chunks: "all",
          enforce: true,
        },
      },
    },
  },
});
