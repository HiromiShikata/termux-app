package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class ApkFileValidator {

    private static final long MINIMUM_APK_SIZE_BYTES = 1024L * 1024L;

    private static final byte[] ZIP_LOCAL_FILE_HEADER_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    @Nullable
    public String validate(File file, @Nullable String expectedSha256, long expectedSizeBytes) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "file is missing";
        }
        long length = file.length();
        if (length < MINIMUM_APK_SIZE_BYTES) {
            return "file is smaller than the minimum APK size (" + length + " bytes)";
        }
        if (expectedSizeBytes > 0 && length != expectedSizeBytes) {
            return "file length " + length + " does not match expected size " + expectedSizeBytes;
        }
        String zipMagicReason = validateZipMagic(file);
        if (zipMagicReason != null) {
            return zipMagicReason;
        }
        if (expectedSha256 != null && !expectedSha256.isEmpty()) {
            return validateSha256(file, expectedSha256);
        }
        return null;
    }

    @Nullable
    private String validateZipMagic(File file) {
        byte[] header = new byte[ZIP_LOCAL_FILE_HEADER_MAGIC.length];
        try (InputStream inputStream = new FileInputStream(file)) {
            int totalRead = 0;
            while (totalRead < header.length) {
                int read = inputStream.read(header, totalRead, header.length - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            if (totalRead < header.length) {
                return "file is too short to contain a ZIP header";
            }
        } catch (IOException exception) {
            return "failed to read file header: " + messageOf(exception);
        }
        for (int index = 0; index < ZIP_LOCAL_FILE_HEADER_MAGIC.length; index++) {
            if (header[index] != ZIP_LOCAL_FILE_HEADER_MAGIC[index]) {
                return "file does not start with the ZIP local-file-header magic bytes";
            }
        }
        return null;
    }

    @Nullable
    private String validateSha256(File file, String expectedSha256) {
        String actualSha256;
        try {
            actualSha256 = computeSha256(file);
        } catch (IOException | NoSuchAlgorithmException exception) {
            return "failed to compute SHA-256: " + messageOf(exception);
        }
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            return "SHA-256 mismatch (expected " + expectedSha256.toLowerCase(Locale.ROOT)
                + ", actual " + actualSha256 + ")";
        }
        return null;
    }

    private String computeSha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                digest.update(chunk, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private static String messageOf(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.toString();
    }
}
