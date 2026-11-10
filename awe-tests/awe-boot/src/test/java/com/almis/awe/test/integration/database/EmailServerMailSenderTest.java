package com.almis.awe.test.integration.database;

import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Email servers stored by the maintain (password encrypted) must reach SMTP with the plain password
 */
@Tag("integration")
@DisplayName("Email server mail sender tests")
@WithMockUser
class EmailServerMailSenderTest extends AbstractSpringAppIntegrationTest {

  private static final int SERVER_ID = 98700;
  private static final String SERVER_NAME = "EmlSrvLstDecryptTest";
  private static final String PLAIN_PASSWORD = "smtp-password-870";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private EncodeService encodeService;

  @Autowired
  private MailSenderFactory mailSenderFactory;

  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void createJdbcTemplate() {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterEach
  void deleteServer() {
    jdbcTemplate.update("DELETE FROM AweEmlSrv WHERE IdeAweEmlSrv = ?", SERVER_ID);
    mailSenderFactory.init();
  }

  @Test
  void mailSenderOfAServerStoredByTheMaintainUsesThePlainPassword() throws Exception {
    // Store the password as the EmlSrvNew maintain does (STRING_ENCRYPT)
    jdbcTemplate.update("INSERT INTO AweEmlSrv (IdeAweEmlSrv, SrvNam, Hst, Ath, EmlUsr, EmlPwd, Act) VALUES (?, ?, ?, 1, ?, ?, 1)",
      SERVER_ID, SERVER_NAME, "smtp.example.com", "sender@example.com", encodeService.encryptRipEmd160(PLAIN_PASSWORD));

    JavaMailSenderImpl mailSender = (JavaMailSenderImpl) mailSenderFactory.init().getMailSender(SERVER_NAME);

    assertThat(mailSender.getHost()).isEqualTo("smtp.example.com");
    assertThat(PLAIN_PASSWORD.equals(mailSender.getPassword())).as("password is decrypted").isTrue();
  }
}
