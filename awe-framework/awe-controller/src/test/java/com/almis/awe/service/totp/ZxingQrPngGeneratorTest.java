package com.almis.awe.service.totp;

import com.almis.awe.exception.AWERuntimeException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZxingQrPngGeneratorTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "otpauth://totp/AWE:user?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&algorithm=SHA1&digits=6&period=30",
    "otpauth://totp/john.doe%2Bqa%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Almis%20Web%20Engine&algorithm=SHA1&digits=6&period=30"
  })
  void generateEncodesQrPayload(String otpAuthUri) throws Exception {
    ZxingQrPngGenerator generator = new ZxingQrPngGenerator();

    byte[] png = generator.generate(URI.create(otpAuthUri));
    BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

    assertThat(png).isNotEmpty();
    assertThat(image).isNotNull();
    assertThat(image.getWidth()).isGreaterThan(0);
    assertThat(image.getHeight()).isGreaterThan(0);
    assertThat(decodeQrPayload(image)).isEqualTo(otpAuthUri);
  }

  @Test
  void generateRejectsNullUri() {
    ZxingQrPngGenerator generator = new ZxingQrPngGenerator();

    assertThatThrownBy(() -> generator.generate(null))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("URI");
  }

  private String decodeQrPayload(BufferedImage image) throws Exception {
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
    return new MultiFormatReader().decode(bitmap).getText();
  }
}
