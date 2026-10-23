// The frozen bases of the component registry outlive a Redux store: reset them after every test so no test sees the
// components another one registered. The registry is required lazily so this file does not load it (and what it imports)
// before a suite sets up its own mocks
afterEach(() => {
  require('../../src/redux/registry/ComponentRegistry').default.clearAll();
});
