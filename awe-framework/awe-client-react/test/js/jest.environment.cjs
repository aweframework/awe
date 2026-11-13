const JSDOMEnvironment = require('jest-environment-jsdom').TestEnvironment;

class FilteredJSDOMEnvironment extends JSDOMEnvironment {
  constructor(config, context) {
    super(config, context);

    const virtualConsole = this.dom?.window?._virtualConsole;
    if (!virtualConsole || typeof virtualConsole.emit !== 'function') {
      return;
    }

    const originalEmit = virtualConsole.emit.bind(virtualConsole);
    virtualConsole.emit = (type, ...args) => {
      if (type === 'jsdomError') {
        const [error] = args;
        const message = String(error?.message || '');
        const stack = String(error?.stack || '');
        const detail = String(error?.detail || '');
        const fullText = `${message} ${stack} ${detail}`;

        if (fullText.includes('Could not parse CSS stylesheet')) {
          return false;
        }
      }

      return originalEmit(type, ...args);
    };
  }
}

module.exports = FilteredJSDOMEnvironment;
