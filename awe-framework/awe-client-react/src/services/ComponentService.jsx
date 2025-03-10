import {ButtonTypes} from "../redux/actions/components";
import {getActionAddress, getComponent} from "../utilities";

const {BUTTON_RESET} = ButtonTypes;

/**
 * Component service
 * @category Services
 */
class ComponentService {

  getActions() {
    return {
      // Button
      "click": this.onButtonClick.bind(this),

      // Uploader
      "file-uploaded": this.onUploaderFinishUpload.bind(this),
      "clear-file": this.onUploaderDelete.bind(this),

      // Dialog
      "dialog": this.onDialogOpen.bind(this),
      "close": this.onDialogClose.bind(this),
      "close-cancel": this.onDialogCloseAndCancel.bind(this),

      // Wizard
      "next-step": this.onNextStep.bind(this),
      "prev-step": this.onPrevStep.bind(this),
      "first-step": this.onFirstStep.bind(this),
      "last-step": this.onLastStep.bind(this),
      "nth-step": this.onNthStep.bind(this),

      // Chart
      "add-points": this.onAddPoints.bind(this),
      "add-chart-series": this.onAddSeries.bind(this),
      "remove-chart-series": this.onRemoveSeries.bind(this),
      "replace-chart-series": this.onReplaceSeries.bind(this),

      // Pivot
      "set-pivot-sorters": this.onSetPivotSorters.bind(this),
      "set-pivot-group-rows": this.onSetPivotGroupRows.bind(this),
      "set-pivot-group-cols": this.onSetPivotGroupCols.bind(this),

      // Menu
      "toggle-menu": this.toggleMenu.bind(this),
      "toggle-navbar": this.toggleNavbar.bind(this),
      "change-menu": this.changeMenu.bind(this),
    }
  }

  onButtonClick(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);

    if (component.actions.length > 0) {
      props.addActionsTop(component.actions.map(item => ({...item, address: {...component.address}})));
    } else if (BUTTON_RESET === component.attributes.buttonType) {
      props.addActionsTop([{type: "restore", address: {...component.address}}]);
    }

