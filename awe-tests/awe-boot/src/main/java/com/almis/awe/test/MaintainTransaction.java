package com.almis.awe.test;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.MaintainService;
import org.springframework.stereotype.Service;

/**
 * Java services called from the maintain transaction tests
 */
@Service
public class MaintainTransaction {

  private final MaintainService maintainService;

  /**
   * Autowired constructor
   *
   * @param maintainService Maintain service
   */
  public MaintainTransaction(MaintainService maintainService) {
    this.maintainService = maintainService;
  }

  /**
   * Launches a nested maintain from a Java service
   *
   * @return Nested maintain result
   * @throws AWException Error launching the nested maintain
   */
  public ServiceData launchNestedMaintain() throws AWException {
    return maintainService.launchPrivateMaintain("TxNestedInsert");
  }

  /**
   * Returns an ERROR result without throwing an exception
   *
   * @return Service data of type ERROR
   */
  public ServiceData returnError() {
    return new ServiceData()
      .setType(AnswerType.ERROR)
      .setTitle("Transaction test error")
      .setMessage("The service returned an error");
  }
}
