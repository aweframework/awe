import React from "react";
import {Dialog} from 'primereact/dialog';
import {AweComponent, connectComponent} from "./AweComponent";
import {Components} from "../utilities/structure";
import {translateLabel} from "../utilities";

class AweDialog extends AweComponent {

  constructor(props) {
    super(props);
    this.hide = this.hide.bind(this);
  }

  hide() {
    this.props.attributes.isShowing && this.props.addActionsTop([{address: this.props.address, type: "close"}]);
  }

  render() {
    const {t, id, style, label, elementList, attributes} = this.props;
    return (
      <Dialog id={id} className={style} style={{minWidth:"40vw", maxWidth: "90vw"}} header={translateLabel(label, t)} visible={attributes.isShowing} onHide={this.hide}
              focusOnShow={false}>
        {elementList.map((node, index) => Components(node, index))}
      </Dialog>
    );
  }
}

export default connectComponent(AweDialog);