    // Accept action
    props.acceptAction(action);
  }

  onUploaderFinishUpload(action, props) {
    const {name, path, size, type} = action.parameters;

    // Accept action
    props.acceptAction(action);

    // Change component data
    const address = getActionAddress(action);
    props.updateModelWithDependencies(address, {
      values: [{
        value: path,
        label: name,
        size: size,
        type: type,
        selected: true
      }]
    });
  }

  onUploaderDelete(action, props) {
    // Accept action
    props.acceptAction(action);

    // Change component data
    const address = getActionAddress(action);
    props.resetModelWithDependencies(address);
  }

  onDialogOpen(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {isShowing} = component.attributes;
    if (!isShowing) {
      props.addStack();
      props.updateAttributes(address, {isShowing: true, action: action});
    }
  }

  onDialogClose(action, props) {
    const address = getActionAddress(action);
    props.acceptAction(action);
    const component = getComponent(props.components, address);
    const {isShowing} = component.attributes;
    if (isShowing) {
      props.removeStack();
      props.acceptAction(component.attributes.action);
      props.updateAttributes(address, {isShowing: false, action: null});
    }
  }

  onDialogCloseAndCancel(action, props) {
    const address = getActionAddress(action);
    props.acceptAction(action);
    const component = getComponent(props.components, address);
    const {isShowing} = component.attributes;
    if (isShowing) {
      props.removeStack();
      props.rejectAction(component.attributes.action);
      props.updateAttributes(address, {isShowing: false, action: null});
    }
  }


  onNextStep(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    const nextIndex = Math.min(component.model.values.findIndex(item => item.selected) + 1, component.model.values.length - 1);
    updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === nextIndex
      }))
    });

    // Accept action
    acceptAction(action);
  }

  onPrevStep(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    const nextIndex = Math.max(component.model.values.findIndex(item => item.selected) - 1, 0);
    updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === nextIndex
      }))
    });

    // Accept action
    acceptAction(action);
  }

  onFirstStep(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === 0
      }))
    });

    // Accept action
    acceptAction(action);
  }

  onLastStep(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    updateModelWithDependencies(address, {
      values: component.model.values.map((item, index) => ({
        ...item,
        selected: index === component.model.values.length - 1
      }))
    });

    // Accept action
    acceptAction(action);
  }

  onNthStep(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    updateModelWithDependencies(address, {
      values: component.model.values.map(item => ({
        ...item,
        selected: item.value === action.parameters.value
      }))
    });

    // Accept action
    acceptAction(action);
  }

  onAddPoints(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateModelWithDependencies} = props;

    // Change state
    updateModelWithDependencies(address, {
      values: [...component.model.values, action.parameters.value]
    });

    // Accept action
    acceptAction(action);
  }

  onAddSeries(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateAttributes, updateModelWithDependencies} = props;
    const series = action.parameters.series || [];
    const currentSeries = (component.attributes?.chartModel?.series || []).filter(serie => !series.map(s => s.id).includes(serie.id));

    // Add serie
    updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: [
          ...currentSeries,
          ...series
        ]
      }
    });

    const values = [...currentSeries, ...series].map((s) => s.data.map(([k, v]) => ({[s.xValue]:k, [s.yValue]: v})))
      .reduce((t, a) => [...a.map((ar, i) => ({...component.model.values[i] || {}, ...ar, ...t[i] || {}}))], []);

    // Change model
    updateModelWithDependencies(address, { values });

    // Accept action
    acceptAction(action);
  }

  onRemoveSeries(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateAttributes} = props;
    const series = action.parameters.series || [];
    const currentSeries = (component.attributes?.chartModel?.series || []).filter(serie => !series.map(s => s.id).includes(serie.id));

    // Remove serie
    updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: currentSeries
      }
    });

    // Accept action
    acceptAction(action);
  }

  onReplaceSeries(action, props) {
    const address = getActionAddress(action);
    const component = getComponent(props.components, address);
    const {acceptAction, updateAttributes, updateModelWithDependencies} = props;
    const series = action.parameters.series || [];

    // Replace serie
    updateAttributes(address, {
      chartModel: {
        ...component.attributes.chartModel,
        series: [
          ...series
        ]
      }
    });

    const values = [...series].map((s) => s.data.map(([k, v]) => ({[s.xValue]:k, [s.yValue]: v})))
      .reduce((t, a) => [...a.map((ar, i) => ({...ar, ...t[i] || {}}))], []);

    // Change model
    updateModelWithDependencies(address, { values });

    // Accept action
    acceptAction(action);
  }

  onSetPivotSorters(action, props) {
    const address = getActionAddress(action);
    const {acceptAction, updateAttributes} = props;
    const { parameters = {}}  = action;
    const { sorters = {} } = parameters;

    // Change attributes
    updateAttributes(address, {sorters});

    // Accept action
    acceptAction(action);
  }

  onSetPivotGroupRows(action, props) {
    const address = getActionAddress(action);
    const {acceptAction, updateAttributes} = props;
    const { parameters = {}}  = action;
    const { rows = "" } = parameters;

    // Change attributes
    updateAttributes(address, {rows});

    // Accept action
    acceptAction(action);
  }

  onSetPivotGroupCols(action, props) {
    const address = getActionAddress(action);
    const {acceptAction, updateAttributes} = props;
    const { parameters = {}}  = action;
    const { cols = "" } = parameters;

    // Change attributes
    updateAttributes(address, {cols});

    // Accept action
    acceptAction(action);
  }

  toggleMenu(action, props) {
    const address = getActionAddress(action);
    const {acceptAction, components, updateAttributes} = props;
    const component = getComponent(components, address);

    // Toggle minimized attribute
    updateAttributes(address, {minimized: !component.attributes.minimized});

    // Accept action
    acceptAction(action);
  }

  toggleNavbar(action, props) {
    const {acceptAction} = props;

    // Fake action, do nothing

    // Accept action
    acceptAction(action);
  }

  changeMenu(action, props) {
    const {options} = action.parameters;
    const {acceptAction, updateMenu} = props;

    // Change menu
    updateMenu(options);

    // Accept action
    acceptAction(action);
  }
}

export default ComponentService;
