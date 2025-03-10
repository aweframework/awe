import {withTranslation} from "react-i18next";
import ColumnTextType from "./ColumnTextType";

class ColumnText extends ColumnTextType {

  constructor(props) {
    super(props, "text");
  }
}

export default withTranslation()(ColumnText);