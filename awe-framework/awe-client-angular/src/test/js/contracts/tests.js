// Application
import "./../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

// Test libraries
import "angular-mocks";

// Contract suites
require("./selectorFamily/selector.contract.js");
require("./selectorFamily/serverData.contract.js");
require("./numericFamily/numeric.contract.js");
require("./numericFamily/serverData.contract.js");
require("./dateTimeFamily/dateTime.contract.js");
require("./dateTimeFamily/serverData.contract.js");
require("./gridFamily/grid.contract.js");
require("./gridFamily/serverData.contract.js");
require("./textFamily/text.contract.js");
require("./textFamily/serverData.contract.js");
