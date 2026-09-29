export const navigationActions = {
  navigateTo: (target, options = {}) => (dispatch, getState, { navigate }) => {
    navigate(target, options);
    dispatch({ type: "NAVIGATE_TO", payload: target });
  }
};