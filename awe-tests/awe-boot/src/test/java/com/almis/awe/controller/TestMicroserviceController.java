package com.almis.awe.controller;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


/**
 * Created by pgarcia on 18/05/2018.
 */
@Controller
public class TestMicroserviceController {

  @Autowired
  private AweRequest aweRequest;

  /**
   * Test post parameter list
   *
   * @param database Database parameter
   * @param user User parameter
   * @param date Date parameter
   * @param parameterNumber Number of parameters
   * @param request Http request
   * @return Empty service data
   */
  @PostMapping(value = "/alu-microservice/data/aluWbsCorGetKey")
  @ResponseBody
  public ServiceData testPostParameterList(@RequestParam(name = "database") String database,
                                           @RequestParam(name = "username") String user,
                                           @RequestParam(name = "currentDate") @DateTimeFormat(pattern="dd/MM/yyyy") Date date,
                                           @RequestParam(name = "numPar") Integer parameterNumber,
                                           HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData();
  }

  /**
   * Test post parameter list
   * @return Empty service data
   */
  @PostMapping(value = "/alu-microservice/invoke/{lala}")
  @ResponseBody
  public ServiceData testPostParameterListAnotherMicroservice(@PathVariable(value = "lala") String lala, @RequestBody ObjectNode jsonData, HttpServletRequest request) {
    Map<String, CellData> row = new HashMap<>();
    row.put("text", new CellData("test"));
    row.put("date", new CellData(new GregorianCalendar(1978, Calendar.OCTOBER, 23).getTime()));
    row.put("integer", new CellData(22));
    row.put("long", new CellData(22L));
    row.put("double", new CellData(22D));
    row.put("float", new CellData(22F));
    row.put("null", new CellData());

    DataList dataList = new DataList();
    dataList.addRow(row);

    // Initialize parameters
    return new ServiceData()
      .setDataList(dataList);
  }

  /**
   * Test post parameter list
   * @return Empty service data
   */
  @PostMapping(value = "/alu-microservice/invoke")
  @ResponseBody
  public ServiceData testPostMicroservice(HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData();
  }

  /**
   * Test get overwrite microservice name
   * @return Empty service data
   */
  @GetMapping(value = "/alu-service-bis/invoke")
  @ResponseBody
  public ServiceData testGetOverwriteMicroservice(HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData();
  }

  /**
   * Test get overwrite microservice name
   * @return Empty service data
   */
  @GetMapping(value = "/alu-service-bis/error")
  @ResponseBody
  public ServiceData testGetOverwriteMicroserviceError(HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData().setType(AnswerType.ERROR).setTitle("Error title").setMessage("Error message");
  }

  /**
   * Test get overwrite microservice name
   * @return Empty service data
   */
  @GetMapping(value = "/alu-service-bis/warning")
  @ResponseBody
  public ServiceData testGetOverwriteMicroserviceWarning(HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData().setType(AnswerType.WARNING).setTitle("Warning title").setMessage("Warning message");
  }

  /**
   * Test get overwrite microservice name
   * @return Empty service data
   */
  @GetMapping(value = "/alu-service-bis/info")
  @ResponseBody
  public ServiceData testGetOverwriteMicroserviceInfo(HttpServletRequest request) {
    // Initialize parameters
    return new ServiceData().setType(AnswerType.INFO).setTitle("Info title").setMessage("Info message");
  }
}
