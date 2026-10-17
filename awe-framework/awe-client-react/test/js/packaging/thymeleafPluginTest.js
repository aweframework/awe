const ThymeleafPlugin = require('../../../src/plugins/thymeleaf-plugin');

/**
 * Run the plugin over a page as HtmlWebpackPlugin emits it
 * @param {string} html Page
 * @returns {Promise<string>} Page the plugin emits
 */
function emit(html) {
  let beforeEmit = null;
  const htmlWebpackPlugin = {getHooks: () => ({beforeEmit: {tapAsync: (_name, callback) => { beforeEmit = callback; }}})};
  const compiler = {hooks: {compilation: {tap: (_name, callback) => callback({})}}};
  new ThymeleafPlugin({htmlWebpackPlugin}).apply(compiler);
  return new Promise((resolve, reject) => beforeEmit({html}, (error, data) => (error ? reject(error) : resolve(data.html))));
}

// Spring only adds the content hash (resource chain content strategy) to context-relative links, so a bundle linked
// relatively (HtmlWebpackPlugin publicPath "./") keeps its plain name and browsers keep a stale copy after a release
describe('awe-react-client/plugins/thymeleaf-plugin', () => {
  it('links the bundles injected with a relative public path from the context root', async () => {
    const html = await emit('<link href="./css/bundle.css" rel="stylesheet"><script defer src="./js/bundle.js"></script>');

    expect(html).toBe('<link th:href="@{/css/bundle.css}" rel="stylesheet"><script defer th:src="@{/js/bundle.js}"></script>');
  });

  it('links the bundles injected without a public path from the context root', async () => {
    const html = await emit('<link href="css/commons.css" rel="stylesheet"><script defer src="js/commons.js"></script>');

    expect(html).toBe('<link th:href="@{/css/commons.css}" rel="stylesheet"><script defer th:src="@{/js/commons.js}"></script>');
  });

  it('keeps the links that are already context-relative', async () => {
    const html = await emit('<link href="/css/bundle.css" rel="stylesheet"><script defer src="/js/bundle.js"></script>');

    expect(html).toBe('<link th:href="@{/css/bundle.css}" rel="stylesheet"><script defer th:src="@{/js/bundle.js}"></script>');
  });

  it('keeps the external and parent-relative links as they are', async () => {
    const html = await emit('<link href="https://cdn.example.com/a.css"><link href="//cdn.example.com/b.css">' +
      '<script src="../static/js/c.js"></script>');

    expect(html).toBe('<link th:href="@{https://cdn.example.com/a.css}"><link th:href="@{//cdn.example.com/b.css}">' +
      '<script th:src="@{../static/js/c.js}"></script>');
  });

  it('leaves the links that are not stylesheets or scripts as they are', async () => {
    const html = await emit('<link rel="icon" href="images/favicon.ico"><img src="images/logo.png">');

    expect(html).toBe('<link rel="icon" href="images/favicon.ico"><img src="images/logo.png">');
  });

  it('leaves the links of the template that are already Thymeleaf links', async () => {
    const html = await emit('<link rel="stylesheet" th:href="@{/css/splash.css}"/>');

    expect(html).toBe('<link rel="stylesheet" th:href="@{/css/splash.css}"/>');
  });
});
