import React, {useEffect, useRef} from "react";
import View from "./View";
import ActionsContainer from "../containers/ActionsContainer";
import FormContainer from "../containers/FormContainer";
import MessageContainer from "../containers/MessageContainer";

import {getComponentValue} from "../utilities";
import {useDispatch, useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {makeGetComponent} from "../redux/selectors/componentSelectors";
import {applyThemeAppearance} from "../utilities/themeAppearance";
import {refreshThemeAppearanceAction} from "../redux/thunks/screen";

// Mode selector of the info buttons (criterion of the session variable themeMode): undefined while it is not mounted
const getModeSelector = makeGetComponent();
const selectUserMode = (state) => {
  const selector = getModeSelector(state, "currentMode");
  return selector.model ? getComponentValue(selector) ?? "" : undefined;
};

/**
 * Render the theme appearance of the settings on <html>, and read it again from the server when the user changes the
 * mode (the classes of the body do not tell the light mode from no choice)
 * @param {object} settings Settings
 */
function useThemeAppearance(settings) {
  const dispatch = useDispatch();
  const userMode = useSelector(selectUserMode);
  const previousUserMode = useRef(userMode);
  const {themePrimary, themeSurface, themeMode, density, themePalette} = settings;

  useEffect(() => {
    applyThemeAppearance({themePrimary, themeSurface, themeMode, density, themePalette});
  }, [themePrimary, themeSurface, themeMode, density, themePalette]);

  useEffect(() => {
    // A selector that appears or disappears with its screen is not a change of the user
    if (previousUserMode.current !== undefined && userMode !== undefined && previousUserMode.current !== userMode) {
      dispatch(refreshThemeAppearanceAction());
    }
    previousUserMode.current = userMode;
  }, [userMode, dispatch]);
}

function AweApp() {

  const settings = useSelector(selectSettings);
  useThemeAppearance(settings);

  return (
    <>
      <MessageContainer/>
      <FormContainer>
        <View
          name={"base"}
          reloadCurrentScreen={settings.reloadCurrentScreen}
          initialURL={settings.initialURL}
        />
      </FormContainer>
      <ActionsContainer/>
    </>
  );
}

// Connect redux store updates
export default AweApp;
