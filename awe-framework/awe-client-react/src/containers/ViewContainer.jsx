import React from "react";
import {AweView} from "../components/AweView";
import {checkActions, combineActions} from "../utilities";
import FormService from "../services/FormService";
import ScreenService from "../services/ScreenService";
import MessageService from "../services/MessageService";
import {connect} from "react-redux";
import {
  afterSaveRow,
  keepModel,
  resetModelWithDependencies,
  resetMultipleModelWithDependencies,
  restoreMultipleModelWithDependencies,
  updateAttributes,
  updateModel,
  updateModelWithDependencies,
  updateSpecificAttributes,
  updateViewComponentsWithDependencies,
  validateComponents,
  validateRow
} from "../redux/actions/components";
import {addMessage, confirmMessage, updateMessages} from "../redux/actions/messages";
import {clearAllViews, clearScreenView, setScreenView, updateScreen} from "../redux/actions/screen";
import {updateSettings} from "../redux/actions/settings";
import {
  acceptAction,
  ActionStatus,
  addActions,
  addActionsTop,
  addStack,
  deleteStack,
  rejectAction,
  removeStack,
  startAction
} from "../redux/actions/actions";
import {withTranslation} from "react-i18next";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import WebsocketService from "../services/WebsocketService";
import ComponentService from "../services/ComponentService";
import GridService from "../services/components/GridService";
import {updateMenu} from "../redux/actions/menu";

const {STATUS_RUNNING} = ActionStatus;

/**
 * View container
 * @extends AweView
 * @category Containers
 * @subcategory View
 */
class ViewContainer extends AweView {

  /**
   * Create the container
   * @param {object} props Container properties
   */
  constructor(props) {
    super(props);

    this.updateWindowDimensions = _.debounce(this.updateWindowDimensions.bind(this), 100);
    this.clearView = this.clearView.bind(this);

    this.previousActions = {runningActions: []};
    this.websocketService = new WebsocketService(props);
    this.combinedActions = combineActions(
      new FormService(),
      new ScreenService(),
      new MessageService(props),
      new ComponentService(),
      new GridService(),
      this.websocketService
    );

    // Connect websocket
    this.websocketService.connectWebsocket(null, this.props);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.setState({view: "base"});

    this.updateWindowDimensions();
    window.addEventListener('resize', this.updateWindowDimensions);
  }

  /**
   * Component was unmounted
   */
  componentWillUnmount() {
    window.removeEventListener('resize', this.updateWindowDimensions);

    // Connect websocket
    this.websocketService.disconnectWebsocket(null, this.props);
  }

  updateWindowDimensions() {
    this.props.updateScreen({size: {width: window.innerWidth, height: window.innerHeight}});
  }

  getOptionId() {
    return this.props.match.params.screenId || null
  }

  clearView() {
    this.props.clearAllViews();
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    // Parent execution
    super.componentDidUpdate(prevProps, prevState, snapshot);

    // Check generic actions
    checkActions(this.combinedActions, this.previousActions, this.props);

    // Store previous actions to check (because react-router dirties prevProps)
    this.previousActions = {runningActions: [...this.props.runningActions]};
  }

  getComponentRender() {
    const {t} = this.props;
    return <div className={"expand expandible-vertical"}>
      <Helmet>
        <title>{t(this.state.screen.title)}</title>
      </Helmet>
      {Templates(this.state.structure)}
    </div>;
  }
}

function mapStateToProps(state) {
  return {
    components: state.components,
    messages: state.messages,
    settings: state.settings,
    router: state.router,
    runningActions: [
      ...state.actions.sync[state.actions.sync.length - 1].filter(a => a.status === STATUS_RUNNING),
      ...state.actions.async.filter(a => a.status === STATUS_RUNNING)
    ]
  }
}

// Connect redux store updates
export default connect(mapStateToProps, {
  updateViewComponentsWithDependencies,
  resetModelWithDependencies,
  addMessage,
  updateMessages,
  confirmMessage,
  setScreenView,
  clearAllViews,
  clearScreenView,
  updateScreen,
  updateSettings,
  addActions,
  addActionsTop,
  startAction,
  acceptAction,
  rejectAction,
  addStack,
  removeStack,
  deleteStack,
  updateModel,
  updateModelWithDependencies,
  resetMultipleModelWithDependencies,
  restoreMultipleModelWithDependencies,
  updateAttributes,
  updateSpecificAttributes,
  validateComponents,
  validateRow,
  afterSaveRow,
  keepModel,
  updateMenu
})(withTranslation()(ViewContainer));
