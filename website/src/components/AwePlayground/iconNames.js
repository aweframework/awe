// The sprite of the site has the current Lucide ids only; the old names (aliases) of the icons an example may use
// resolve through the same table the client uses (awe-design-tokens/icons), which is not copied here.
const lucideNames = require('../../../../awe-framework/awe-design-tokens/icons/lucide-names.json');

const ICONS = new Set(lucideNames.icons);

/**
 * Id of the sprite for an icon option of a playground (`lucide:<name>`).
 * @param {string} option Icon option
 * @returns {string|null} Current Lucide id, or null when the option is not a known Lucide icon
 */
const spriteId = option => {
  const name = option.replace(/^lucide:/, '');
  if (ICONS.has(name)) return name;
  return Object.prototype.hasOwnProperty.call(lucideNames.aliases, name) ? lucideNames.aliases[name] : null;
};

module.exports = {spriteId};
