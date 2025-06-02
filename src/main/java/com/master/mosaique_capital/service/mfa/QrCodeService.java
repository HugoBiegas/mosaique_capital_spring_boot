// com/master/mosaique_capital/service/mfa/QrCodeService.java
package com.master.mosaique_capital.service.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.master.mosaique_capital.exception.QrCodeGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeService {

    @Value("${app.mfa.issuer:MosaiqueCapital}")
    private String issuer;

    private static final int QR_CODE_WIDTH = 250;
    private static final int QR_CODE_HEIGHT = 250;

    /**
     * Génère un QR code sous forme de tableau d'octets pour une URL TOTP
     *
     * @param totpUrl L'URL TOTP formatée
     * @return Le QR code sous forme de tableau d'octets (format PNG)
     * @throws QrCodeGenerationException Si la génération échoue
     */
    public byte[] generateQrCodeImage(String totpUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    totpUrl,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_WIDTH,
                    QR_CODE_HEIGHT,
                    hints
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();

            log.debug("QR code généré avec succès pour l'URL TOTP");
            return qrCodeBytes;

        } catch (WriterException | IOException e) {
            log.error("Erreur lors de la génération du QR code: {}", e.getMessage(), e);
            throw new QrCodeGenerationException("Impossible de générer le QR code", e);
        }
    }

    /**
     * Formate une URL TOTP selon les spécifications RFC 6238
     *
     * @param username Le nom d'utilisateur
     * @param secret Le secret TOTP en base32
     * @return L'URL TOTP formatée
     */
    public String formatTotpUrl(String username, String secret) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide");
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Le secret ne peut pas être vide");
        }

        // Format: otpauth://totp/Issuer:Username?secret=SECRET&issuer=Issuer
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer,
                username,
                secret,
                issuer
        );
    }

    /**
     * Valide les paramètres d'entrée pour la génération de QR code
     *
     * @param username Le nom d'utilisateur
     * @param secret Le secret TOTP
     * @throws IllegalArgumentException Si les paramètres sont invalides
     */
    public void validateQrCodeParameters(String username, String secret) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur est requis pour générer le QR code");
        }
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Le secret TOTP est requis pour générer le QR code");
        }
        if (secret.length() < 16) {
            throw new IllegalArgumentException("Le secret TOTP doit faire au moins 16 caractères");
        }
    }
}