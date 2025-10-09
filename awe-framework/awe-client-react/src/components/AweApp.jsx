import React from "react";
import View from "./View";
import ActionsContainer from "../containers/ActionsContainer";
import FormContainer from "../containers/FormContainer";
import MessageContainer from "../containers/MessageContainer";

import {getContextPath} from "../utilities";
import {useSelector} from "react-redux";
import {Helmet} from "react-helmet";

function AweApp() {

  const {settings} = useSelector((state) => ({settings: state.settings}));

  return (
    <>
      <Helmet>
        <link
          rel="stylesheet"
          href={`${getContextPath()}/css/themes/${settings.theme}/theme.css`}
        />
      </Helmet>
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
