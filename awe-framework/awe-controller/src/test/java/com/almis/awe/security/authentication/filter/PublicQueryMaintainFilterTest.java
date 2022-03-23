package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AuthorizationServiceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicQueryMaintainFilterTest {

  @InjectMocks
  PublicQueryMaintainFilter publicQueryMaintainFilter;
  @Mock
  AweElements elements;

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnTrue() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    assertTrue(publicQueryMaintainFilter.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query());
    assertFalse(publicQueryMaintainFilter.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnNull() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainFilter.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldThrowAWException() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenThrow(AWException.class);
    assertThrows(AWException.class, () -> publicQueryMaintainFilter.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnTrue() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target().setIsPublic(true));
    assertTrue(publicQueryMaintainFilter.isPublicMaintain(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target());
    assertFalse(publicQueryMaintainFilter.isPublicMaintain(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnNull() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainFilter.isPublicMaintain(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldThrowAWException() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenThrow(AWException.class);
    assertThrows(AWException.class, () -> publicQueryMaintainFilter.isPublicMaintain(request));
  }
}