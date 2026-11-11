// Bases registered by each live example, by registry: the registry is one for the page and keyed by component id,
// so examples that share an id stack on it and the last one to mount is the one the registry shows
const stacks = new WeakMap();

/**
 * Registers the components of a live example as the base of the client registry, as a screen load does in the
 * application: the state of the client only keeps deltas (a group selection stores no address), and the base
 * is where the merge takes the address from. Without it a radio of a group turns into the loading skeleton.
 *
 * The registry is shared by every example of the page: releasing removes only what this call registered, and gives
 * back the base of the example it covered, if that one is still mounted.
 *
 * @param {{register: Function, unregister: Function}} registry ComponentRegistry of the client
 * @param {Object<string, Object>} components Full components by id (the preloaded state)
 * @returns {function(): void} Release of what this call registered (idempotent), with `raise()`, which puts this base
 * back on top of the examples that share its ids
 */
function registerLiveComponents(registry, components) {
  if (!stacks.has(registry)) stacks.set(registry, new Map());
  const byId = stacks.get(registry);
  const owned = Object.entries(components ?? {}).map(([id, base]) => {
    const entry = { base };
    if (!byId.has(id)) byId.set(id, []);
    byId.get(id).push(entry);
    registry.register(id, base);
    return [id, entry];
  });
  let released = false;
  const release = () => {
    if (released) return;
    released = true;
    for (const [id, entry] of owned) {
      const stack = byId.get(id);
      const index = stack.indexOf(entry);
      stack.splice(index, 1);
      // Only the example on top of the registry changes it: a covered one is already out of it
      if (index !== stack.length) continue;
      if (stack.length > 0) registry.register(id, stack[stack.length - 1].base);
      else {
        registry.unregister(id);
        byId.delete(id);
      }
    }
  };
  // Puts this base back on top of the examples that share its ids, as the one the registry shows
  release.raise = () => {
    if (released) return;
    for (const [id, entry] of owned) {
      const stack = byId.get(id);
      if (stack[stack.length - 1] === entry) continue;
      stack.splice(stack.indexOf(entry), 1);
      stack.push(entry);
      registry.register(id, entry.base);
    }
  };
  return release;
}

/**
 * Registration of one live example, kept in step with its state. A new state registers its components at once, while
 * the store is built, so the example renders against its own base: registered in an effect, the base would come after
 * the render, and the merge of the client would still give back the attributes of the previous state (an icon that is
 * no longer chosen). The new base goes on top before the old one is released, so the registry never goes empty.
 *
 * @param {{register: Function, unregister: Function}} registry ComponentRegistry of the client
 * @returns {{use: function(string, Object): void, release: function(): void}} `use(key, components)` registers the
 * components of the state `key` unless they are already registered; `release()` gives back what is registered;
 * `raise()` makes its base the one the registry shows again, when another example with the same ids covered it
 */
function liveRegistration(registry) {
  let current = null;
  return {
    use(key, components) {
      if (current?.key === key) return;
      const release = registerLiveComponents(registry, components);
      current?.release();
      current = { key, release };
    },
    release() {
      current?.release();
      current = null;
    },
    raise() {
      current?.release.raise();
    },
  };
}

module.exports = { registerLiveComponents, liveRegistration };
