const {merge} = require('webpack-merge');
const common = require('./webpack.config.js');
const CompressionPlugin = require('compression-webpack-plugin');
const zlib = require('zlib');

module.exports = merge(common, {
  mode: 'production',
  // Brotli and gzip copies of the scripts, styles and SVG files: the AWE server sends them to the browsers that accept
  // them, so they are downloaded compressed without compressing them on each request (#965)
  plugins: [
    new CompressionPlugin({
      test: /\.(js|css|svg)(\?.*)?$/, threshold: 1024, algorithm: 'brotliCompress', filename: '[path][base].br',
      compressionOptions: {params: {[zlib.constants.BROTLI_PARAM_QUALITY]: 11}}
    }),
    new CompressionPlugin({
      test: /\.(js|css|svg)(\?.*)?$/, threshold: 1024, algorithm: 'gzip', filename: '[path][base].gz',
      compressionOptions: {level: 9}
    })
  ],
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
