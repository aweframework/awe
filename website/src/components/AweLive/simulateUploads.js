const UPLOAD_URL = /\/file\/upload$/;
const STEPS = [0.25, 0.5, 0.75, 1];

// The address of the criterion travels as JSON in the form; a body without a readable one hands over no address
function parseAddress(text) {
  try {
    return JSON.parse(text ?? 'null');
  } catch {
    return null;
  }
}

/**
 * Whether an upload belongs to the example that owns the store: the address of its criterion names the view of that
 * example (each example that simulates uploads has its own) and one of its components. Two uploaders on a page, with
 * the same component id, never receive each other's files.
 * @param {{view?: string, component?: string}} [address] Address the upload carries
 * @param {string} view View of the store of the example
 * @param {Object} components Components of its store
 * @returns {boolean} true when the example takes the file
 */
function acceptsUpload(address, view, components) {
  return Boolean(address) && address.view === view && Boolean(components?.[address.component]);
}

/**
 * The simulator of the uploads of one window: the uploader of the client sends its file with an XMLHttpRequest to the
 * server, and the docs must never send a file anywhere. While an example is mounted, the requests to the upload url
 * are not sent: they report their progress, hand the file to the example (which shows it as the server would) and end
 * with status 200. The requests to any other url go on untouched. It patches `open` and `send` of the prototype of the
 * window, installed with the first example and removed with the last one.
 * A function of the window it patches so that node:test can run it with a fake one.
 *
 * @param {{XMLHttpRequest: function}} [win] Window to patch; without one nothing is simulated
 * @param {{delay?: number, schedule?: function, createEvent?: function}} [options] Pause between progress steps in ms,
 * timer and factory of the progress event (the browser ones by default)
 * @returns {{simulate: function(function({address: Object, file: Object}): void): function(): void}} simulate(handler)
 * returns the function that stops it. The handler gets the address of the criterion and the file chosen
 */
function createUploadSimulator(win, options = {}) {
  const {
    delay = 150,
    schedule = (fn, ms) => setTimeout(fn, ms),
    createEvent = (type, init) => new ProgressEvent(type, init),
  } = options;
  const handlers = new Set();
  let proto = null;
  let originalOpen = null;
  let originalSend = null;

  function simulateSend(xhr, body) {
    const file = body?.get?.('file');
    const address = parseAddress(body?.get?.('address'));
    const total = Math.max(file?.size ?? 0, 1);
    STEPS.forEach((step, index) => schedule(() => {
      xhr.upload.dispatchEvent(createEvent('progress', { lengthComputable: true, loaded: Math.round(total * step), total }));
      if (index === STEPS.length - 1) {
        schedule(() => {
          handlers.forEach(handler => handler({ address, file }));
          Object.defineProperties(xhr, { readyState: { value: 4, configurable: true }, status: { value: 200, configurable: true } });
          xhr.onreadystatechange?.();
          // The events of the end of a request, for the code that listens to them instead of to the state
          xhr.upload.dispatchEvent(createEvent('load', { lengthComputable: true, loaded: total, total }));
          xhr.dispatchEvent?.(createEvent('load', {}));
          xhr.dispatchEvent?.(createEvent('loadend', {}));
        }, delay);
      }
    }, delay * (index + 1)));
  }

  function install() {
    proto = win.XMLHttpRequest.prototype;
    originalOpen = proto.open;
    originalSend = proto.send;
    proto.open = function open(method, url, ...rest) {
      this.aweSimulatedUpload = UPLOAD_URL.test(String(url).split(/[?#]/)[0]);
      return originalOpen.call(this, method, url, ...rest);
    };
    proto.send = function send(body) {
      return this.aweSimulatedUpload ? simulateSend(this, body) : originalSend.call(this, body);
    };
  }

  function simulate(handler) {
    if (typeof handler !== 'function' || !win?.XMLHttpRequest) {
      return () => {};
    }
    if (handlers.size === 0) {
      install();
    }
    const entry = (...args) => handler(...args);
    handlers.add(entry);
    return () => {
      handlers.delete(entry);
      if (handlers.size === 0 && proto) {
        proto.open = originalOpen;
        proto.send = originalSend;
        proto = null;
      }
    };
  }

  return { simulate };
}

const simulator = createUploadSimulator(typeof window === 'undefined' ? undefined : window);

/**
 * Simulates the uploads of the uploader criteria while the example is mounted (see createUploadSimulator).
 * @param {function({address: Object, file: Object}): void} handler Receives the file chosen
 * @returns {function(): void} Stops simulating
 */
const simulateUploads = handler => simulator.simulate(handler);

module.exports = { createUploadSimulator, simulateUploads, acceptsUpload };
