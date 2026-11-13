// Webpack loader of the AWE theme of PrimeReact (#885): turns the Lara theme into the AWE theme (see primereact-theme.cjs)
const {buildThemeFrom, TABLE_FILE, TOKENS_FILE, AURA_LICENSE_FILE} = require("./primereact-theme.cjs");

module.exports = function primeReactThemeLoader(laraCss) {
  const {primereactDir} = this.getOptions();
  [TABLE_FILE, TOKENS_FILE, AURA_LICENSE_FILE].forEach(file => this.addDependency(file));
  return buildThemeFrom(laraCss, primereactDir);
};
