const path = require("path");

// Bundle of the AWE chart server: one Node file with ECharts and the chart code of the client, so the container image
// needs no npm install. It is not part of the published npm package (dist/).
module.exports = {
  mode: "production",
  target: "node",
  entry: path.join(__dirname, "src", "renderer", "main.js"),
  output: {
    filename: "awe-chart-server.cjs",
    path: path.resolve(__dirname, "target", "chart-server"),
    clean: true
  },
  optimization: {
    // Keep readable stack traces in the server logs
    minimize: false
  },
  module: {
    rules: [
      {test: /\.(js|ts)$/, exclude: /node_modules/, use: {loader: "babel-loader", options: {presets: [["@babel/preset-env", {targets: {node: "20"}}], "@babel/preset-typescript"]}}}
    ]
  },
  resolve: {
    extensions: [".ts", ".js"]
  }
};
