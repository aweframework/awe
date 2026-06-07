describe('awe-framework/awe-client-angular/src/test/js/services/serverData.js', function() {
  let $injector, $serverData, $storage, $connection, $log, $actionController;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $serverData = $injector.get('ServerData');
      $storage = $injector.get('Storage');
      $connection = $injector.get('Connection');
      $actionController = $injector.get('ActionController');
      $log = $injector.get('$log');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  it('should retrieve form values for printing (empty)', function() {
    jest.spyOn($storage, "get").mockReturnValue({});
    expect($serverData.getFormValuesForPrinting()).toEqual({});
  });

  it('should retrieve form values for printing (some values)', function() {
    jest.spyOn($storage, "get").mockReturnValue({report:{reportOrientation:{selected:"LANDSCAPE", getPrintData: () => ({tutu: "lala"})}, otro: {}}});
    $log.debug($serverData.getFormValuesForPrinting());
    expect($serverData.getFormValuesForPrinting()).toEqual({tutu: "lala"});
  });

  it('should get template url', function() {
    jest.spyOn($connection, "getRawUrl").mockReturnValue("");
    expect($serverData.getTemplateUrl("option", "view")).toBe("/template/screen/view/option");
    expect($serverData.getTemplateUrl("", "view")).toBe("/template/screen");
  });

  it('should get screen data', async function() {
    jest.spyOn($actionController, "addActionList");
    jest.spyOn($connection, "post").mockReturnValue(Promise.reject({
      status: 500,
      data: { title: "Internal Error", message: "Something went wrong" }
    }));

    await $serverData.getScreenData("option", "view");
    expect($actionController.addActionList).toHaveBeenCalled();
  });
});