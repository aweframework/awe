import React, {Component} from "react";
import {connect, Provider} from "react-redux";
import {View} from "./View";
import ActionsContainer from "../containers/ActionsContainer";
import {updateSettings} from "../redux/actions/settings";
import FormContainer from "../containers/FormContainer";
import MessageContainer from "../containers/MessageContainer";
import i18n from "../i18n/i18n";
import {getContextPath} from "../utilities";
import {PrimeReactContext} from "primereact/api";

class AweApp extends Component {

  static contextType = PrimeReactContext;

  constructor(props) {
    super(props);
  }

  changeLanguage(language) {
    i18n.changeLanguage(language);
  }

  changeTheme(oldTheme, theme) {
    const { changeTheme } = this.context;
    changeTheme(oldTheme, theme, "theme-link");
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    // Set new language
    if (prevProps.settings.language !== this.props.settings.language) {
      this.changeLanguage(this.props.settings.language);
    }

    // Set new theme
    if (prevProps.settings.theme !== this.props.settings.theme) {
      this.changeTheme(prevProps.settings.theme, this.props.settings.theme);
    }
  }

  render() {
    return <Provider store={this.props.store}>
          <link id="theme-link" rel="stylesheet" href={`${getContextPath()}/css/themes/${this.props.settings.theme}/theme.css`}/>
          <MessageContainer/>
          <FormContainer>
            <View name={"base"} reloadCurrentScreen={this.props.settings.reloadCurrentScreen} initialURL={this.props.settings.initialURL} updateSettings={this.props.updateSettings}/>
          </FormContainer>
          <ActionsContainer/>
        </Provider>
  }
}

function mapStateToProps(state) {
  return {
    settings: state.settings
  }
}

// Connect redux store updates
export default connect(mapStateToProps, {
  updateSettings
})(AweApp);
