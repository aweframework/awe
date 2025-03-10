import React, {Component} from "react";

/**
 * AWE File Manager component
 * @extends React.Component
 * @category Widgets
 */
class AweFileManager extends Component {
  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {id} = this.props;
    return <div className={"expand expandible-vertical"} id={id}>
      <iframe src={"/fm/home"} className={"expand"} style={{border: "none"}}/>
    </div>;
  }
}

export default AweFileManager;
