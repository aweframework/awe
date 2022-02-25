package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AuthorizationServiceException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filter class to authorize request if query or maintain are public
 */
@Slf4j
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
   * @throws AWException Error retrieving query
   */
  public boolean isPublicQuery(HttpServletRequest request) throws AWException {
    String target = getTarget(request);
    return Optional.ofNullable(elements.getQuery(target))
      .orElseThrow(() -> new AuthorizationServiceException("Query not found: " + target)).isPublic();
  }

  /**
   * Check if maintain request is public
   *
   * @param request Request
   * @return maintain is public
   * @throws AWException Error retrieving maintain
   */
  public boolean isPublicMaintain(HttpServletRequest request) throws AWException {
    // Check if target is a public maintain
    String target = getTarget(request);
    return Optional.ofNullable(elements.getMaintain(target))
      .orElseThrow(() -> new AuthorizationServiceException("Maintain target not found: " + target)).isPublic();
  }

  private String getTarget(HttpServletRequest request) {
    // Check if target is a public query or maintain
    return Arrays.stream(request.getRequestURI().split("/")).reduce((first, second) -> second).orElse(null);
  }
}
