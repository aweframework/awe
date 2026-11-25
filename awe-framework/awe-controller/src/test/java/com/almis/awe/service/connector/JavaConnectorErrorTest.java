package com.almis.awe.service.connector;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.services.ServiceJava;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The answer of a Java service that fails: an unexpected exception reaches the user as a generic message, never its
 * own text, and is logged with its stack trace; an AWException keeps the message written for the user
 */
class JavaConnectorErrorTest {

  private static final String GENERIC_MESSAGE = "Unexpected error, contact your administrator";
  private static final String INTERNAL_DETAIL = "Cannot invoke \"ObjectNode.get(String)\" because \"address\" is null";

  private final JavaConnector connector = new JavaConnector(new JsonMapper());
  private final Logger logger = (Logger) LoggerFactory.getLogger(JavaConnector.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void setUp() {
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(context.getBean(FailingService.class)).thenReturn(new FailingService());
    when(elements.getLanguage()).thenReturn("en");
    when(elements.getLocaleWithLanguage(any(), any())).thenAnswer(call -> call.getArgument(0));
    when(elements.getLocaleWithLanguage("ERROR_MESSAGE_UNEXPECTED_ERROR", "en")).thenReturn(GENERIC_MESSAGE);
    connector.setApplicationContext(context);
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void anUnexpectedExceptionShowsAGenericMessageAndIsLoggedWithItsStackTrace() {
    assertThatThrownBy(() -> connector.launch(service("unexpected"), new HashMap<>()))
      .isInstanceOfSatisfying(AWException.class, error -> {
        assertThat(error.getTitle()).isEqualTo("ERROR_TITLE_UNEXPECTED_ERROR");
        assertThat(error.getMessage()).isEqualTo(GENERIC_MESSAGE).doesNotContain("address");
      });

    List<ILoggingEvent> errors = appender.list.stream().filter(event -> Level.ERROR.equals(event.getLevel())).toList();
    assertThat(errors).singleElement().satisfies(event -> {
      assertThat(event.getFormattedMessage()).contains("unexpected", FailingService.class.getName());
      assertThat(event.getThrowableProxy().getClassName()).isEqualTo(NullPointerException.class.getName());
      assertThat(event.getThrowableProxy().getMessage()).isEqualTo(INTERNAL_DETAIL);
      assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
    });
  }

  @Test
  void anAweExceptionKeepsTheMessageWrittenForTheUser() {
    assertThatThrownBy(() -> connector.launch(service("expected"), new HashMap<>()))
      .isInstanceOfSatisfying(AWException.class, error -> {
        assertThat(error.getTitle()).isEqualTo("Invalid filter");
        assertThat(error.getMessage()).isEqualTo("The origin api is not valid");
      });
  }

  private static ServiceJava service(String method) {
    ServiceJava service = new ServiceJava();
    service.setClassName(FailingService.class.getName());
    service.setMethod(method);
    service.setParameterList(List.of());
    return service;
  }

  /**
   * Java service whose methods fail
   */
  public static class FailingService {
    public ServiceData unexpected() {
      throw new NullPointerException(INTERNAL_DETAIL);
    }

    public ServiceData expected() throws AWException {
      throw new AWException("Invalid filter", "The origin api is not valid");
    }
  }
}
