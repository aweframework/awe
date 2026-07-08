package com.almis.awe.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.service.FileService;
import com.almis.awe.service.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarControllerTest {

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private UserSettingsService userSettingsService;

  @Mock
  private FileService fileService;

  @InjectMocks
  private AvatarController avatarController;

  @BeforeEach
  void setUp() {
    avatarController.setApplicationContext(applicationContext);
  }

  @Test
  void getAvatarReturnsImageBytesWhenAvatarExists() throws AWException {
    FileData fileData = new FileData("avatar.png", 1024L, "image/png");
    when(userSettingsService.getAvatarForCurrentUser()).thenReturn(Optional.of(fileData));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    when(fileService.getFileStream(fileData)).thenReturn(new ResponseEntity<>(new FileSystemResource("avatar.png"), headers, HttpStatusCode.valueOf(200)));

    ResponseEntity<FileSystemResource> response = avatarController.getAvatar();

    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
    assertEquals("no-cache", response.getHeaders().getCacheControl());
  }

  @Test
  void getAvatarReturns404WhenNoAvatarStored() throws AWException {
    when(userSettingsService.getAvatarForCurrentUser()).thenReturn(Optional.empty());

    ResponseEntity<FileSystemResource> response = avatarController.getAvatar();

    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }

  @Test
  void getAvatarReturns404WhenTokenDecodeFails() throws AWException {
    when(userSettingsService.getAvatarForCurrentUser()).thenThrow(new AWException("Error decoding file from string", "bad token"));

    ResponseEntity<FileSystemResource> response = avatarController.getAvatar();

    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }
}
