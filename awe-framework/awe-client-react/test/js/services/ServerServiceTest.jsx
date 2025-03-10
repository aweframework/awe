import ServerService from "../../../src/services/ServerService";

describe('awe-react-client/test/js/services/ServerServiceTest.jsx', function () {
  const service = new ServerService();
  let props;

  beforeEach(function () {
    props = {
      updateSettings:jasmine.createSpy('updateSettings'),
      settings: {},
      addActionsTop: jasmine.createSpy('addActionsTop')};
  })

  it('should launch a server call', function(done) {
    props.acceptAction = () => done();
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      json: () => Promise.resolve([])
    }));
    let actions = service.callServer({parameters: {}, target: "", address: {}}, [], props);
    expect(Object.keys(actions).length).toBe(0);
  });

});
