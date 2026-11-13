import type {AppThunk} from "../types";

export const navigationActions = {
  navigateTo: (target: string | number, options: Record<string, unknown> = {}): AppThunk => (dispatch, _getState, { navigate }) => {
    navigate(target, options);
    dispatch({ type: "NAVIGATE_TO", payload: target });
  }
};