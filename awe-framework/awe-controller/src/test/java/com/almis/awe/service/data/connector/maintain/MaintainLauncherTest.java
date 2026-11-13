package com.almis.awe.service.data.connector.maintain;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.service.screen.ScreenRestrictionRequestCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintainLauncherTest {

  @InjectMocks
  private MaintainLauncher maintainLauncher;

  @Mock
  private ApplicationContext context;

  @Mock
  private SQLMaintainConnector sqlMaintainConnector;

  @BeforeEach
  void setUp() {
    maintainLauncher.setApplicationContext(context);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void aMaintainMakesTheRequestReadTheScreenRestrictionsAgain() throws AWException {
    when(context.getBean(SQLMaintainConnector.class)).thenReturn(sqlMaintainConnector);
    when(sqlMaintainConnector.launch(any(), any(), any())).thenReturn(new ServiceData());
    AtomicInteger loads = new AtomicInteger();
    ScreenRestrictionRequestCache.Loader loader = () -> {
      loads.incrementAndGet();
      return List.of(new ScreenRestriction().setOption("option").setRestricted(true));
    };

    ScreenRestrictionRequestCache.get("test", loader);
    ScreenRestrictionRequestCache.get("test", loader);
    maintainLauncher.launchMaintain(new Insert(), null, null);
    ScreenRestrictionRequestCache.get("test", loader);

    assertEquals(2, loads.get());
  }
}
