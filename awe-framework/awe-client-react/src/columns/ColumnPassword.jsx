import {withTranslation} from "react-i18next";
import ColumnTextType from "./ColumnTextType";

class ColumnPassword extends ColumnTextType {

  constructor(props) {
    super(props, "password");
  }
}

export default withTranslation()(ColumnPassword);