const InitialState = {
  currentScreen: null,
};

export function navigation(state = InitialState, action = {}) {
  switch (action.type) {
    case 'NAVIGATE_TO':
      return {
        ...state,
        currentScreen: action.payload,
      };
    default:
      return state;
  }
}