const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const dateTimeTests = path.join(__dirname, "src", "test", "js", "contracts", "dateTimeFamily", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [dateTimeTests],
    preprocessors: {
      [dateTimeTests]: ["webpack", "sourcemap"]
    }
  });
};
