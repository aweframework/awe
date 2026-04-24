const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const booleanTests = path.join(__dirname, "src", "test", "js", "contracts", "booleanFamily", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [booleanTests],
    preprocessors: {
      [booleanTests]: ["webpack", "sourcemap"]
    }
  });
};
