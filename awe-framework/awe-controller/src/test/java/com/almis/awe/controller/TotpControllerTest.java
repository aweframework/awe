package com.almis.awe.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.TotpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpControllerTest {

  @Mock
  private TotpService totpService;

  @InjectMocks
  private TotpController totpController;

  @Test
  void getQRCodeReturnsPngBodyAndHeaders() throws AWException {
    byte[] png = new byte[]{1, 2, 3};
    when(totpService.getQRCode()).thenReturn(png);

    ResponseEntity<byte[]> response = totpController.getQRCode();

    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
    assertEquals("no-cache", response.getHeaders().getPragma());
    assertEquals("0", response.getHeaders().getFirst("Expires"));
    assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
    assertTrue(response.getHeaders().getCacheControl().contains("private"));
    assertArrayEquals(png, response.getBody());
  }

  @Test
  void getQRCode_propagatesAweException_whenServiceDeniesAccess() throws AWException {
    // When TotpService enforces the access guard and throws, the controller must propagate.
    when(totpService.getQRCode()).thenThrow(new AWException("denied", "forbidden"));

    assertThrows(AWException.class, () -> totpController.getQRCode(),
      "AWException from the service guard must propagate through the controller");
  }
}
