import {aweApplication} from "../awe";
import {DefaultSpin} from "./../data/options";
import "../services/chart/chart";

// Chart directive
aweApplication.directive('aweChart',
  ['ServerData', 'Chart',
    function (serverData, Chart) {
      return {
        restrict: 'E',
        replace: true,
        transclude: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('chart');
        },
        scope: {
          'chartId': '@'
        },
        link: function (scope, element) {
          // Set spin options
          scope.spinOptions = DefaultSpin.big;

          // Init as chart: the chart plugin draws the ECharts model of the component
          let  component = new Chart(scope, scope.chartId, element);
          component.asChart();
        }
      };
    }]);
