import "awe-react-client/css/main.css";
import "../css/main.css";

const { registerTemplate } = require('awe-react-client/js/main');
import { addLocale } from 'primereact/api';

registerTemplate("test", {});
addLocale("ca-ES", {})