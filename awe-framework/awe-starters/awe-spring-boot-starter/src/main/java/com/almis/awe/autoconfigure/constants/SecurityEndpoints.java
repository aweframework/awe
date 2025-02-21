package com.almis.awe.autoconfigure.constants;

import lombok.Getter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Getter
public class SecurityEndpoints {

  // File manager actions requests
  protected final RequestMatcher[] fileRequestMatchers = {
      antMatcher("/file/text"),
      antMatcher("/file/stream"),
      antMatcher("/file/download"),
      antMatcher("/file/upload"),
      antMatcher("/file/delete")};

  // Authenticated requests
  protected final RequestMatcher[] authenticatedRequestMatchers = {
      antMatcher("/action/data*/**"),
      antMatcher("/action/control*/**"),
      antMatcher("/action/update*/**"),
      antMatcher("/action/control*/**"),
      antMatcher("/action/unique*/**"),
      antMatcher("/action/value*/**"),
      antMatcher("/action/validate*/**"),
      antMatcher("/action/subscribe*/**"),
      antMatcher("/action/maintain*/**"),
      antMatcher("/action/get-file-maintain/**"),
      antMatcher("/file/stream/maintain/**"),
      antMatcher("/file/download/maintain/**")};

  // Web resources
  protected final RequestMatcher[] webResourcesRequestMatchers = {
      antMatcher("/css/**"),
      antMatcher("/js/**"),
      antMatcher("/fonts/**"),
      antMatcher("/images/**"),
      antMatcher("/locales/**"),
      antMatcher("/error**"),
      antMatcher("/websocket/**"),
      antMatcher("/template/**"),
      antMatcher("/settings"),
      antMatcher("/locals-*/**")};

  // Public actions
  protected final RequestMatcher[] publicActionsRequestMatchers = {
      antMatcher("/action/login*"),
      antMatcher("/action/logout*"),
      antMatcher("/action/get-locals"),
      antMatcher("/action/screen-data"),
      antMatcher("/action/encrypt"),
      antMatcher("/action/get-file"),
      antMatcher("/action/file-info"),
      antMatcher("/action/delete-file"),
      antMatcher("/screen/public/**"),
      antMatcher("/screen-data/**")
  };
}
