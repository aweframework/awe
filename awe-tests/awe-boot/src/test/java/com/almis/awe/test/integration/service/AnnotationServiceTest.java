package com.almis.awe.test.integration.service;

import com.almis.awe.annotation.aspect.AuditAnnotation;
import com.almis.awe.annotation.aspect.DownloadAnnotation;
import com.almis.awe.annotation.entities.security.Hash;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.AnnotationTestService;
import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class used for testing rest services through ActionController
 *
 * @author pgarcia
 */
@Tag("integration")
@DisplayName("Annotations service Tests")
@Slf4j
class AnnotationServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private AnnotationTestService annotationTestService;

  @Autowired
  private BaseConfigProperties baseConfigProperties;

  @Autowired
  private EncodeService encodeService;

  @Mock
  private AweSession aweSession;

  @Test
  void checkLocaleAnnotations() {

    String valueFromInput = annotationTestService.localeFromParameters("ENUM_LAN_ES-ES");
    String valueFromReturn = annotationTestService.localeFromReturnedValue();

    assertEquals("Español", valueFromInput);
    assertEquals("Español", valueFromReturn);
    assertEquals("Español", annotationTestService.localeFromAnnotationValue("This value should be overwritten"));
  }

  @Test
  void checkHashAnnotations() throws Exception {
    //Hashing
    logger.warn("Check hash annotations");
    assertEquals(encodeService.hash(Hash.HashingAlgorithm.SHA_256.getAlgorithm(), "Moderdonio", "1234"), annotationTestService.hashParameter("Moderdonio"));
    assertEquals(encodeService.hash(Hash.HashingAlgorithm.SHA_256.getAlgorithm(), "Moderdonio", "1234"), annotationTestService.hashReturnedValue("Moderdonio"));
  }

  @Test
  void checkCryptoAnnotations() throws Exception {
    // Crypto annotation on input parameters
    logger.warn("Check crypto annotations");
    String encriptedTextUtil = encodeService.encryptAes("Moderdonio", "1234");
    logger.warn("EncodeUtil => " + encriptedTextUtil);
    String encryptedText = annotationTestService.encryptText("Moderdonio");
    logger.warn("Annotation => " + encryptedText);

    assertEquals("Moderdonio", encodeService.decryptAes(encryptedText, "1234"));
    assertEquals("Moderdonio", annotationTestService.decryptText(encriptedTextUtil));

    // Crypto annotation on return values
    logger.warn("Check crypto annotations on return values");
    assertEquals("Moderdonio", encodeService.decryptAes(annotationTestService.encryptReturnedText("Moderdonio"), "1234"));
    assertEquals("Moderdonio", annotationTestService.decryptReturnedText(encriptedTextUtil));
  }

  @Test
  void checkAuditAnnotation() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    String auditMessage = annotationTestService.testAuditParamToConsole("Test message");
    assertNotNull(auditMessage);
  }

  @Test
  void checkAuditAnnotationPrivateMethodsAsTrue() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    String auditMessage = annotationTestService.testAuditPrivateMethodToConsole("Test message of private method");
    assertNotNull(auditMessage);
  }

  @Test
  void checkAuditAnnotationPrivateMethodsAsFalse() {
    // Test message audit | Symbolic, no Audit messages should appear on the log files
    String auditMessage = annotationTestService.testAuditPrivateMethodAsFalseToConsole("Test message of private method");
    assertNull(auditMessage);
  }

  @Test
  void checkAuditAnnotationReturnValue() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    String auditMessage = annotationTestService.testAuditMethodReturnValuesToConsole("Test message of private method");
    assertNotNull(auditMessage);
  }

  @Test
  void checkAuditAnnotationReturnValueList() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    annotationTestService.testAuditMethodReturnList(Arrays.asList("Elem1", "Elem2", "Elem3"));
  }

  @Test
  void checkAuditAnnotationReturnValueMap() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    Map<String, String> dummyMap = new HashMap<String, String>() {{
      put("key1", "value1");
      put("key2", "value2");
      put("key3", "value3");
    }};

    annotationTestService.testAuditMethodReturnMap(dummyMap);
  }

  @Test
  void checkAuditGetAuditAnnotationIsNull() throws AWException, NoSuchMethodException {
    // Test audit annotation is null
    AuditAnnotation auditAnnotation = new AuditAnnotation();

    //Mocks
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(String.class);
    when(signature.getMethod()).thenReturn(getDummyMethod());

    // Assert
    assertNull(auditAnnotation.auditClassProcessor(joinPoint));
  }

  @Test
  void checkSessionAnnotation() {
    // Test variable input
    String inputValue = "This is the variable input value";
    String returnValue = "This is the return value";
    given(aweSession.getParameter(any(), anyString())).willReturn(inputValue, returnValue);
    String inputVariableText = annotationTestService.addParameterToSessionFromInputVariable(inputValue);
    assertEquals(inputVariableText, annotationTestService.getValueFromSessionOnInputVariable("This text should be overwritten"));

    String returnValueText = annotationTestService.addParameterToSessionFromReturnValue(returnValue);
    assertEquals(returnValueText, annotationTestService.getValueFromSessionOnReturnValue());

    given(aweSession.hasParameter(anyString())).willReturn(true);
    annotationTestService.addParameterToSessionFromInputVariable("test");
  }

  @Test
  void checkGoToAnnotation() {
    // Test message audit | Symbolic, some Audit messages should appear on the log files
    assertEquals("index", annotationTestService.testGoToAnnotation().getClientActionList().get(0).getTarget());

    // Test message audit | Symbolic, some Audit messages should appear on the log files
    assertEquals("index", annotationTestService.testGoToAnnotationClientAction().getTarget());

    // Test message audit | Symbolic, some Audit messages should appear on the log files
    assertEquals("default", annotationTestService.testGoToAnnotationWithoutScreen().getTarget());

    // Test message audit | Symbolic, some Audit messages should appear on the log files
    assertEquals("default", annotationTestService.testGoToAnnotationReturningString());
  }

  @Test
  void checkDownloadAnnotation() throws Exception {
    String file = Objects.requireNonNull(this.getClass().getClassLoader().getResource("application.properties")).getFile();

    FileData fileData = new FileData(new java.io.File(file).getName(), new java.io.File(file).length(), "application/octet-stream");
    fileData.setBasePath(new File(file).getParent());
    fileData.setFileName("customName");
    String fileDataString = FileUtil.fileDataToString(fileData);

    ClientAction clientAction = annotationTestService.downloadFile();
    assertEquals(fileDataString, clientAction.getParameterMap().get("filename"));

    ClientAction clientAction2 = annotationTestService.downloadFileNoParam();
    assertEquals(fileDataString, clientAction2.getParameterMap().get("filename"));

    ClientAction clientAction3 = annotationTestService.downloadFileFromVar(new File(file));
    assertEquals(fileDataString, clientAction3.getParameterMap().get("filename"));

    ClientAction clientAction4 = annotationTestService.downloadFileFromVarMixed(file);
    assertEquals(fileDataString, clientAction4.getParameterMap().get("filename"));
  }

  @Test
  void checkDownloadAnnotationNull() throws Throwable {
    // Test audit annotation is null
    DownloadAnnotation downloadAnnotation = new DownloadAnnotation(baseConfigProperties);

    //Mocks
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getMethod()).thenReturn(getDummyMethod());

    // Assert
    assertNull(downloadAnnotation.goToMethodProcessor(joinPoint));

  }

  private Method getDummyMethod() throws NoSuchMethodException {
    return getClass().getDeclaredMethod("dummyMethod");
  }

  private void dummyMethod() {
  }
}