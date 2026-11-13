// Type-level assertions for utilities/testIds, checked by `npm run typecheck` (not run by Jest)
import {TestAttributes, TestId, TestIds, testHook} from "../../../../src/utilities/testIds";

// The vocabulary is made of literals
const grid: "grid" = TestIds.grid;
const attribute: "data-testid" = TestAttributes.testId;
const id: TestId = TestIds.selectOption;
// @ts-expect-error a value of the vocabulary is a literal, not any text
const wrongId: "select" = TestIds.grid;
// @ts-expect-error there is no such part
const unknownId = TestIds.unknownPart;
// @ts-expect-error the vocabulary is frozen
TestIds.grid = "other";
// @ts-expect-error a test id is one of the vocabulary
const notAnId: TestId = "not-in-the-vocabulary";

// A hook is an object of attributes
const hook: Record<string, unknown> = testHook(TestIds.grid);
const state: Record<string, unknown> = testHook(TestIds.selectOption, {owner: "Sel", selected: true, disabled: false, open: null});
const full: Record<string, unknown> = testHook(TestIds.alert, {
  type: "danger", container: "body", attributes: {"row-id": 3, "column-id": "name"}
});
// @ts-expect-error the hook is not text
const hookText: string = testHook(TestIds.grid);
// @ts-expect-error the test id is one of the vocabulary
testHook("not-in-the-vocabulary");
// @ts-expect-error there is no such state
testHook(TestIds.grid, {selectd: true});
// @ts-expect-error the owner is text
testHook(TestIds.grid, {owner: 1});

export {grid, attribute, id, wrongId, unknownId, notAnId, hook, state, full, hookText};
