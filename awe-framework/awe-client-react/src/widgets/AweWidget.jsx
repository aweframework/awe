import {Component} from "react";
import {connect} from "react-redux";
import {addActions, addActionsTop} from "../redux/actions/actions";
import {updateAttributes} from "../redux/actions/components";
import {withTranslation} from 'react-i18next';

/**
 * Awe Widget (parent of all widgets)
 * @extends React.Component
 * @category Widgets
 */
export class AweWidget extends Component {

  /**
   * Create a component
   * @param {object} props Component properties
   */
  constructor(props) {
    super(props);
    this.state = {};
    this.previousActions = {runningActions: []};
  }
}

/**
 * Map state to props
 * @param state State
 * @param ownProps Properties
 * @returns {object} Properties to map
 */
function mapStateToProps(state, ownProps) {
  const {id} = ownProps;
  return {
    settings: state.settings,
    components: state.components,
    address: state.components[id].address,
    model: state.components[id].model,
    attributes: state.components[id].attributes,
    context: state.components[id].context,
    actions: state.components[id].actions || [],
    breadcrumbs: state.screen.breadcrumbs || [],
    currentOption: state.screen.report || {},
    disabled: state.actions.running
  };
}

/**
 * Connect component to redux store
 * @param component Component to connect
 * @returns {function} connect method
 */
export const connectWidget = (component) => connect(mapStateToProps, {
  updateAttributes,
  addActionsTop,
  addActions,
})(withTranslation()(component));
