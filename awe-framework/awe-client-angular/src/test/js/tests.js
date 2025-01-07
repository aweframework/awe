// Application
import "./../../main/resources/js/awe/app";
import "../../main/resources/webpack/locals-en-GB.config";
import "../../main/resources/webpack/locals-es-ES.config";
import "../../main/resources/webpack/locals-eu-ES.config";
import "../../main/resources/webpack/locals-fr-FR.config";

// Tests libraries
import "angular-mocks";

// Tests
import './controllers/tests.js';
import './data/tests.js';
import './filters/tests.js';
import './components/tests.js';
import './services/tests.js';
import './singletons/tests.js';
import './plugins/tests.js';
import './viewers/tests.js';