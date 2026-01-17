package com.kuneiform.infraestructure.adapter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kuneiform.domain.port.QrCodeService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation of QR code service using the ZXing (Zebra Crossing) library.
 *
 * <p>Generates PNG images of QR codes for TOTP URI data.
 */
@Slf4j
@Component
public class ZxingQrCodeService implements QrCodeService {

  private static final String IMAGE_FORMAT = "PNG";

  @Override
  public byte[] generateQrCode(String data, int width, int height)
      throws QrCodeGenerationException {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("QR code data cannot be null or empty");
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
          "QR code dimensions must be positive (width: " + width + ", height: " + height + ")");
    }

    try {
      Map<EncodeHintType, Object> hints = new HashMap<>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // 15% tolerance
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.MARGIN, 1);

      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);
        byte[] qrCodeBytes = outputStream.toByteArray();
        log.debug("Generated QR code image ({}x{}, {} bytes)", width, height, qrCodeBytes.length);
        return qrCodeBytes;
      }

    } catch (WriterException e) {
      log.error("Failed to encode QR code data", e);
      throw new QrCodeGenerationException("Failed to encode QR code", e);
    } catch (IOException e) {
      log.error("Failed to write QR code image", e);
      throw new QrCodeGenerationException("Failed to write QR code image", e);
    }
  }
}
