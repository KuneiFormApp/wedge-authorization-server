package com.kuneiform.domain.port;

/**
 * Port interface for QR code generation.
 *
 * <p>Defines the contract for generating QR code images from data strings.
 */
public interface QrCodeService {

  /**
   * Generates a QR code image from the provided data.
   *
   * @param data the data to encode in the QR code (e.g., otpauth:// URI)
   * @param width the width of the QR code image in pixels
   * @param height the height of the QR code image in pixels
   * @return byte array containing the PNG image data
   * @throws QrCodeGenerationException if QR code generation fails
   */
  byte[] generateQrCode(String data, int width, int height) throws QrCodeGenerationException;

  /** Exception thrown when QR code generation fails. */
  class QrCodeGenerationException extends Exception {
    public QrCodeGenerationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
