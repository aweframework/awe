import ScreenService from "../../../src/services/ScreenService";
import React from 'react';
import {render} from "@testing-library/react";

describe('awe-react-client/test/js/services/ScreenServiceTest.jsx', function () {
  const service = new ScreenService();
  let history;
  let historyList;
  let props;

  beforeEach(function () {
    historyList = [];
    history = {push: (e) => historyList.push(e), go: jasmine.createSpy('historyGo'), goBack: jasmine.createSpy('historyGoBack')};
    props = {updateSettings:jasmine.createSpy('updateSettings'), acceptAction: jasmine.createSpy('accept'), history: history, settings: {
      targetActionKey: "targetAction"
      }};
  })

  // Get all screen actions
  it('should get all screen actions', function() {
    let actions = service.getActions();
    expect(Object.keys(actions).length).toBe(14);
  });

  // Launch screen action erasing the token
  it('should launch a screen action erasing the token', function() {
    service.screen({parameters:{language:"es", theme:"clean", token: null}, context: "screen", target: "signin", reload: false}, props);
    expect(historyList.length).toBe(1);
    expect(historyList[0]).toBe("/screen/signin");

    // Spies
    expect(props.updateSettings).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.go).not.toHaveBeenCalled();
  });

  // Launch screen action
  it('should launch a screen action with token', function() {
    service.screen({parameters:{target: "signin", token: "allalla"}, reload: false}, props);
    expect(historyList.length).toBe(1);
    expect(historyList[0]).toBe("signin");

    // Spies
    expect(props.updateSettings).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.go).not.toHaveBeenCalled();
  });

  // Launch screen action
  it('should launch a screen action with screen', function() {
    service.screen({parameters:{screen: "/"}}, props);
    expect(historyList.length).toBe(1);
    expect(historyList[0]).toBe("/");

    // Spies
    expect(props.updateSettings).not.toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.go).not.toHaveBeenCalled();
  });

  // Launch screen with reload
  it('should launch a screen with reload', function() {
    let props = {updateSettings:jasmine.createSpy('updateSettings'), acceptAction: jasmine.createSpy('accept'), history: history, settings: {reloadCurrentScreen: true}};
    service.screen({parameters:{screen: "/context.html"}}, {...props, settings: {reloadCurrentScreen: true}});
    expect(historyList.length).toBe(0);

    // Spies
    expect(props.updateSettings).not.toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.go).toHaveBeenCalled();
  });

  // Launch screen with no further action
  it('should launch a screen with no further action', function() {
    service.screen({parameters:{screen: "/context.html"}}, props);
    expect(historyList.length).toBe(0);

    // Spies
    expect(props.updateSettings).not.toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.go).not.toHaveBeenCalled();
  });

  // Go back
  it('should go back', function() {
    service.back({}, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(history.goBack).toHaveBeenCalled();
  });

  // Change language from parameters
  it('should change language from parameters', function() {
    service.changeLanguage({parameters:{language: 'es'}}, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSettings).toHaveBeenCalledWith({language: 'es'});
  });

  // Change language from component
  it('should change language from component', function() {
    service.changeLanguage({parameters:{target: 'component'}}, {...props, components:{component:{model:{values:[{selected: true, value:'fr'}]}}}});

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSettings).toHaveBeenCalledWith({language: 'fr'});
  });

  // Change theme from parameters
  it('should change theme from parameters', function() {
    service.changeTheme({parameters:{theme: 'gray'}}, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSettings).toHaveBeenCalledWith({theme: 'gray'});
  });

  // Change theme from component
  it('should change theme from component', function() {
    service.changeTheme({parameters:{target: 'component'}}, {...props, components:{component:{model:{values:[{selected: true, value:'red'}]}}}});

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSettings).toHaveBeenCalledWith({theme: 'red'});
  });

  // Wait default time
  it('should wait 1 millisecond', function() {
    // Install clock
    jasmine.clock().install();

    service.wait({parameters:{}}, props);

    // Wait till action has been accepted
    jasmine.clock().tick(2);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();

    // Uninstall clock
    jasmine.clock().uninstall();
  });

  // Wait defined time
  it('should wait 5 milliseconds', function() {
    // Install clock
    jasmine.clock().install();

    service.wait({parameters:{target: 5}}, props);

    // Wait till action has been accepted
    jasmine.clock().tick(6);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();

    // Uninstall clock
    jasmine.clock().uninstall();
  });

  // Add css class
  it('should add a css class', function() {
    // Install clock
    jasmine.clock().install();

    render(<div className={"test"}></div>);

    service.addClass({target: ".test", parameters:{targetAction: "tutu"}}, props);

    // Wait till action has been accepted
    jasmine.clock().tick(500);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(document.querySelector(".test").classList.contains("tutu")).toBe(true);

    // Uninstall clock
    jasmine.clock().uninstall();
  });

  // Remove css class
  it('should remove a css class', function() {
    // Install clock
    jasmine.clock().install();

    render(<div className={"test tutu"}></div>);

    service.removeClass({target: ".test", parameters:{targetAction: "tutu"}}, props);

    // Wait till action has been accepted
    jasmine.clock().tick(500);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(document.querySelector(".test").classList.contains("tutu")).toBe(false);

    // Uninstall clock
    jasmine.clock().uninstall();
  });

  // Toggle css class
  it('should toggle a css class', function() {
    // Install clock
    jasmine.clock().install();

    render(<div className={"test tutu"}></div>);

    service.toggleClass({target: ".test", parameters:{targetAction: "tutu lala"}}, props);

    // Wait till action has been accepted
    jasmine.clock().tick(500);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(document.querySelector(".test").classList.contains("lala")).toBe(true);
    expect(document.querySelector(".test").classList.contains("tutu")).toBe(false);

    // Uninstall clock
    jasmine.clock().uninstall();
  });

});
