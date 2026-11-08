package com.almis.awe.security.authentication.entrypoint;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Action entry point.
 * Handle exceptions in actions request (/action/**).
 * <p>
 * An expired session is answered with the same client actions a refused or failed launch answers: the clients end the
 * loading state and open the session expired panel from the list of actions, without reading a text body.
 */
@Slf4j
public class ActionAuthenticationEntryPoint implements AuthenticationEntryPoint {

  // Locale keys of the message of an expired session
  private static final String SESSION_EXPIRED_TITLE = "ERROR_TITLE_SESSION_EXPIRED";
  private static final String SESSION_EXPIRED_MESSAGE = "ERROR_MESSAGE_SESSION_EXPIRED";

  // Autowired services
  private final AweSessionDetails sessionDetails;
  private final AweElements elements;
  private final ObjectMapper objectMapper;

  /**
   * ActionAuthenticationEntryPoint constructor
   *
   * @param sessionDetails Access service
   * @param elements       Application elements (translations)
   * @param objectMapper   Object mapper to write the client actions
   */
  public ActionAuthenticationEntryPoint(AweSessionDetails sessionDetails, AweElements elements,
                                        ObjectMapper objectMapper) {
    this.sessionDetails = sessionDetails;
    this.elements = elements;
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException exception) throws IOException {

    var session = httpServletRequest.getSession(false);
    String sessionId = (session != null) ? session.getId() : "no-session";

    log.warn("Unauthorized /action request due to expired or missing session. sessionId=[{}], uri=[{}]", sessionId, httpServletRequest.getRequestURI());

    // Clear websocket connections
    sessionDetails.onLogoutSuccess();

    // Send 401 Error with the client actions of an expired session
    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
    httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
    httpServletResponse.getWriter().write(objectMapper.writeValueAsString(sessionExpiredActions(httpServletRequest)));
    httpServletResponse.getWriter().flush();
  }

  /**
   * Client actions of an expired session: the answer of a client launch was over, and the message tells the client
   * that the session is gone
   *
   * @param request Request answered (its language chooses the translation)
   * @return Client action list
   */
  private List<ClientAction> sessionExpiredActions(HttpServletRequest request) {
    String language = getRequestLanguage(request);
    return List.of(
      new ClientAction("end-load"),
      new ClientAction("message")
        .addParameter("type", "error")
        .addParameter("title", getLocale(SESSION_EXPIRED_TITLE, language))
        .addParameter("message", getLocale(SESSION_EXPIRED_MESSAGE, language)),
      new ClientAction("cancel"));
  }

  /**
   * Language of the request, named as the locales of the application name it (<code>es-ES</code>). The language of
   * the session is unknown here (it has expired), so the browser language answers; a language the application does
   * not translate falls back to its default language
   *
   * @param request Request answered
   * @return Language of the request, or null when it declares none
   */
  private String getRequestLanguage(HttpServletRequest request) {
    Locale locale = request.getLocale();
    if (locale == null || locale.getLanguage() == null || locale.getLanguage().isEmpty()) {
      return null;
    }
    return (locale.getCountry() == null || locale.getCountry().isEmpty())
      ? locale.getLanguage() : locale.getLanguage() + "-" + locale.getCountry();
  }

  /**
   * Translate a locale key, answering the key itself when no translation is available
   *
   * @param key      Locale key
   * @param language Language of the answer
   * @return Translated text
   */
  private String getLocale(String key, String language) {
    try {
      return elements.getLocaleWithLanguage(key, language);
    } catch (RuntimeException exc) {
      log.debug("No translation available for [{}], answering its key", key, exc);
      return key;
    }
  }
}
