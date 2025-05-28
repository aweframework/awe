import {Component} from "react";
import {connect} from "react-redux";
import {
  acceptAction,
  addActions,
  addActionsTop,
  addStack,
  deleteStack,
  rejectAction,
  removeStack,
  startAction
} from "../redux/actions/actions";
import {
  afterSaveRow,
  keepModel,
  updateAttributes,
  updateModel,
  updateModelWithDependencies,
  updateMultipleComponentsWithDependencies,
  updateSpecificAttributes,
  updateValidation
} from "../redux/actions/components";
import {updateSettings} from "../redux/actions/settings";
import {withTranslation} from 'react-i18next';
import {updateBreadcrumbs} from "../redux/actions/screen";

/**
 * Awe Component (parent of all components)
 * @extends React.Component
 * @category Components
 */
export class AweComponent extends Component {

  /**
   * Create a component
   * @param {object} props Component properties
   */
  constructor(props) {
    super(props);
    this.state = {};
    this.previousActions = {runningActions: []};
  }

  /**
   * When component mounts check autoload
   */
  componentDidMount() {
    const {attributes, address, addActionsTop} = this.props;
    const {autoload} = attributes;

    // If autoload, launch a filter of this component
    if (autoload) {
      addActionsTop([{type: "filter", address}]);
    }
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
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    specificAttributes: state.components[id]?.specificAttributes,
    validationRules: state.components[id]?.validationRules,
    context: state.components[id]?.context,
    actions: state.components[id]?.actions || [],
    breadcrumbs: state.screen.breadcrumbs || [],
    currentOption: state.screen.report || {},
    disabled: state.actions.running
  }
}

/**
 * Connect component to redux store
 * @param component Component to connect
 * @returns {function} connect method
 */
export const connectComponent = (component)=> connect(mapStateToProps, {
  updateAttributes,
  updateSpecificAttributes,
  updateModel,
  updateModelWithDependencies,
  updateMultipleComponentsWithDependencies,
  updateValidation,
  addActionsTop,
  addActions,
  startAction,
  acceptAction,
  rejectAction,
  addStack,
  removeStack,
  deleteStack,
  updateSettings,
  keepModel,
  updateBreadcrumbs,
  afterSaveRow
})(withTranslation()(component));
