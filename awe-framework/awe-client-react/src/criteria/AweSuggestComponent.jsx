import AweCriterionComponent from "./AweCriterionComponent";

import "./AweSuggest.less";
import {bindMethods, componentValue} from "../utilities";
import {suggest} from "../utilities/components";

class AweSuggestComponent extends AweCriterionComponent {

  constructor(props) {
    super(props);
    this.state = {
      suggest: props.model.values.filter(item => item.selected),
      suggestions: [...props.model.values]
    };

    // Bind events
    bindMethods(this, ["onChange", "onSelect", "onClear", "onKeyPress", "suggest", "getComponent"]);

    this.suggesting = false;
    this.abortController = new AbortController();
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {checkTarget, targetAction} = this.props;
    let currentValue = this.state.suggest;
    if ((checkTarget || targetAction) && currentValue.find(item => !("label" in item))) {
      this.autocomplete.search(new Event("init"), componentValue(currentValue.map(item => item.value)), null);
    }

    // Set initial state
    this.setState({suggest: [...currentValue.map(item => ({...item, label: item.label || item.value}))]});
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    const {model} = this.props;
    if (!_.isEqual(prevProps.model.values, model.values)) {
      this.setState({suggest: model.values.filter(v => v.selected)});
    }
  }

  onChange(e) {
    // Update suggest
    if (this.state.suggest !== e.value) {
      this.setState({suggest: e.value});
    }
  }

  onClear() {
    const {address, updateModelWithDependencies} = this.props;

    // Clear suggest
    updateModelWithDependencies(address, {values: []});
  }

  onSelect(e) {
    const {address, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {values: [e.value].flat().map(item => ({...item, selected: true}))});
  }

  onKeyPress(e) {
    if (e.code === "Space" && e.target.value === "") {
      this.autocomplete.onDropdownClick();
    }
  }

  suggest(event) {
    const {originalEvent, query} = event;
    suggest(this, originalEvent, query);
  }
}

export default AweSuggestComponent;
