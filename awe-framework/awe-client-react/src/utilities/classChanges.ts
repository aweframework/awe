/**
 * CSS class changes requested on DOM nodes by selector (add-class / remove-class / toggle-class actions).
 *
 * Nodes can be unmounted when the change is requested (e.g. the content of an inactive tab), be mounted again later or
 * render their classes again (React writes the whole class attribute of a node when its className changes), losing the
 * class changes done on them. The requested changes are remembered and applied again to the nodes matching the
 * selector when they are mounted (only the added nodes are touched, never the ones already mounted) and when a render
 * writes their class attribute, until they are cleared.
 *
 * As ng-class does in the AngularJS client, a render that changes one of those classes on purpose (the class was not in
 * the previous render and now it is, or the other way round) wins: the remembered change of that class is dropped.
 */

/**
 * Method of a class change: "add" and "remove" are remembered; "toggle" is remembered as the state it leaves
 */
export type ClassChangeMethod = "add" | "remove" | "toggle";

type RememberedMethod = Exclude<ClassChangeMethod, "toggle">;

const changes = new Map<string, Map<string, RememberedMethod>>();
const invalidSelectors = new Set<string>();
// Class attribute that the last render wrote on each changed node, without the remembered changes
let renderedClasses = new WeakMap<Element, string>();
let observer: MutationObserver | null = null;

function classesOf(node: Element): string {
  return node.getAttribute("class") || "";
}

function hasClass(classes: string, cssClass: string): boolean {
  return classes.split(/\s+/).includes(cssClass);
}

function matches(node: Element, selector: string): boolean {
  try {
    return node.matches(selector);
  } catch (error) {
    if (!invalidSelectors.has(selector)) {
      invalidSelectors.add(selector);
      console.warn(`Warning, invalid selector '${selector}' in a class change`, error);
    }
    return false;
  }
}

function setClass(node: Element, cssClass: string, method: RememberedMethod): void {
  // Only a real change touches the attribute
  if (node.classList.contains(cssClass) !== (method === "add")) {
    node.classList[method](cssClass);
  }
}

function applyToNode(node: Element, selector: string, cssClasses: Map<string, RememberedMethod>): void {
  const targets = [
    ...(matches(node, selector) ? [node] : []),
    ...(invalidSelectors.has(selector) ? [] : node.querySelectorAll(selector))
  ];
  targets.forEach(target => {
    renderedClasses.set(target, classesOf(target));
    cssClasses.forEach((method, cssClass) => setClass(target, cssClass, method));
  });
}

function forget(selector: string, cssClass: string): void {
  const cssClasses = changes.get(selector);
  if (cssClasses?.delete(cssClass) && !cssClasses.size) {
    changes.delete(selector);
  }
}

function onRender(node: Element): void {
  const previous = renderedClasses.get(node);
  const rendered = classesOf(node);
  renderedClasses.set(node, rendered);
  changes.forEach((cssClasses, selector) => {
    if (invalidSelectors.has(selector) || !matches(node, selector)) {
      return;
    }
    cssClasses.forEach((method, cssClass) => {
      if (previous !== undefined && hasClass(previous, cssClass) !== hasClass(rendered, cssClass)) {
        forget(selector, cssClass);
      } else {
        setClass(node, cssClass, method);
      }
    });
  });
}

function onMutations(mutations: MutationRecord[]): void {
  const rendered = new Set<Element>();
  mutations.forEach(mutation => {
    if (mutation.type === "attributes") {
      rendered.add(mutation.target as Element);
    } else {
      mutation.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          changes.forEach((cssClasses, selector) => !invalidSelectors.has(selector) && applyToNode(node as Element, selector, cssClasses));
        }
      });
    }
  });
  rendered.forEach(onRender);
  // The records of the changes just applied are not renders
  observer?.takeRecords();
}

function observe(): void {
  if (!observer && typeof MutationObserver !== "undefined") {
    observer = new MutationObserver(onMutations);
    observer.observe(document.body, {childList: true, subtree: true, attributes: true, attributeFilter: ["class"]});
  }
}

/**
 * Remember a class change so it is applied when the target node is mounted or renders its classes again.
 * A later change of the same selector and class replaces the previous one. A toggle flips the remembered change, or
 * does nothing when there is none (its node is not mounted, so it is not known whether it has the class)
 * @param {string} selector Target selector
 * @param {string} cssClass Class to change
 * @param {string} method "add", "remove" or "toggle"
 */
export function rememberClassChange(selector: string, cssClass: string, method: ClassChangeMethod): void {
  const cssClasses = changes.get(selector) || new Map<string, RememberedMethod>();
  const remembered = cssClasses.get(cssClass);
  let next: RememberedMethod | undefined = method === "toggle" ? undefined : method;
  if (method === "toggle" && remembered) {
    next = remembered === "add" ? "remove" : "add";
  }
  if (next) {
    changes.set(selector, cssClasses.set(cssClass, next));
    observe();
  }
}

/**
 * Change the classes of the node of a selector now, when it is mounted, and remember the changes for the next renders
 * and mounts of the nodes of the selector. A toggle is remembered as the state it leaves on the node
 * @param {Element|null} node Node of the selector, null when it is not mounted
 * @param {string} selector Target selector
 * @param {string[]} cssClasses Classes to change
 * @param {string} method "add", "remove" or "toggle"
 */
export function changeClasses(node: Element | null, selector: string, cssClasses: string[], method: ClassChangeMethod): void {
  // The renders written before this change are processed first, so they are not taken for one after it
  if (observer) {
    onMutations(observer.takeRecords());
  }
  if (node && !renderedClasses.has(node)) {
    renderedClasses.set(node, classesOf(node));
  }
  cssClasses.filter(cssClass => cssClass).forEach(cssClass => {
    if (node) {
      const added = method === "toggle" ? !node.classList.contains(cssClass) : method === "add";
      rememberClassChange(selector, cssClass, added ? "add" : "remove");
      setClass(node, cssClass, added ? "add" : "remove");
    } else {
      rememberClassChange(selector, cssClass, method);
    }
  });
  // The records of these changes are not renders
  observer?.takeRecords();
}

/**
 * Forget all the remembered class changes (when the screen changes)
 */
export function clearClassChanges(): void {
  changes.clear();
  invalidSelectors.clear();
  renderedClasses = new WeakMap();
  if (observer) {
    observer.disconnect();
    observer = null;
  }
}
