// Type-level assertions for utilities/general, checked by `npm run typecheck` (not run by Jest)
import {
  ComponentType,
  DATE_RANGE_FROM_SUFFIX,
  compareEqualValues,
  getFirstDefinedAndNotNullValue,
  getFirstDefinedValue,
  isEmpty,
  isEmptyCell,
  isMultipleComponent
} from "../../../../src/utilities/general";

// The component types are literal values, so a type name can be checked against them
const grid: "grid" = ComponentType.COMPONENT_GRID;
const picklist: "picklist" = ComponentType.COMPONENT_PICKLIST;
const suffix: ".from" = DATE_RANGE_FROM_SUFFIX;
// @ts-expect-error a component type is a literal, not any text
const wrongType: "text" = ComponentType.COMPONENT_GRID;
// @ts-expect-error there is no such component type
const unknownType = ComponentType.COMPONENT_UNKNOWN;

// The predicates answer with a boolean for any value
const multiple: boolean = isMultipleComponent(ComponentType.COMPONENT_SELECT_MULTIPLE);
const maybeMultiple: boolean = isMultipleComponent(undefined);
const equal: boolean = compareEqualValues(1, "1");
const empty: boolean = isEmpty({});
const emptyCell: boolean = isEmptyCell(null);
// @ts-expect-error a predicate does not answer with text
const emptyText: string = isEmpty("");

// The first defined value keeps the type of the values it is given (and undefined when there is none)
const firstText: string | undefined = getFirstDefinedValue("a", undefined);
const firstNumber: number | undefined = getFirstDefinedAndNotNullValue(undefined, 2, 3);
const nothing: undefined = getFirstDefinedValue();
// @ts-expect-error the first defined value may be undefined
const definite: number = getFirstDefinedAndNotNullValue(undefined, 2);
// @ts-expect-error the first defined value is not text when the values are numbers
const notText: string | undefined = getFirstDefinedValue(1, 2);

export {unknownType, grid, picklist, suffix, wrongType, multiple, maybeMultiple, equal, empty, emptyCell, emptyText};
export {firstText, firstNumber, nothing, definite, notText};
