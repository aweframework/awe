const {merge} = require('webpack-merge');
const common = require('./webpack.config.js');

module.exports = merge(common, {
  mode: 'development',
  devtool: "eval-cheap-module-source-map",
  optimization: {
    minimize: false,
    splitChunks: {
      cacheGroups: {
        "commons": {
          test: /[\\/]node_modules[\\/]((?!(awe\-react\-client|primereact)))/,
          name: 'commons',
          // Initial chunks only: the packages the client loads on demand (charts, maps, editors) keep their own chunks
          chunks: 'initial'
        },
      }
    }
  }
});
