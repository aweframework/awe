package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicQueryMaintainFilterTest {

  @InjectMocks
  PublicQueryMaintainFilter publicQueryMaintainFilter;
  @Mock
  AweElements elements;

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenThrow(AWException.class);
    assertFalse(publicQueryMaintainFilter.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenThrow(AWException.class);
    assertFalse(publicQueryMaintainFilter.isPublicMaintain(request));
  }
}