package com.almis.awe.security.authentication.filter;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/data",
    "/action/update-value",
    "/action/data-silent",
    "/action/unique",
    "/action/control",
    "/action/control-cancel",
    "/action/validate",
    "/action/subscribe",
    "/action/tree-branch",
    "/api/data"
  })
  void givenNullAweElement_checkIsPublicQuery_shouldReturnTrue(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query().setIsPublic(true));
    assertTrue(publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/data",
    "/action/update-value",
    "/action/data-silent",
    "/action/unique",
    "/action/control",
    "/action/control-cancel",
    "/action/validate",
    "/action/subscribe",
    "/action/tree-branch",
    "/api/data"
  })
  void givenNullAweElement_checkIsPublicQuery_shouldReturnFalse(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getQuery(anyString())).thenReturn(new Query());
    assertFalse(publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/data",
    "/action/update-value",
    "/action/data-silent",
    "/action/unique",
    "/action/control",
    "/action/control-cancel",
    "/action/validate",
    "/action/subscribe",
    "/action/tree-branch",
    "/api/data"
  })
  void givenNullAweElement_checkIsPublicQuery_shouldReturnNull(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getQuery(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainAuthorization.isPublicQuery(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/maintain",
    "/action/maintain-silent",
    "/action/get-file-maintain",
    "/file/stream/maintain",
    "/file/download/maintain",
    "/api/maintain",
    "/api/public/maintain"
  })
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnTrue(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target().setIsPublic(true));
    assertTrue(publicQueryMaintainAuthorization.isPublicMaintain(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/maintain",
    "/action/maintain-silent",
    "/action/get-file-maintain",
    "/file/stream/maintain",
    "/file/download/maintain",
    "/api/maintain",
    "/api/public/maintain"
  })
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnFalse(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getMaintain(anyString())).thenReturn(new Target());
    assertFalse(publicQueryMaintainAuthorization.isPublicMaintain(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/action/maintain",
    "/action/maintain-silent",
    "/action/get-file-maintain",
    "/file/stream/maintain",
    "/file/download/maintain",
    "/api/maintain",
    "/api/public/maintain"
  })
  void givenNullAweElement_checkIsPublicMaintain_shouldReturnNull(String action) throws AWException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(action + "/foo");
    when(elements.getMaintain(anyString())).thenReturn(null);
    assertThrows(AuthorizationServiceException.class, () -> publicQueryMaintainAuthorization.isPublicMaintain(request));
  }
}