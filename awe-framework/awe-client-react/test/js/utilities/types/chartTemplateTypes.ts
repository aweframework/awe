// Type-level assertions for utilities/chartTemplate, checked by `npm run typecheck` (not run by Jest)
import {Expression, TemplateNode, evaluate, parseTemplate, resolvePath} from "../../../../src/utilities/chartTemplate";

// A path is resolved in any context, the value is unknown
const value: unknown = resolvePath({point: {y: 1}}, "point.y");
// @ts-expect-error the value is not known to be a number
const amount: number = resolvePath({point: {y: 1}}, "point.y");
// @ts-expect-error the path is text
resolvePath({}, 1);

// A template is parsed into nodes, whatever it is
const nodes: TemplateNode[] = parseTemplate("{point.y}");
parseTemplate(null);
parseTemplate(undefined);
// @ts-expect-error the nodes are not text
const text: string = parseTemplate("a");

// An expression is a number, a path or a call
const number: Expression = {type: "number", value: 1};
const path: Expression = {type: "path", path: "a.b"};
const call: Expression = {type: "call", helper: "gt", args: [number, path]};
// @ts-expect-error a number holds a number
const wrongNumber: Expression = {type: "number", value: "1"};
// @ts-expect-error there is no such expression
const unknownType: Expression = {type: "text"};

// A node is text, an expression or a condition
const node: TemplateNode = nodes[0];
const content: string | undefined = node.type === "text" ? node.value : undefined;
const whenTrue: TemplateNode[] | undefined = node.type === "if" ? node.whenTrue : undefined;
const condition: Expression | undefined = node.type === "if" ? node.condition : undefined;
const spec: string | undefined = node.type === "expression" ? node.spec : undefined;

const result: unknown = evaluate(call, {y: 1});
// @ts-expect-error an expression is needed
evaluate("gt", {});

export {value, amount, nodes, text, number, path, call, wrongNumber, unknownType, node, content, whenTrue, condition, spec, result};
