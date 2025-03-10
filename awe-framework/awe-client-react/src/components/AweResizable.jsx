import React from "react";
import {Splitter, SplitterPanel} from "primereact/splitter";
import {bindMethods} from "../utilities";
import {connect} from "react-redux";
import {updateScreen} from "../redux/actions/screen";
import {Components} from "../utilities/structure";

class Resizable extends React.Component {

  /**
   * Create a splitter
   * @param {object} props Component properties
   */
  constructor(props) {
    super(props);

    bindMethods(this, ["resize"]);
  }

  resize(e) {
    this.props.updateScreen({size: {width: window.innerWidth, height: window.innerHeight, current: e.sizes[0]}});
  }

  getLayout(directions) {
    return ["top", "bottom"].includes(directions) ? "vertical" : "horizontal";
  }

  render() {
    const {elementList, directions, style} = this.props;
    return <Splitter layout={this.getLayout(directions)} className={`expand ${style}`}
      onResizeEnd={this.resize}>
      {elementList.map((node, index) => <SplitterPanel key={`splitter-${index}`}
        className="expand expandible-vertical scrollable">{Components(node, index)}</SplitterPanel>)}
    </Splitter>
  }
}

export default connect(state => ({}), {updateScreen})(Resizable);
