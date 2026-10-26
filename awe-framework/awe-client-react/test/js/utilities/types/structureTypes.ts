// Type-level assertions for utilities/structure, checked by `npm run typecheck` (not run by Jest)
import type {ReactElement, ReactNode} from "react";
import {Columns, Components, Criteria, getSource, getSourceChildren, getSourceStyle} from "../../../../src/utilities/structure";

const elements = [{source: "center", elementList: [{elementType: "Button", id: "btn"}]}, {source: "left"}];
const columnNode = {component: "text", address: {view: "report", component: "Grd", column: "col", row: "1"}};

// The source is found by name, or it is an empty source
const source: {source?: string; elementList?: unknown[]} = getSource("center", elements);
const sourceList: unknown[] | undefined = getSource("center", elements).elementList;
// @ts-expect-error the elements are a list
getSource("center", {source: "center"});
// @ts-expect-error the source is text
getSource(1, elements);

// The children of a source are nodes, one element when it has a type, or null
const children: ReactNode = getSourceChildren(elements[0]);
const defaultChildren: ReactNode = getSourceChildren();
const noChildren: ReactNode = getSourceChildren(null);
const childList: ReactNode[] | ReactElement | null = getSourceChildren({type: "section", elementList: []});
// @ts-expect-error the type of a source is text
getSourceChildren({type: 1});

// The style of a source is text
const style: string = getSourceStyle({style: "p-3"});
const noStyle: string = getSourceStyle(undefined);
// @ts-expect-error the style is text, not a number
const wrongStyle: number = getSourceStyle({style: "p-3"});

// A component is an element, null when it is ignored, or nothing when it is unknown
const component: ReactElement | null | undefined = Components({elementType: "Button", id: "btn"}, 0);
// @ts-expect-error the component may be null or undefined
const definiteComponent: ReactElement = Components({elementType: "Button"}, 0);
// @ts-expect-error the index is a number
Components({elementType: "Button"}, "0");

// A criterion is always an element
const criterion: ReactElement = Criteria({component: "text", id: "cri"}, 0);
// @ts-expect-error the criterion needs its index
Criteria({component: "text"});

// A column is an element
const cell: ReactElement = Columns(columnNode, {label: "One", value: 1});
const cellEditing: ReactElement = Columns(columnNode, [{value: 1, selected: true}], {}, true);
const defaultCell: ReactElement = Columns(columnNode);
// @ts-expect-error a column has an address
Columns({component: "text"});
// @ts-expect-error the editing flag is a boolean
Columns(columnNode, {}, {}, "yes");

export {source, sourceList, children, defaultChildren, noChildren, childList, style, noStyle, wrongStyle, component};
export {definiteComponent, criterion, cell, cellEditing, defaultCell};
