package com.almis.awe.service.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AweUserDetailServicePasswordUpdateTest {

  private static final String USER = "jorgito";
  private static final String OLD_HASH = "4e0a5a3c1d5d3f8ff3e2c6b8d9f5a0ad1c2b3e4f";
  private static final String NEW_HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv";

  private final Logger logger = (Logger) LoggerFactory.getLogger(AweUserDetailService.class);
  private ListAppender<ILoggingEvent> appender;

  @Mock
  private UserDAO userDAO;

  private AweUserDetailService service;

  @BeforeEach
  void setUp() {
    service = new AweUserDetailService(new BaseConfigProperties(), new SecurityConfigProperties(), userDAO);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void updatePassword_storesTheNewHashAndReturnsTheUserWithIt() {
    AweUserDetails user = new AweUserDetails().setUsername(USER).setPassword(OLD_HASH);
    when(userDAO.updatePassword(USER, NEW_HASH)).thenReturn(true);

    UserDetails updated = service.updatePassword(user, NEW_HASH);

    assertThat(updated.getUsername()).isEqualTo(USER);
    assertThat(updated.getPassword()).isEqualTo(NEW_HASH);
    assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.WARN);
  }

  @Test
  void updatePassword_whenNotStored_returnsTheUserUnchangedAndWarnsWithTheUserNameOnly() {
    AweUserDetails user = new AweUserDetails().setUsername(USER).setPassword(OLD_HASH);
    when(userDAO.updatePassword(USER, NEW_HASH)).thenReturn(false);

    UserDetails updated = service.updatePassword(user, NEW_HASH);

    assertThat(updated).isSameAs(user);
    assertThat(updated.getPassword()).isEqualTo(OLD_HASH);
    assertWarnWithoutSecrets();
  }

  @Test
  void updatePassword_whenTheDaoFails_doesNotThrowAndReturnsTheUserUnchanged() {
    AweUserDetails user = new AweUserDetails().setUsername(USER).setPassword(OLD_HASH);
    when(userDAO.updatePassword(USER, NEW_HASH)).thenThrow(new IllegalStateException("value too long for " + NEW_HASH));

    UserDetails updated = service.updatePassword(user, NEW_HASH);

    assertThat(updated).isSameAs(user);
    assertThat(updated.getPassword()).isEqualTo(OLD_HASH);
    assertWarnWithoutSecrets();
  }

  @Test
  void updatePassword_withAnotherUserDetailsType_returnsTheSameUser() {
    UserDetails user = User.withUsername(USER).password(OLD_HASH).build();
    when(userDAO.updatePassword(USER, NEW_HASH)).thenReturn(true);

    assertThat(service.updatePassword(user, NEW_HASH)).isSameAs(user);
  }

  private void assertWarnWithoutSecrets() {
    assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN).singleElement().satisfies(event -> {
      assertThat(event.getFormattedMessage()).contains(USER).doesNotContain(NEW_HASH).doesNotContain(OLD_HASH);
      assertThat(event.getThrowableProxy()).isNull();
    });
  }
}
