package com.almis.awe.test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Class used for configure application context for integration test with Spring
 *
 * @author pvidal
 */
@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@WebAppConfiguration
@SpringBootTest
public abstract class AbstractSpringAppIntegrationTest {

  @Autowired
  public MockMvc mockMvc;

  @Autowired
  public ObjectMapper objectMapper;

  @Autowired
  public WebApplicationContext context;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
  }
}
