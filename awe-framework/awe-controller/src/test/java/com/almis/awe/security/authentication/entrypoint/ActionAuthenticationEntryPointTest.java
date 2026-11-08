package com.almis.awe.security.authentication.entrypoint;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ActionAuthenticationEntryPointTest {

	private static final String EXPIRED_TITLE = "Session expired";
	private static final String EXPIRED_MESSAGE = "Your connection has expired. You must login again.";

	private AweSessionDetails sessionDetails;
	private AweElements elements;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private ActionAuthenticationEntryPoint entryPoint;
	private StringWriter body;

	@BeforeEach
	void setUp() throws Exception {
		sessionDetails = mock(AweSessionDetails.class);
		elements = mock(AweElements.class);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		entryPoint = new ActionAuthenticationEntryPoint(sessionDetails, elements, new JsonMapper());
		body = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(body));
		when(request.getRequestURI()).thenReturn("/action/maintain/Usr");
		when(elements.getLocaleWithLanguage(eq("ERROR_TITLE_SESSION_EXPIRED"), any())).thenReturn(EXPIRED_TITLE);
		when(elements.getLocaleWithLanguage(eq("ERROR_MESSAGE_SESSION_EXPIRED"), any())).thenReturn(EXPIRED_MESSAGE);
	}

	@Test
	void answersAnExpiredSessionWithTheClientActionsOfAnError() throws Exception {
		HttpSession session = mock(HttpSession.class);
		when(request.getSession(false)).thenReturn(session);
		when(session.getId()).thenReturn("session-id");

		entryPoint.commence(request, response, mock(AuthenticationException.class));

		JsonNode answer = new JsonMapper().readTree(body.toString());
		assertThat(answer.isArray()).isTrue();
		assertThat(answer).extracting(node -> node.get("type").asString())
			.containsExactly("end-load", "message", "cancel");
		assertThat(answer.get(1).get("parameters").get("title").asString()).isEqualTo(EXPIRED_TITLE);
		assertThat(answer.get(1).get("parameters").get("message").asString()).isEqualTo(EXPIRED_MESSAGE);
	}

	@Test
	void answersUnauthorizedWithJsonActionsAndClosesTheSession() throws Exception {
		when(request.getSession(false)).thenReturn(null);

		entryPoint.commence(request, response, mock(AuthenticationException.class));

		verify(sessionDetails).onLogoutSuccess();
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(response).setContentType("application/json");
		verify(response).setCharacterEncoding("UTF-8");
		assertThat(body.toString()).startsWith("[");
	}

	@Test
	void translatesTheMessageWithTheLanguageOfTheRequest() throws Exception {
		when(request.getSession(false)).thenReturn(null);
		when(request.getLocale()).thenReturn(Locale.forLanguageTag("es-ES"));

		entryPoint.commence(request, response, mock(AuthenticationException.class));

		verify(elements).getLocaleWithLanguage("ERROR_TITLE_SESSION_EXPIRED", "es-ES");
		verify(elements).getLocaleWithLanguage("ERROR_MESSAGE_SESSION_EXPIRED", "es-ES");
	}

	@Test
	void answersTheLocaleKeyWhenNoTranslationIsAvailable() throws Exception {
		when(request.getSession(false)).thenReturn(null);
		when(request.getLocale()).thenReturn(Locale.forLanguageTag("en-GB"));
		when(elements.getLocaleWithLanguage(anyString(), any())).thenThrow(new IllegalStateException("no locales"));

		entryPoint.commence(request, response, mock(AuthenticationException.class));

		assertThat(body.toString()).contains("ERROR_TITLE_SESSION_EXPIRED", "ERROR_MESSAGE_SESSION_EXPIRED");
	}
}
