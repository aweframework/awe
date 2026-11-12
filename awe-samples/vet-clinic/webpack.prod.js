const {merge} = require('webpack-merge');
const common = require('./webpack.config.js');

module.exports = merge(common, {
  mode: 'production',
  devtool: "source-map",
  performance: {
    hints: false
  },
  optimization: {
    minimize: true,
    splitChunks: {
      name: "main",
      cacheGroups: {
        "commons": {
          test: /[\\/]node_modules[\\/]((?!(awe\-react\-client|primereact)))/,
          name: 'commons',
          chunks: 'all'
        },
      }
    }
  }
});
