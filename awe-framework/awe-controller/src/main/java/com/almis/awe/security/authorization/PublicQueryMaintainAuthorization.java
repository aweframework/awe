package com.almis.awe.security.authorization;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Filter class to authorize request if query or maintain are public
 */
@Slf4j
public class PublicQueryMaintainAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

  private final AweElements elements;
  private static final List<String> QUERY_PUBLIC_LIST = Arrays.asList("/action/data",
    "/action/update",
    "/action/control",
    "/action/unique",
    "/action/value",
    "/action/validate",
    "/action/subscribe",
    "/action/tree-branch",
    "/api/data",
    "/api/public/data");

  private static final List<String> MAINTAIN_PUBLIC_LIST = Arrays.asList("/action/maintain",
    "/action/get-file-maintain",
    "/file/stream/maintain",
    "/file/download/maintain",
    "/api/maintain",
    "/api/public/maintain");

  /**
   * Autowired constructor
   *
   * @param aweElements AweElements
   */
  public PublicQueryMaintainAuthorization(AweElements aweElements) {
    this.elements = aweElements;
  }

  @Override
  public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext authorizationContext) {
    HttpServletRequest request = authorizationContext.getRequest();
    if (isQueryAction(request)) {
      return new AuthorizationDecision( isAuthenticated(authentication.get()) || isPublicQuery(request));
    } else if (isMaintainAction(request)) {
      return new AuthorizationDecision(isAuthenticated(authentication.get()) || isPublicMaintain(request));
    } else return new AuthorizationDecision(false);
  }

  /**
   * Check if query request is public
   *
   * @param request Request
   * @return query is public
   */
  public boolean isPublicQuery(HttpServletRequest request){
    // Check if target is a public query
    String target = getTarget(request);
    try {
      return Optional.ofNullable(elements.getQuery(target))
              .orElseThrow(() -> new AuthorizationServiceException("Query not found: " + target)).isPublic();
    } catch (AWException ex) {
      log.warn("Query not found: " + target);
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
      return Optional.ofNullable(elements.getMaintain(target))
              .orElseThrow(() -> new AuthorizationServiceException("Maintain not found: " + target)).isPublic();
    } catch (AWException ex) {
      log.warn("Maintain not found: " + target);
      return false;
    }
  }

  /**
   * Check if request is a QUERY action
   * @param request Http request
   * @return if is request is a query
   */
  private boolean isQueryAction(HttpServletRequest request) {
    final String requestString = request.getRequestURI();
    return QUERY_PUBLIC_LIST.stream().anyMatch(requestString::contains);
  }

  /**
   * Check if request is a MAINTAIN action
   * @param request Http request
   * @return if is request is a maintain
   */
  private boolean isMaintainAction(HttpServletRequest request) {
    final String requestString = request.getRequestURI();
    return MAINTAIN_PUBLIC_LIST.stream().anyMatch(requestString::contains);
  }

  /**
   * Extract target parameter from request
   * @param request Http request
   * @return target
   */
  private String getTarget(HttpServletRequest request) {
    // Check if target is a public query or maintain
    return Arrays.stream(request.getRequestURI().split("/")).reduce((first, second) -> second).orElse(null);
  }

  /**
   * Check if user is not anonymous and is authenticated
   * @param authentication Authentication type
   * @return if user is authenticated
   */
  private boolean isAuthenticated(Authentication authentication) {
    return (!(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated());
  }
}
