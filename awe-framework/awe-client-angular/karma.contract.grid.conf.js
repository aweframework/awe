const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const gridTests = path.join(__dirname, "src", "test", "js", "contracts", "gridFamily", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [gridTests],
    preprocessors: {
      [gridTests]: ["webpack", "sourcemap"]
    }
  });
};
