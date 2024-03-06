package com.almis.awe.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * User data implementation
 */
@Slf4j
public class UserDAOImpl extends ServiceConfig implements UserDAO {

  // Query service
  private final QueryUtil queryUtil;
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public UserDAOImpl(QueryUtil queryUtil, QueryService queryService) {
    this.queryUtil = queryUtil;
    this.queryService = queryService;
  }

  @Override
  public User findByUserName(String userName) {
    try {
      // Get user details from database
      return DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_QUERY, queryUtil.getParameters()
          .put("user", userName)).getDataList(), User.class)
        .stream()
        .findFirst()
        .orElseThrow(() -> new UsernameNotFoundException(userName));
    } catch (Exception exc) {
      log.error("Error retrieving user details", exc);
      throw new UsernameNotFoundException(userName, exc);
    }
  }
}
