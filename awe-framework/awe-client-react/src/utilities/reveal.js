/**
 * Helpers to bring an invalid field into view when a validation fails.
 *
 * A component knows the chain of containers that hold it ("context.source": the ids of its ancestors followed by its
 * own id). A panelable container (tabs, wizard, accordion) keeps in its model one option per child, and the value of
 * that option is the id of the child. So the way to a hidden field is the list of options to select in its ancestors.
 */
import {TestIds} from "./testIds";

const FOCUSABLE = `[data-testid='${TestIds.criterionInput}'], input:not([type='hidden']), textarea, select, button`;
const DEFAULT_ATTEMPTS = 10;
const DEFAULT_DELAY = 50;

/**
 * Get the ids of a screen structure in document order (dependency elements only reference components)
 * @param {object} structure Screen structure
 * @param {string[]} order Ids already found
 * @return {string[]} Ids in document order
 */
export function getStructureOrder(structure, order = []) {
  if (!structure) {
    return order;
  }
  if (structure.id && structure.elementType !== "DependencyElement" && !order.includes(structure.id)) {
    order.push(structure.id);
  }
  (structure.elementList || []).forEach(child => getStructureOrder(child, order));
  return order;
}

/**
 * Find the first invalid component
 * @param {object} components Components by id
 * @param {string[]} order Ids in document order (the components out of it go last)
 * @return {object|null} First invalid component
 */
export function findFirstInvalid(components, order = []) {
  const rank = new Map(order.map((id, index) => [id, index]));
  const rankOf = (component) => rank.get(component.address.component) ?? Number.MAX_SAFE_INTEGER;
  return Object.values(components || {})
    .filter(component => component?.attributes?.error && component.address?.component)
    .reduce((first, component) => (first === null || rankOf(component) < rankOf(first)) ? component : first, null);
}

/**
 * Get the options to select in the containers that hide a component, from the outer container to the inner one
 * @param {object} components Components by id
 * @param {object} target Component to reveal
 * @return {{address: object, values: object[]}[]} Model updates
 */
export function getRevealSelections(components, target) {
  const source = target?.context?.source || [];
  return source.slice(0, -1).reduce((selections, parentId, index) => {
    const parent = components[parentId];
    const values = parent?.model?.values;
    const optionIndex = Array.isArray(values) ? values.findIndex(item => item?.value === source[index + 1]) : -1;
    if (optionIndex < 0 || values[optionIndex].selected) {
      return selections;
    }
    const multiple = parent.attributes?.autocollapse === false;
    return [...selections, {
      address: parent.address,
      values: values.map((item, itemIndex) => ({
        ...item,
        selected: itemIndex === optionIndex || (multiple && !!item.selected)
      }))
    }];
  }, []);
}

/**
 * Get the options of a container that hold invalid fields
 * @param {object} components Components by id
 * @param {string} panelId Container id
 * @return {string[]} Values of the options with invalid fields
 */
export function getInvalidPanelValues(components, panelId) {
  const invalid = new Set();
  Object.values(components || {}).forEach(component => {
    const source = component?.context?.source || [];
    const position = source.indexOf(panelId);
    if (component?.attributes?.error && position >= 0 && position + 1 < source.length) {
      invalid.add(source[position + 1]);
    }
  });
  return [...invalid];
}

/**
 * Focus a criterion and scroll it into view. A criterion inside a panel that has just been opened is not rendered
 * yet, so the lookup is retried a few times.
 * @param {string} id Criterion id
 * @param {object} options Options
 * @param {number} options.attempts Number of retries
 * @param {number} options.delay Milliseconds between retries
 * @return {boolean} The criterion was found at the first try
 */
export function focusComponent(id, {attempts = DEFAULT_ATTEMPTS, delay = DEFAULT_DELAY} = {}) {
  const criterion = document.querySelector(`[criterion-id="${id}"]`);
  if (!criterion) {
    if (attempts > 1) {
      setTimeout(() => focusComponent(id, {attempts: attempts - 1, delay}), delay);
    }
    return false;
  }
  criterion.querySelector(FOCUSABLE)?.focus({preventScroll: true});
  criterion.scrollIntoView?.({block: "center", behavior: "smooth"});
  return true;
}
