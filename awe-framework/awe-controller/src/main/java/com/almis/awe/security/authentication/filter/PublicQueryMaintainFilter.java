package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import lombok.extern.log4j.Log4j2;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filter class to authorize request if query or maintain are public
 */
@Log4j2
public class PublicQueryMaintainFilter {

  private final AweElements elements;

  /**
   * Autowired constructor
   *
   * @param aweElements AweElements
   */
  public PublicQueryMaintainFilter(AweElements aweElements) {
    this.elements = aweElements;
  }

  /**
   * Check if query request is public
   *
   * @param request Request
   * @return query is public
   */
  public boolean isPublicQuery(HttpServletRequest request) {
    String target = getTarget(request);
    try {
      return Optional.ofNullable(elements.getQuery(target)).orElse(new Query()).isPublic();
    } catch (AWException ex) {
      log.error("Error filtering query", ex);
      return false;
    }
  }

  /**
   * Check if maintain request is public
   *
   * @param request Request
   * @return maintain is public
   */
  public boolean isPublicMaintain(HttpServletRequest request) {
    // Check if target is a public maintain
    String target = getTarget(request);
    try {
      return Optional.ofNullable(elements.getMaintain(target)).orElse(new Target()).isPublic();
    } catch (AWException ex) {
      log.error("Error filtering maintain", ex);
      return false;
    }
  }

  private String getTarget(HttpServletRequest request) {
    // Check if target is a public query or maintain
    return Arrays.stream(request.getRequestURI().split("/")).reduce((first, second) -> second).orElse(null);
  }
}
