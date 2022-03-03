package com.almis.awe.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * User data implementation
 */
@Slf4j
public class UserDAOImpl extends ServiceConfig implements UserDAO {

  // Query service
  private final QueryService queryService;
  private final DataListService dataListService;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public UserDAOImpl(QueryService queryService, DataListService dataListService) {
    this.queryService = queryService;
    this.dataListService = dataListService;
  }

  @Override
  public User findByUserName(String userName) {
    try {
      // Get user details from database
      getRequest().setParameter("user", userName);
      ServiceData userData = queryService.launchPrivateQuery(AweConstants.USER_DETAIL_QUERY);
      return dataListService.asBeanList(userData.getDataList(), User.class).stream().findFirst()
        .orElseThrow(() -> new UsernameNotFoundException(userName));
    } catch (Exception exc) {
      log.error("Error retrieving user details", exc);
      throw new UsernameNotFoundException(userName, exc);
    }
  }
}
