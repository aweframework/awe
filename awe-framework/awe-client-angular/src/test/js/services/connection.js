describe('awe-framework/awe-client-angular/src/test/js/services/connection.js', function() {
  let $injector, $ajax, $connection;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $ajax = $injector.get('Ajax');
      $connection = $injector.get('Connection');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {  });

  // Send a message
  it('should send a message', function() {
    // Mock
    jest.spyOn($ajax, "sendMessage");

    // Launch
    $connection.sendMessage({values: {serverAction: "test"}});

    // Assert
    expect($ajax.sendMessage).toHaveBeenCalled();
  });

  // Send a message
  it('should send', function() {
    // Mock
    jest.spyOn($ajax, "send");

    // Launch
    $connection.send({values: {serverAction: "test"}});

    // Assert
    expect($ajax.send).toHaveBeenCalled();
  });

  // Send a get request
  it('should launch a get request', function() {
    // Mock
    jest.spyOn($ajax, "get");

    // Launch
    $connection.get("http://server/action/test", "application/json");

    // Assert
    expect($ajax.get).toHaveBeenCalled();
  });

  // Send a post request
  it('should launch a post request', function() {
    // Mock
    jest.spyOn($ajax, "post");

    // Launch
    $connection.post("http://server/action/test", {}, "application/json");

    // Assert
    expect($ajax.post).toHaveBeenCalled();
  });

  // Get file request
  it('should launch a get file', function() {
    // Mock
    jest.spyOn($ajax, "getFile");

    // Launch
    $connection.getFile("http://server/action/test", {}, "application/pdf", "blob");

    // Assert
    expect($ajax.getFile).toHaveBeenCalled();
  });

  // Serialize parameters
  it('should serialize parameters', function() {
    // Mock
    jest.spyOn($ajax, "serializeParameters");

    // Launch
    $connection.serializeParameters({tutu:"lala"});

    // Assert
    expect($ajax.serializeParameters).toHaveBeenCalled();
  });
});