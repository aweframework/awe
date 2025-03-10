import React, {Component} from "react";
import {connect} from "react-redux";
import {StackList} from "../components/Actions";
import {updateSettings} from "../redux/actions/settings";

const DIGITS = ["Digit0", "Digit1", "Digit2", "Digit3", "Digit4", "Digit5", "Digit6", "Digit7", "Digit8", "Digit9"];

/**
 * Actions container
 * @extends React.Component
 * @category Containers
 */
class ActionsContainer extends Component {

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
    // Activate actions stack
    window.onkeydown = (event) => {
      if (event.altKey && event.shiftKey && DIGITS.includes(event.code)) {
        // Toggle stack
        this.props.updateSettings({actionsStack: DIGITS.indexOf(event.code) * 1000});
      }
    }
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {isShowing, asyncStack, syncStack} = this.props;
    return <div
      className="actions-zone" style={{display: isShowing ? "block" : "none"}}>
      <StackList type={"async"} elements={asyncStack}/>
      <StackList type={"sync"} stacks={syncStack}/>
    </div>;
  }
}

function mapStateToProps(state) {
  return {
    isShowing: state.settings.actionsStack > 0,
    running: state.actions.running,
    currentStack: state.actions.sync[state.actions.sync.length - 1],
    syncStack: state.actions.sync,
    asyncStack: state.actions.async
  };
}

// Connect redux store updates
export default connect(mapStateToProps, {updateSettings})(ActionsContainer);