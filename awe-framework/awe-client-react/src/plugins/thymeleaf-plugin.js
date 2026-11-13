// External (scheme or protocol-relative), context-relative or parent-relative links are kept as they are
const KEPT_LINK = /^([a-z][a-z\d+.-]*:|\/|\.\.\/)/i;

/**
 * Link a resource from the context root: Spring only adds the content hash (resource chain content strategy) to
 * context-relative links, so a bundle linked relatively keeps its plain name and browsers keep a stale copy after a release
 * @param {string} link Resource link
 * @returns {string} Context-relative link
 */
const fromContextRoot = link => (KEPT_LINK.test(link) ? link : `/${link.replace(/^\.\//, '')}`);

/**
 * Replace the attribute of the resources with a given extension with its Thymeleaf link from the context root
 * @param {string} html Page
 * @param {string} attribute Attribute (href or src)
 * @param {string} extension Extension of the resources
 * @returns {string} Page
 */
const linkResources = (html, attribute, extension) => html.replace(new RegExp(`(\\s)${attribute}="([^"]*)"`, 'g'),
  (match, space, link) => (link.endsWith(extension) ? `${space}th:${attribute}="@{${fromContextRoot(link)}}"` : match));

class ThymeleafPlugin {
  constructor(options = {}) {
    this.htmlWebpackPlugin = options.htmlWebpackPlugin || null;
  }

  apply(compiler) {
    compiler.hooks.compilation.tap('ThymeleafPlugin', (compilation) => {
      const HtmlWebpackPlugin = this.htmlWebpackPlugin || require('html-webpack-plugin');

      HtmlWebpackPlugin.getHooks(compilation).beforeEmit.tapAsync(
        'ThymeleafPlugin',
        (data, cb) => {
          // Reemplazar las rutas de CSS y JS con sintaxis de Thymeleaf
          data.html = linkResources(linkResources(data.html, 'href', '.css'), 'src', '.js');

          cb(null, data);
        }
      );
    });
  }
}

module.exports = ThymeleafPlugin;
