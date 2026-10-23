import React from "react";
import View from "./View";
import ActionsContainer from "../containers/ActionsContainer";
import FormContainer from "../containers/FormContainer";
import MessageContainer from "../containers/MessageContainer";

import {getContextPath} from "../utilities";
import {useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {useHeadStylesheet} from "../hooks/useDocumentHead";

function AweApp() {

  const settings = useSelector(selectSettings);
  useHeadStylesheet(`${getContextPath()}/css/themes/${settings.theme}/theme.css`);

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
