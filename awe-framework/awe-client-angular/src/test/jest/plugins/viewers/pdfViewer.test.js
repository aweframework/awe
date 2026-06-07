jest.mock("pdfobject", () => ({
  embed: jest.fn()
}));

import PDFObject from "pdfobject";
import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";
import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";

describe("awePdfViewer", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let constructedComponent;
  let serverData;
  let connection;
  let windowMock;

  beforeEach(() => {
    serverData = {getFileUrl: jest.fn(path => `/file/${path}`), getFormValues: jest.fn(() => ({CrtPdf: "report"}))};
    connection = {getFile: jest.fn(() => Promise.resolve({data: "pdf-data"}))};
    windowMock = {URL: {createObjectURL: jest.fn(() => "blob:pdf")}};
    function ComponentMock(scope, id) {
      constructedComponent = {asComponent: jest.fn(() => true), controller: {targetAction: "printReport"}, id, scope};
      scope.controller = constructedComponent.controller;
      return constructedComponent;
    }

    angular.mock.module("aweApplication", {Component: ComponentMock, Connection: connection, ServerData: serverData, $window: windowMock});
    inject(["$rootScope", "$compile", "$httpBackend", (_$rootScope_, _$compile_, _$httpBackend_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  afterEach(() => {
    try {
      $httpBackend && $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend && $httpBackend.verifyNoOutstandingRequest();
    } catch (error) {
      // These focused specs do not flush settings; keep cleanup best-effort.
    }
  });

  it("renders the pdf viewer wrapper and streams reload data into PDFObject", async () => {
    const element = $compile("<awe-pdf-viewer pdf-viewer-id='viewerId'></awe-pdf-viewer>")($rootScope);
    $rootScope.$digest();

    await constructedComponent.reload();

    expect(element.hasClass("pdf-viewer")).toBe(true);
    expect(connection.getFile).toHaveBeenCalledWith("/file/stream/maintain/printReport", expect.objectContaining({CrtPdf: "report", r: expect.any(Number)}), "application/pdf", "blob");
    expect(windowMock.URL.createObjectURL).toHaveBeenCalledWith(expect.any(Blob));
    expect(PDFObject.embed).toHaveBeenCalledWith("blob:pdf", ".pdf-viewer");
  });

  it("builds the reload URL from the controller target action", async () => {
    $compile("<awe-pdf-viewer pdf-viewer-id='viewerId'></awe-pdf-viewer>")($rootScope);
    $rootScope.$digest();

    await constructedComponent.reload();

    expect(serverData.getFileUrl).toHaveBeenCalledWith("stream/maintain/printReport");
    expect(connection.getFile.mock.calls[0][0]).toBe("/file/stream/maintain/printReport");
  });

  it("adds a cache-busting random value to reload form values", async () => {
    jest.spyOn(Math, "random").mockReturnValue(0.6789);
    $compile("<awe-pdf-viewer pdf-viewer-id='viewerId'></awe-pdf-viewer>")($rootScope);
    $rootScope.$digest();

    await constructedComponent.reload();

    expect(connection.getFile.mock.calls[0][1]).toEqual({CrtPdf: "report", r: 0.6789});
  });
});
