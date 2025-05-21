import React from "react";
import {bindMethods, fetchHtml, getRestUrl, translateLabel} from "../utilities";
import {AweWidget, connectWidget} from "./AweWidget";
import "./AweHelpViewer.less";
import {Skeleton} from "primereact/skeleton";
import {isEmpty} from "../utilities/general";

/**
 * AWE Help Viewer component
 * @extends AweWidget
 * @category Widgets
 */
class AweHelpViewer extends AweWidget {

  constructor(props) {
    super(props);
    this.state = {help: ""};
    bindMethods(this, ["fetchHelp", "translateHelp"]);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.fetchHelp();
  }

  /**
   * Retrieve help data
   */
  fetchHelp() {
    const {settings, currentOption} = this.props;
    const {option = null} = currentOption;
    fetchHtml(getRestUrl("template", "help", option), settings.token)
      .then((help) => this.setState({help}))
      .catch((reason) => console.error("Error reading help file:", reason));
  }

  /**
   * Translate help
   * @param {string} help String
   * @return {string} translated text
   */
  translateHelp(help) {
    const {t} = this.props;
    return help.replace(/\{t\{'([\w\.\$ ]*)'\}\}/g, (m, text) => translateLabel(text, t));
  }

  skeletonTemplate(key) {
    return <li className="mb-3" key={`skeleton-${key}`}>
      <div className="flex">
        <Skeleton shape="circle" size="4rem" className="mr-2"/>
        <div style={{flex: '1'}}>
          <Skeleton width="100%" className="mb-2"/>
          <Skeleton width="75%"/>
        </div>
      </div>
    </li>;
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {id} = this.props;
    const {help} = this.state;
    if (isEmpty(help)) {
      const skeletons = [1, 2, 3, 4, 5];
      return <ul className="m-0 p-0" style={{width: "800px"}} role="alert" aria-busy="true">
        {skeletons.map(this.skeletonTemplate)}
      </ul>;
    } else {
      return <div className="help-viewer m-4" id={id} dangerouslySetInnerHTML={{__html: this.translateHelp(help)}}/>;
    }
  }
}

export default connectWidget(AweHelpViewer);
