import React, {Component} from "react";
import {
  fixController,
  fixModel,
  getFormValues,
  getSpecificAttributes,
  inspectComponentStructure,
  parseValidationRules,
} from "../utilities/components";
import {ProgressSpinner} from "primereact/progressspinner";
import {fetchScreen, generateMessageAction} from "../utilities";

export class AweView extends Component {

  constructor(props) {
    super(props);
    this.state = {structure: props.structure || {elementList: []}, screen: {}, loading: true};
  }

  onRouteChanged() {
    // Load screen data for view
    let option = this.getOptionId();

    // Set loaded false state
    this.setState({loading: true, option: option});

    // Clear screen view
    this.clearView();

    fetchScreen(option, this.state.view, this.props.settings.token, getFormValues(this.props))
      .then(response => {
        if ("structure" in response) {
          this.drawScreen(response)
        } else {
          this.manageScreenError(option, response);
        }
      });
  }

  manageScreenError(option, error = {}) {
    const {t, addActionsTop} = this.props;
    console.error(`Error retrieving screen structure: ${option}`, error);
    const {status} = error;
    switch (status) {
      case 401:
      case 403:
        // Send action confirm
        addActionsTop([
          {type: "screen", target: ""},
          {...generateMessageAction("warning", t('ERROR_TITLE_SESSION_EXPIRED'), t('ERROR_MESSAGE_SESSION_EXPIRED')), async: true}
        ]);
        break;
      default:
        // Internal error: Show message
        addActionsTop([generateMessageAction("error", t('ERROR_TITLE_SCREEN_GENERATION_ERROR'), t(error.message))]);
        break;
    }
  }

  drawScreen(response = {}) {
    const {settings, updateViewComponentsWithDependencies, updateMessages, setScreenView, updateMenu} = this.props;

    // Read structure and generate micro contexts
    const componentStructure = inspectComponentStructure(response.structure, [], {});

    // Store components
    updateViewComponentsWithDependencies(this.state.view, response.components.reduce((list = {}, component = {}) => {
      const isGrid = "columnModel" in component.controller
      const address = {view: this.state.view, component: component.id}
      const model = fixModel(component.model, isGrid);
      const controller = fixController(component.controller, isGrid, settings);
      const specificAttributes = getSpecificAttributes(component.controller, isGrid, settings);
      const validationRules = parseValidationRules(controller.validation, address);
      return {
        ...list,
        [component.id]: {
          uid: component.id,
          address: {...address},
          model: {...model},
          storedModel: {...model},
          attributes: {...controller},
          specificAttributes: {...specificAttributes},
          storedAttributes: {...controller},
          validationRules: {...validationRules},
          storedValidationRules: {...validationRules},
          actions: component.controller.actions || [],
          dependencies: component.controller.dependencies || [],
          contextMenu: component.controller.contextMenu || [],
          context: {view: address.view, source: [...(componentStructure[component.id] || [])]}
        }
      };
    }, {}));

    // Get MainMenu if it exists
    const menu = response.components.find((component) => component.id === "MainMenu");
    if (menu) {
      updateMenu(menu.controller.options);
    }

    // Store messages
    updateMessages(this.state.view, response.messages);

    // Store screen
    setScreenView(this.state.view, response.screen);

    // Draw screen based on screen data
    this.setState({structure: response.structure, screen: response.screen, loading: false});
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (this.getOptionId() !== this.state.option) {
      this.onRouteChanged();
    }
  }

  render() {
    if (this.state.loading) {
      return <div className="expand grid animate__animated animate__fadeIn"><ProgressSpinner className="p-col align-self-center"/></div>;
    } else {
      return this.getComponentRender();
    }
  }
}
