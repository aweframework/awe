package com.almis.awe.scheduler.dao;

import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.NamingException;
import java.util.Date;

import static com.almis.awe.scheduler.constant.MaintainConstants.FILE_INSERT_MODIFICATION_QUERY;
import static com.almis.awe.scheduler.constant.MaintainConstants.FILE_UPDATE_MODIFICATION_QUERY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing File DAO class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class FileDAOTest {

  @InjectMocks
  private FileDAO fileDAO;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryUtil queryUtil;

  /**
   * Test add modification (insert)
   *
   * @throws NamingException Test error
   */
  @Test
  void addModificationInsert() throws Exception {
    // Mock
    given(queryUtil.getParameters((String) null)).willReturn(JsonNodeFactory.instance.objectNode());
    Task task = new Task().setFile(new File());

    // Call method
    fileDAO.addModification(task, "", new Date(), false);

    // Assert called
    verify(maintainService, times(1)).launchPrivateMaintain(eq(FILE_INSERT_MODIFICATION_QUERY), any(ObjectNode.class));
  }

  /**
   * Test add modification (update)
   *
   * @throws NamingException Test error
   */
  @Test
  void addModificationUpdate() throws Exception {
    // Mock
    given(queryUtil.getParameters((String) null)).willReturn(JsonNodeFactory.instance.objectNode());
    Task task = new Task().setFile(new File());

    // Call method
    fileDAO.addModification(task, "", new Date(), true);

    // Assert called
    verify(maintainService, times(1)).launchPrivateMaintain(eq(FILE_UPDATE_MODIFICATION_QUERY), any(ObjectNode.class));
  }
}
