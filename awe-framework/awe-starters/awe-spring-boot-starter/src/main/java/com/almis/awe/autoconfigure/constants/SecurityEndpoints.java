package com.almis.awe.autoconfigure.constants;

import lombok.Getter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Getter
public class SecurityEndpoints {

  // File manager actions requests
  protected final RequestMatcher[] fileRequestMatchers = {
      PathPatternRequestMatcher.withDefaults().matcher("/file/text"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/stream"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/download"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/upload"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/delete")
  };

  // Authenticated requests
  protected final RequestMatcher[] authenticatedRequestMatchers = {
      PathPatternRequestMatcher.withDefaults().matcher("/action/data*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/update*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/control*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/unique*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/value*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/validate*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/subscribe*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/maintain*/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/get-file-maintain/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/stream/maintain/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/file/download/maintain/**")
  };

  // Web resources
  protected final RequestMatcher[] webResourcesRequestMatchers = {
      PathPatternRequestMatcher.withDefaults().matcher("/css/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/js/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/fonts/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/images/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/locales/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/error**"),
      PathPatternRequestMatcher.withDefaults().matcher("/websocket/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/template/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/settings"),
      PathPatternRequestMatcher.withDefaults().matcher("/locals-*/**")
  };

  // Public actions
  protected final RequestMatcher[] publicActionsRequestMatchers = {
      PathPatternRequestMatcher.withDefaults().matcher("/action/login*"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/logout*"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/get-locals"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/screen-data"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/encrypt"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/get-file"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/file-info"),
      PathPatternRequestMatcher.withDefaults().matcher("/action/delete-file"),
      PathPatternRequestMatcher.withDefaults().matcher("/screen/public/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/screen-data/**")
  };
}
