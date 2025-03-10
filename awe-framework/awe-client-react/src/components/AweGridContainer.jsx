import React, {Component, createRef} from "react";
import {connect} from "react-redux";
import {bindMethods} from "../utilities";

/**
 * AWE Grid Container component
 * @extends Component
 * @category Components
 * @subcategory Grid
 */
class AweGridContainer extends Component {

  /**
   * Create a grid container
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);

    this.containerRef = createRef();
    this.state = {containerHeight: 0};

    bindMethods(this, ["checkKey", "contextMenu"]);
  }

  checkKey(e) {
    const {onKeyCancelRow, onKeySaveRow} = this.props;
    switch (e.key) {
      case "Escape":
        onKeyCancelRow();
        break;
      case "Enter":
        document.body.focus();
        setTimeout(onKeySaveRow, 100);
        break;
      default:
        // do nothing
        break;
    }
  }

  contextMenu(e) {
    const {onContextMenu} = this.props;
    onContextMenu({originalEvent: e, data: null, index: -1});
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.setState({
      containerHeight: this.containerRef?.current?.clientHeight || 0
    });
  }

  /**
   * Component was updated
   * @param {object} _prevProps Previous props
   * @param {object} _prevState Previous state
   * @param {object} _snapshot Current snapshot
   */
  componentDidUpdate(_prevProps, _prevState, _snapshot) {
    const {containerHeight} = this.state;
    if (containerHeight === undefined) {
      this.setState({containerHeight: this.containerRef?.current?.clientHeight});
    }
  }

  shouldComponentUpdate(nextProps, _nextState, _nextContext) {
    const {screenSize} = this.props;
    if (screenSize?.height !== nextProps.screenSize?.height || screenSize?.current !== nextProps.screenSize?.current) {
      this.setState({containerHeight: undefined});
    }
    return true;
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {containerHeight = 0} = this.state;
    return <div className="grid-container expand expandible-vertical" ref={this.containerRef}>
      <div className="grid-contents expand expandible-vertical" role="grid-container" /*style={{height: `${containerHeight}px`}}*/ onKeyDown={this.checkKey} onContextMenu={this.contextMenu}>
        {this.props.children}
      </div>
    </div>;
  }
}

export default connect(state => ({screenSize: state.screen.size}), {})(AweGridContainer);
