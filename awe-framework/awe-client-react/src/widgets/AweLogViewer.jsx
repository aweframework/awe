import React from "react";
import {LazyLog} from 'react-lazylog';
import {bindMethods, fetchAction, getIconCode} from "../utilities";
import {getFormValues} from "../utilities/components";
import {Button} from "primereact/button";
import "./AweLogViewer.less";
import {AweWidget, connectWidget} from "./AweWidget";
import {isEmpty} from "../utilities/general";

/**
 * AWE Log Viewer component
 * @extends AweWidget
 * @category Widgets
 */
class AweLogViewer extends AweWidget {

  constructor(props) {
    super(props);
    this.state = {offset: 1, text: " ", showLoadingDots: true};
    this.loadingTimeout = null;
    this.lastAutorefresh = props.attributes.autorefresh;
    bindMethods(this, ["fetchLog", "updateLog", "toggleAutoRefresh", "checkAutoRefresh"]);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    setTimeout(this.fetchLog, 100);
  }

  /**
   * Component will be unmounted
   */
  componentWillUnmount() {
    clearTimeout(this.loadingTimeout);
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevProps.attributes.autorefresh !== this.props.attributes.autorefresh) {
      this.checkAutoRefresh();
    }
  }

  /**
   * Call for log delta
   */
  fetchLog() {
    const {serverAction, targetAction, settings} = this.props;
    fetchAction(serverAction, targetAction, {...getFormValues(this.props), offset: this.state.offset}, settings.token)
      .then(this.updateLog)
      .catch((reason) => console.error("Error reading log file:", reason));
  }

  /**
   * Update log data from actions
   * @param {object[]} actions Action list
   */
  updateLog(actions = []) {
    const {offset = 1, text = " "} = this.state;
    const newLines = actions.filter(a => a.type === "log-delta").map(a => a.parameters.log).flat();
    this.setState({offset: offset + newLines.length, text: (isEmpty(text.trim()) ? "" : text + "\n") + newLines.join("\n")});
    this.checkAutoRefresh();
  }

  /**
   * Check if autorefresh must be active or not
   */
  checkAutoRefresh() {
    const {autorefresh} = this.props.attributes;
    if (autorefresh) {
      this.loadingTimeout = setTimeout(() => this.fetchLog(), autorefresh * 1000);
      this.lastAutorefresh = autorefresh;
      this.setState({showLoadingDots: true});
    } else {
      this.setState({showLoadingDots: false});
    }
  }

  /**
   * Turn on/off autorefresh on log viewer
   */
  toggleAutoRefresh() {
    const {updateAttributes, address} = this.props;
    clearTimeout(this.loadingTimeout);
    updateAttributes(address, {autorefresh: this.state.showLoadingDots ? 0 : this.lastAutorefresh});
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {id} = this.props;
    const {text, offset, showLoadingDots} = this.state;
    return <div className={"expand expandible-vertical panel-body p-0 log-container"} id={id}>
      <Button data-testid="autoload-button"
              className={"p-button-text p-button-rounded log-button-autoload"}
              icon={getIconCode("refresh", showLoadingDots ? "fa-spin" : "")}
              onClick={this.toggleAutoRefresh}/>
      <LazyLog text={text} scrollToLine={offset} enableSearch caseInsensitive selectableLines extraLines={1}/>
      <div className={"log-loading-dots " + (!showLoadingDots ? "hidden" : "")}>
        {getIconCode("circle", "fa-fw fade1 animation-dot")}
        {getIconCode("circle", "fa-fw fade2 animation-dot")}
        {getIconCode("circle", "fa-fw fade3 animation-dot")}
      </div>
    </div>;
  }
}

export default connectWidget(AweLogViewer);
