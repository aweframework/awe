import FormService from "../../../src/services/FormService";

describe('awe-react-client/test/js/services/FormServiceTest.jsx', () => {
  const service = new FormService();

  beforeEach(function () {
  });

  it('should get all form actions', () => {
    let actions = service.getActions();
    expect(Object.keys(actions).length).toBe(23);
  });

});
