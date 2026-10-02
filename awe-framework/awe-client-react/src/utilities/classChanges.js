/**
 * CSS class changes requested on DOM nodes by selector (add-class / remove-class actions).
 *
 * Nodes can be unmounted when the change is requested (e.g. the content of an inactive tab) or
 * be mounted again later, losing the class changes done on them. The requested changes are remembered
 * and applied to the nodes matching the selector when they are mounted (only the added nodes are
 * touched, never the ones already mounted), until they are cleared.
 */
const changes = new Map();
const invalidSelectors = new Set();
let observer = null;

function applyToNode(node, selector, cssClasses) {
  try {
    const targets = [
      ...(node.matches(selector) ? [node] : []),
      ...node.querySelectorAll(selector)
    ];
    targets.forEach(target => cssClasses.forEach((method, cssClass) => target.classList[method](cssClass)));
  } catch (error) {
    if (!invalidSelectors.has(selector)) {
      invalidSelectors.add(selector);
      console.warn(`Warning, invalid selector '${selector}' in a class change`, error);
    }
  }
}

function onMutations(mutations) {
  mutations.forEach(mutation => mutation.addedNodes.forEach(node => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      changes.forEach((cssClasses, selector) => !invalidSelectors.has(selector) && applyToNode(node, selector, cssClasses));
    }
  }));
}

function observe() {
  if (!observer && typeof MutationObserver !== "undefined") {
    observer = new MutationObserver(onMutations);
    observer.observe(document.body, {childList: true, subtree: true});
  }
}

/**
 * Remember a class change so it is applied when the target node is mounted.
 * A later change of the same selector and class replaces the previous one (a toggle drops it)
 * @param {string} selector Target selector
 * @param {string} cssClass Class to change
 * @param {string} method "add", "remove" or "toggle"
 */
export function rememberClassChange(selector, cssClass, method) {
  const cssClasses = changes.get(selector) || new Map();
  if (method === "add" || method === "remove") {
    changes.set(selector, cssClasses.set(cssClass, method));
    observe();
  } else if (cssClasses.delete(cssClass) && !cssClasses.size) {
    changes.delete(selector);
  }
}

/**
 * Forget all the remembered class changes (when the screen changes)
 */
export function clearClassChanges() {
  changes.clear();
  invalidSelectors.clear();
  if (observer) {
    observer.disconnect();
    observer = null;
  }
}
