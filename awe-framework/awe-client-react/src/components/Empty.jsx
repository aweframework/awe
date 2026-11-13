import {Components} from "../utilities/structure";

export default (props) => props.elementList.map((node, index) => Components(node, index));
