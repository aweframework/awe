import {AweComponent} from "./AweComponent";

export class AwePanelableComponent extends AweComponent {

  constructor(props) {
    super(props);
    this.onChange = this.onChange.bind(this);
  }

  getActiveIndex() {
    return this.props.model.values.findIndex(item => item.selected);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {model, updateModelWithDependencies, address} = this.props;
    this.getActiveIndex() < 0 && updateModelWithDependencies(address, {
      values: model.values.map((item, index) => ({
        ...item,
        selected: index === 0
      }))
    });
  }

  onChange(e) {
    const {address, model, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {
      values: model.values.map((item, index) => ({
        ...item,
        selected: index === e.index
      }))
    });
  }
}
