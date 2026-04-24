const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const textTests = path.join(__dirname, "src", "test", "js", "contracts", "textFamily", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [textTests],
    preprocessors: {
      [textTests]: ["webpack", "sourcemap"]
    }
  });
};
