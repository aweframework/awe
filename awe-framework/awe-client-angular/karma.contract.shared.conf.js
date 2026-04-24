const path = require("path");

const applyBaseConfig = require("./karma.contract.conf.js");
const sharedTests = path.join(__dirname, "src", "test", "js", "contracts", "shared", "tests.js");

module.exports = config => {
  applyBaseConfig(config);
  config.set({
    files: [sharedTests],
    preprocessors: {
      [sharedTests]: ["webpack", "sourcemap"]
    }
  });
};
