// PostCSS for the Tailwind side of the AngularJS client (src/main/resources/tailwind). The LESS build does not use it.
// webpack.config.js (postcss-loader) and the Jest style tests read this same file.
module.exports = {
  plugins: [
    // Not minified here: the whole stylesheet is emitted unminified today, so the size comparison stays honest
    require("@tailwindcss/postcss")({optimize: false})
  ]
};
