const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const numericTests = path.join(__dirname, "src", "test", "js", "contracts", "numericFamily", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [numericTests],
    preprocessors: {
      [numericTests]: ["webpack", "sourcemap"]
    }
  });
};
