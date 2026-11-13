// Type-level assertions for redux/actions/dependencies, checked by `npm run typecheck` (not run by Jest)
import type {Dispatch, UnknownAction} from "@reduxjs/toolkit";
import {checkDependencies, forgetInputDependencies, initializeDependencies} from "../../../../../src/redux/actions/dependencies";
import {createStore} from "../../../../../src/redux/store";
import type {RootState} from "../../../../../src/redux/types";

const store = createStore();
const state: RootState = store.getState();
const dispatch: Dispatch<UnknownAction> = store.dispatch;

// The dependencies are checked over the state, dispatching the actions they produce
const checked: void = checkDependencies(state, dispatch);
checkDependencies(state, dispatch, {address: {view: "report", component: "Cri"}, event: "change"});
// @ts-expect-error the state is the root state
checkDependencies("state", dispatch);
// @ts-expect-error the event that launches them has an address and a name
checkDependencies(state, dispatch, {event: "change"});

// The dependencies of a view are initialised once the view is loaded
const initialised: void = initializeDependencies("report", state, dispatch);
// @ts-expect-error the view is text
initializeDependencies(1, state, dispatch);

// The dependencies stored for the restored components are forgotten
forgetInputDependencies([{address: {view: "report", component: "Cri"}}]);
forgetInputDependencies();
// @ts-expect-error the components are a list
forgetInputDependencies({address: {view: "report"}});

export {checked, initialised};
