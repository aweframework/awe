import {
  extractCellModel,
  extractCellValue,
  fetchHtml,
  fetchJson,
  getCellValue,
  getContextPath,
  getHelpTooltipNode,
  getSizeString
} from '../../../src/utilities';
import {waitFor} from "@testing-library/react";
import {Tooltip} from "primereact/tooltip";
import React from "react";

describe('awe-react-client/test/js/utilities/utilsTest.jsx', () => {
  it('should extract a cell value from array', () => {
    expect(extractCellValue([{value: 1, label: "1", selected: false}, {value: 2, label: "2", selected: true}])).toBe("2");
  });

  it('should extract a cell value from object', () => {
    expect(extractCellValue({value: 1, label: "1", selected: false})).toBe(1);
  });

  it('should extract a cell value from data', () => {
    expect(extractCellValue("pepe")).toBe("pepe");
  });

  it('should extract a cell model from array', () => {
    expect(extractCellModel([{value: 1, label: "1", selected: false}, {value: 2, label: "2", selected: true}])).toEqual({value: 2, label: "2", selected: true});
  });

  it('should extract a cell model from object', () => {
    expect(extractCellModel({value: 1, label: "1", selected: false})).toEqual({value: 1, label: "1", selected: false});
  });

  it('should extract a cell model from data', () => {
    expect(extractCellModel("luis")).toEqual({value: "luis"});
  });

  it('should get a cell value from a grid', () => {
    expect(getCellValue([{"col1": 1}, {"col1": 2}, {"col1": 3}], 1, "col1")).toBe(2);
  });

  it('should get context path', async () => {
    //console.info('Context path test');
    await waitFor(() => expect( getContextPath()).toEqual(""));
  });

  it('should fetch a json and return json', async () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: () => ({})
    }));

    expect(await fetchJson('POST', '/test', "{}", "token")).toEqual({});
  });

  it('should fetch an html file and return text', async () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      text: () => Promise.resolve("<texto en html=''>")
    }));

    expect(await fetchHtml('/template/help', 'token')).toEqual("<texto en html=''>");
  });

  it('should transform a size into a string', () => {
    expect(getSizeString(12)).toBe("12 B");
    expect(getSizeString(123223)).toBe("120.3 KB");
    expect(getSizeString(123223327)).toBe("117.5 MB");
    expect(getSizeString(60897938234)).toBe("56.7 GB");
    expect(getSizeString(9809234902935523)).toBe("8.7 PB");
  });

  it('should generate a help tooltip node without image', () => {
    expect(getHelpTooltipNode("help", null, t => t, "test")).toEqual(<Tooltip target="test">
      {["help", null]}
    </Tooltip>)
  });

  it('should generate a help tooltip node with image', () => {
    expect(getHelpTooltipNode("help", null, t => t, "test")).toEqual(<Tooltip target="test">
      {["help", null]}
    </Tooltip>)
  });

  it('should generate a help tooltip node only with image', () => {
    expect(getHelpTooltipNode("help", "test", t => t, "test")).toEqual(<Tooltip target="test">
      {["help", <img src="test" alt="help"/>]}
    </Tooltip>)
  });

  it('should generate a help tooltip node empty', () => {
    expect(getHelpTooltipNode(null, null, t => t, "test")).toEqual(null)
  });
});
