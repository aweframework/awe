import {aweApplication} from "../awe";
import "../services/geoMap";

// Map directive (<map> screen element, rendered as geo-map)
aweApplication.directive('aweGeoMap',
  ['ServerData', 'GeoMap',
    /**
     * Map directive: the rows of the component as markers over an OpenStreetMap map drawn with Leaflet
     * @param {Object} serverData Server call service
     * @param {Function} GeoMap Map component class
     */
    function (serverData, GeoMap) {
      return {
        restrict: 'E',
        replace: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('geoMap');
        },
        scope: {
          'geoMapId': '@'
        },
        link: function (scope, element) {
          // Init as map
          let component = new GeoMap(scope, scope.geoMapId, element);
          component.asGeoMap();
        }
      };
    }
  ]);
