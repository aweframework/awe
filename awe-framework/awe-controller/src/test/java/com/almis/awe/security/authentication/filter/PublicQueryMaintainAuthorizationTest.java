package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
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
class PublicQueryMaintainAuthorizationTest {

  @InjectMocks
  PublicQueryMaintainAuthorization publicQueryMaintainAuthorization;
  @Mock
  AweElements elements;

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnTrue() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    assertTrue(publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query());
    assertFalse(publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicQuery_shouldReturnNull() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/data/foo");
    when(elements.getQuery(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnTrue() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target().setIsPublic(true));
    assertTrue(publicQueryMaintainAuthorization.isPublicMaintain(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnFalse() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target());
    assertFalse(publicQueryMaintainAuthorization.isPublicMaintain(request));
  }

  @Test
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnNull() throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/action/maintain/foo");
    when(elements.getMaintain(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainAuthorization.isPublicMaintain(request));
  }
}