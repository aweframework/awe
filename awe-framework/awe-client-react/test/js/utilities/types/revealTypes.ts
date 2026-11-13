// Type-level assertions for utilities/reveal, checked by `npm run typecheck` (not run by Jest)
import {
  findFirstInvalid,
  focusComponent,
  getInvalidPanelValues,
  getRevealSelections,
  getStructureOrder
} from "../../../../src/utilities/reveal";

const field = {
  address: {view: "base", component: "field"},
  context: {view: "base", source: ["tabs", "two", "field"]},
  attributes: {error: {message: "x"}},
  model: {}
};
const tabs = {
  address: {view: "base", component: "tabs"},
  context: {source: ["tabs"]},
  attributes: {autocollapse: false},
  model: {values: [{value: "one", selected: true}, {value: "two", selected: false}]}
};
const components = {field, tabs};

// The structure order is a list of ids
const order: string[] = getStructureOrder({id: "root", elementList: [{id: "a"}, {id: "dep", elementType: "DependencyElement"}]});
const emptyOrder: string[] = getStructureOrder(undefined);

// The first invalid component is one of the received components, so it keeps their type
const invalid: typeof field | typeof tabs | null = findFirstInvalid(components, order);
const address: string | undefined = invalid?.address.component;
// @ts-expect-error the result may be null
const notNull: typeof field | typeof tabs = findFirstInvalid(components);
// @ts-expect-error the order is a list of ids
findFirstInvalid(components, [1, 2]);

// The selections tell which options to select in which container
const selections = getRevealSelections(components, field);
const optionsToSelect: {value?: unknown; selected?: boolean}[] = selections[0].values;
const selectionAddress: string | undefined = selections[0].address.component;
// @ts-expect-error the target to reveal is a component, not its id
getRevealSelections(components, "field");

// The invalid panel values are option values
const values: string[] = getInvalidPanelValues(components, "tabs");
// @ts-expect-error the panel is identified by its id
getInvalidPanelValues(components, 1);

// Focusing answers whether the criterion was found at the first try
const found: boolean = focusComponent("field", {attempts: 3, delay: 10});
focusComponent("field");
// @ts-expect-error the attempts are a number
focusComponent("field", {attempts: "3"});
// @ts-expect-error the criterion is identified by its id
focusComponent(1);

export {order, emptyOrder, invalid, address, notNull, optionsToSelect, selectionAddress, values, found};
