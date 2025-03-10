import React from "react";
import {AweView} from "../components/AweView";
import {connect} from "react-redux";
import {updateMessages} from "../redux/actions/messages";
import {clearScreenView, setScreenView} from "../redux/actions/screen";
import {withTranslation} from "react-i18next";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import {updateViewComponentsWithDependencies} from "../redux/actions/components";
import {addActionsTop} from "../redux/actions/actions";

/**
 * SubView container
 * @extends AweView
 * @category Containers
 * @subcategory View
 */
class SubViewContainer extends AweView {

  /**
   * Create the container
   * @param {object} props Container properties
   */
  constructor(props) {
    super(props);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.setState({view: "report"});
  }

  getOptionId() {
    return this.props.match.params.subScreenId || null
  }

  clearView() {
    this.props.clearScreenView(this.state.view);
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
    settings: state.settings
  }
}

// Connect redux store updates
export default connect(mapStateToProps, {
  updateViewComponentsWithDependencies,
  updateMessages,
  setScreenView,
  clearScreenView,
  addActionsTop
})(withTranslation()(SubViewContainer));
