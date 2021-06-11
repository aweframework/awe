package com.almis.awe.service;

import com.almis.awe.model.component.AweElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Menu service tests
 *
 * @author pgarcia
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

  @InjectMocks
  private MenuService menuService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweElements aweElements;

  @BeforeEach
  public void setUp() {
    menuService.setApplicationContext(context);
  }

  @Test
  void getMenu() {
    assertNull(null);
  }
}