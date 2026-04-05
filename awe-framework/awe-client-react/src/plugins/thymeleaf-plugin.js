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
          data.html = data.html
            .replace(/(\s+)href="(.+?\.css)"/g, '$1th:href="@{$2}"')
            .replace(/(\s+)src="(.+?\.js)"/g, '$1th:src="@{$2}"');

          cb(null, data);
        }
      );
    });
  }
}

module.exports = ThymeleafPlugin;
