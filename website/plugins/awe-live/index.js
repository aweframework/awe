/**
 * AWE live plugin: lets the docs render the REAL components of the AWE React client (awe-client-react/src).
 *
 * Docusaurus only compiles the site folder and knows neither LESS nor the AWE theme of PrimeReact, so the plugin adds
 * to the webpack configuration:
 *  - the `@awe-client` alias to the client sources and `@awe-tokens` to the design tokens;
 *  - a LESS rule (the client styles its components with LESS);
 *  - the AWE theme of PrimeReact (the client build turns the Lara theme into it with PrimeReactThemePlugin);
 *  - the Lucide icon sprite the client icons point to;
 *  - ONE copy of react, react-dom, react-redux, @reduxjs/toolkit, i18next and react-i18next, so hooks and the store
 *    context are shared between the site and the client.
 */
const fs = require('fs');
const path = require('path');

const CLIENT_DIR = path.resolve(__dirname, '../../../awe-framework/awe-client-react');
const DESIGN_TOKENS_DIR = path.resolve(__dirname, '../../../awe-framework/awe-design-tokens');
const CLIENT_MODULES = path.join(CLIENT_DIR, 'node_modules');

// Singletons: a second copy would break hooks (react) or the store context (react-redux)
const SITE_COPIES = ['react', 'react-dom'];
const CLIENT_COPIES = ['react-redux', '@reduxjs/toolkit', 'i18next', 'react-i18next'];

// The client dependencies are installed with `npm ci` in its folder; without them webpack fails with an obscure
// "Module not found", so the build says what to do instead
function assertClientInstalled() {
  const required = [
    [CLIENT_MODULES, 'the dependencies of awe-framework/awe-client-react'],
    [path.join(CLIENT_MODULES, 'primereact'), 'primereact in awe-framework/awe-client-react'],
    [path.join(DESIGN_TOKENS_DIR, 'primereact/primereact-theme.cjs'), 'awe-framework/awe-design-tokens'],
  ];
  const missing = required.filter(([location]) => !fs.existsSync(location)).map(([, what]) => what);
  if (missing.length > 0) {
    throw new Error(
      `awe-live: cannot find ${missing.join(', ')}. The component catalog renders the real AWE React client: ` +
      'run `npm ci --prefix awe-framework/awe-client-react` (and check out awe-framework/awe-design-tokens) first.',
    );
  }
}

module.exports = function aweLivePlugin(context) {
  return {
    name: 'awe-live',
    configureWebpack(config, isServer, utils) {
      assertClientInstalled();
      const {PrimeReactThemePlugin} = require(path.join(DESIGN_TOKENS_DIR, 'primereact/primereact-theme.cjs'));
      const {LucideSpritePlugin} = require(path.join(DESIGN_TOKENS_DIR, 'icons/lucide-sprite.cjs'));

      const alias = {'@awe-client': path.join(CLIENT_DIR, 'src'), '@awe-tokens': DESIGN_TOKENS_DIR};
      for (const name of SITE_COPIES) {
        alias[`${name}$`] = path.join(context.siteDir, 'node_modules', name);
      }
      for (const name of CLIENT_COPIES) {
        alias[`${name}$`] = path.join(CLIENT_MODULES, name);
      }

      const plugins = [new PrimeReactThemePlugin({primereactDir: path.join(CLIENT_MODULES, 'primereact')})];
      // The sprite is a build asset: it is emitted once, by the client compilation
      if (!isServer) {
        plugins.push(new LucideSpritePlugin({
          lucideDir: path.join(CLIENT_MODULES, 'lucide-static'),
          filename: 'icons/lucide-sprite.svg',
        }));
      }

      return {
        resolve: {
          alias,
          // Packages only the client declares (primereact, lodash, ...)
          modules: [CLIENT_MODULES],
          extensions: ['.tsx', '.ts', '.jsx'],
        },
        module: {
          rules: [
            {
              test: /\.less$/,
              use: [...utils.getStyleLoaders(isServer, {importLoaders: 1}), 'less-loader'],
            },
          ],
        },
        plugins,
      };
    },
  };
};
