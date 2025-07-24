import {AweComponent} from "./AweComponent";

export class AwePanelableComponent extends AweComponent {

  constructor(props) {
    super(props);
    this.onChange = this.onChange.bind(this);
  }

  getActiveIndex() {
    const {model = {}} = this.props;
    const {values = []} = model;
    return values.findIndex(item => item.selected);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {updateModelWithDependencies = fn => fn, address = {}, model = {}} = this.props;
    const {values = []} = model;
    this.getActiveIndex() < 0 && updateModelWithDependencies(address, {
      values: values.map((item, index) => ({
        ...item,
        selected: index === 0
      }))
    });
  }

  onChange(e) {
    const {updateModelWithDependencies = fn => fn, address = {}, model = {}} = this.props;
    const {values = []} = model;
    updateModelWithDependencies(address, {
      values: values.map((item, index) => ({
        ...item,
        selected: index === e.index
      }))
    });
  }
}
