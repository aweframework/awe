import AweCriterionComponent from "./AweCriterionComponent";
import {bindMethods} from "../utilities";

export default class AweTextComponent extends AweCriterionComponent {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onChange", "onBlur", "onSubmit", "getValue"]);

    this.state = {value: this.getValue()};
  }

  onChange(e) {
    // Check if element is active or not (to fix browser criteria autofill issues)
    if (document.activeElement !== e.target) {
      this.props.updateModelWithDependencies(this.props.address, {values: [{value: e.target.value, selected: true}]});
    } else {
      this.setState({value: e.target.value, writing: true});
    }
  }

  onBlur() {
    if (this.getValue() !== this.state.value) {
      this.props.updateModelWithDependencies(this.props.address, {values: [{value: this.state.value, selected: true}]});
    }
    this.setState({writing: false});
  }

  onSubmit() {
    const {address, context, addActionsTop} = this.props;

    // Store data
    this.onBlur();

    // Publish submit action
    addActionsTop([{type: "submit", target: context.source[context.source.length - 2], address}]);
  }

  getValue() {
    const {model = {}} = this.props;
    const {values = []} = model;
    return values.filter(v => v.selected).map(v => v.value).join(", ");
  }

  /**
   * Component was updated
   * @param {object} _prevProps Previous props
   * @param {object} _prevState Previous state
   * @param {object} _snapshot Current snapshot
   */
  componentDidUpdate(_prevProps, _prevState, _snapshot) {
    let newValue = this.getValue();
    if (newValue !== this.state.value && !this.state.writing) {
      this.setState({value: newValue});
    }
  }
}
