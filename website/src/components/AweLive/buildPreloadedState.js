const isObject = value => value !== null && typeof value === 'object' && !Array.isArray(value);

/**
 * Redux preloaded state of an AWE React client store, built from a plain description of its components.
 * Pure on purpose (no JSX, no CSS) so that node:test can run it.
 *
 * The settings slice is returned as given: the provider completes it with the client DEFAULT_SETTINGS.
 *
 * @param {Object<string, {attributes?: Object, actions?: Array, model?: Object, validationRules?: Object, specificAttributes?: Object}>} [components]
 *   Description by component id (none: empty). The model ({values: [...]}), the validation rules and the specific
 *   attributes (first row, sort of a grid) are registered only when they are described: the client reads the options
 *   of a select, the rows of a grid or the required rule of a field from them
 * @param {{view?: string, settings?: Object, messages?: Object<string, {title: string, message: string}>}} [options]
 *   View name of the addresses, settings overrides and the predefined messages of the view (`message` elements: the
 *   `confirm` actions name them in their target)
 * @returns {{components: Object, settings: Object, actions: {running: boolean}, messages?: {defined: Object}}} Preloaded state;
 *   the messages slice is partial: see completeMessages
 */
function buildPreloadedState(components, { view = 'live', settings, messages } = {}) {
  if (messages !== undefined && !isObject(messages)) {
    throw new TypeError('AweLive: the messages must be an object');
  }
  const registered = {};
  for (const [id, description] of Object.entries(components ?? {})) {
    const { attributes = {}, actions = [], model, validationRules, specificAttributes } = description ?? {};
    if (!isObject(description ?? {})) {
      throw new TypeError(`AweLive: the description of the component "${id}" must be an object`);
    }
    if (!isObject(attributes)) {
      throw new TypeError(`AweLive: the attributes of the component "${id}" must be an object`);
    }
    if (!Array.isArray(actions)) {
      throw new TypeError(`AweLive: the actions of the component "${id}" must be an array`);
    }
    for (const [name, part] of [['model', model], ['validationRules', validationRules], ['specificAttributes', specificAttributes]]) {
      if (part !== undefined && !isObject(part)) {
        throw new TypeError(`AweLive: the ${name} of the component "${id}" must be an object`);
      }
    }
    registered[id] = {
      address: { view, component: id },
      attributes: { visible: true, ...structuredClone(attributes) },
      actions: structuredClone(actions),
      ...(model !== undefined && { model: structuredClone(model) }),
      ...(validationRules !== undefined && { validationRules: structuredClone(validationRules) }),
      ...(specificAttributes !== undefined && { specificAttributes: structuredClone(specificAttributes) }),
    };
  }
  return {
    components: registered,
    settings: { ...settings },
    actions: { running: false },
    // The slice keeps what the reducer starts with (the messages of the base and report views)
    ...(messages && { messages: { defined: { [view]: structuredClone(messages) } } }),
  };
}

/**
 * Completes the messages slice of the preloaded state with what the reducer of the client starts with (its base and
 * report views, the list of the messages showing...), so that the example does not keep a copy of that shape.
 * @param {{defined?: Object}} [slice] Slice built by buildPreloadedState (none: the reducer starts it)
 * @param {{defined?: Object}} initial Initial state of the messages reducer
 * @returns {Object|undefined} Slice to preload
 */
function completeMessages(slice, initial) {
  return slice && { ...initial, ...slice, defined: { ...initial.defined, ...slice.defined } };
}

module.exports = { buildPreloadedState, completeMessages };
