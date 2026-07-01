package com.almis.awe.service.totp;

import com.almis.awe.exception.AWERuntimeException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class ZxingQrPngGenerator {

  private static final int QR_WIDTH = 250;
  private static final int QR_HEIGHT = 250;
  private static final String PNG_FORMAT = "PNG";

  public byte[] generate(URI uri) {
    if (uri == null) {
      throw new AWERuntimeException("QR URI must not be null");
    }

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      BitMatrix matrix = new QRCodeWriter().encode(uri.toString(), BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);
      MatrixToImageWriter.writeToStream(matrix, PNG_FORMAT, outputStream);
      return outputStream.toByteArray();
    } catch (WriterException | IOException exc) {
      throw new AWERuntimeException("Unable to generate QR PNG", exc);
    }
  }
}
