import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";
import "../../../main/resources/webpack/locals-eu-ES.config";
import "../../../main/resources/webpack/locals-fr-FR.config";

describe('awe-framework/awe-client-angular/src/test/jest/singletons/dependencyController.js', function () {
  let $injector, $dependencyController, $settings;

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function (__$injector__) {
      $injector = __$injector__;
      $dependencyController = $injector.get('DependencyController');
      $settings = $injector.get('AweSettings');

      // Get settings
      jest.spyOn($settings, "get").mockReturnValue("");
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function () {
  });

  it('should register some dependencies', function () {
    // Mock
    let component1 = {id: 'component1', controller: {optionId: "a"}, address: {component: 'component1', view: 'report'}, view: 'report'};
    let dependencies = [{component: component1}, {component: component1}, {component: component1}];

    // Launch and assert
    $dependencyController.register(dependencies, component1);

    expect($dependencyController.dependencies.length).toBe(3);
  });

  it('should unregister a component dependencies', function () {
    // Mock
    let destroy = jest.fn().mockName('destroy');
    let dependency = {belongsTo: () => true, destroy: destroy};
    jest.spyOn(dependency, "belongsTo").mockReturnValue(true);
    let component = {controller: {optionId: ""}, address: {}, dependencies: [dependency]};
    $dependencyController.dependencies = [dependency];


    // Launch and assert
    $dependencyController.unregister(component);

    expect(dependency.belongsTo).toHaveBeenCalled();
    expect(dependency.destroy).toHaveBeenCalled();
    expect(component.dependencies).toEqual([]);
  });

  it('should unregister a view', function () {
    // Mock
    let belongsTo = function(a,o) { return _.isEqual(this.component.address, a);};
    let dependency = {belongsTo: belongsTo, destroy: jest.fn().mockName('destroy')};
    let component1 = {id: 'component1', controller: {optionId: "a"}, address: {component: 'component1', view: 'report'}, view: 'report', dependencies: [dependency]};
    let component2 = {id: 'component2', controller: {optionId: "b"}, address: {component: 'component2', view: 'base'}, view: 'base', dependencies: [dependency, dependency]};
    let component3 = {id: 'component3', controller: {optionId: "a"}, address: {component: 'component3', view: 'report'}, view: 'report', dependencies: [dependency, dependency]};
    let component4 = {id: 'component4', controller: {optionId: "b"}, address: {component: 'component4', view: 'base'}, view: 'base', dependencies: [dependency]};
    let component5 = {id: 'component5', controller: {optionId: "a"}, address: {component: 'component5', view: 'report'}, view: 'report', dependencies: [dependency]};
    $dependencyController.dependencies = [
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component1},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component2},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component3},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component3},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component2},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component4},
      {belongsTo: belongsTo, destroy: dependency.destroy, component: component5}
    ];

    // Launch and assert
    $dependencyController.unregisterView("report");

    expect(dependency.destroy).toHaveBeenCalledTimes(4);
    expect($dependencyController.dependencies.length).toBe(3);
  });

  it('should unregister an empty view', function () {
    // Mock
    let belongsTo = function(a,o) { return _.isEqual(this.component.address, a);};
    let dependency = {belongsTo: belongsTo, destroy: jest.fn().mockName('destroy')};
    $dependencyController.dependencies = [];

    // Launch and assert
    $dependencyController.unregisterView("report");

    expect(dependency.destroy).toHaveBeenCalledTimes(0);
    expect($dependencyController.dependencies.length).toBe(0);
  });
});
