package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ApkFileValidatorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ApkFileValidator validator = new ApkFileValidator();

    @Test
    public void acceptsValidZipWithMatchingSha256() throws IOException, NoSuchAlgorithmException {
        File apkFile = writeApk(new byte[]{0x50, 0x4B, 0x03, 0x04}, 2 * 1024 * 1024);
        String expectedSha256 = sha256Of(apkFile);

        Assert.assertNull(validator.validate(apkFile, expectedSha256, 0L));
    }

    @Test
    public void acceptsValidZipWhenExpectedShaIsUnavailable() throws IOException {
        File apkFile = writeApk(new byte[]{0x50, 0x4B, 0x03, 0x04}, 2 * 1024 * 1024);

        Assert.assertNull(validator.validate(apkFile, null, 0L));
    }

    @Test
    public void rejectsFileSmallerThanMinimumSize() throws IOException {
        File apkFile = writeApk(new byte[]{0x50, 0x4B, 0x03, 0x04}, 1024);

        String reason = validator.validate(apkFile, null, 0L);

        Assert.assertNotNull(reason);
        Assert.assertTrue(reason.contains("minimum"));
    }

    @Test
    public void rejectsFileWithoutZipMagic() throws IOException {
        File apkFile = writeApk(new byte[]{0x00, 0x01, 0x02, 0x03}, 2 * 1024 * 1024);

        String reason = validator.validate(apkFile, null, 0L);

        Assert.assertNotNull(reason);
        Assert.assertTrue(reason.contains("ZIP"));
    }

    @Test
    public void rejectsFileWithSha256Mismatch() throws IOException {
        File apkFile = writeApk(new byte[]{0x50, 0x4B, 0x03, 0x04}, 2 * 1024 * 1024);

        String reason = validator.validate(apkFile,
            "0000000000000000000000000000000000000000000000000000000000000000", 0L);

        Assert.assertNotNull(reason);
        Assert.assertTrue(reason.contains("SHA-256 mismatch"));
    }

    @Test
    public void rejectsFileWhenLengthDoesNotMatchExpectedSize() throws IOException {
        File apkFile = writeApk(new byte[]{0x50, 0x4B, 0x03, 0x04}, 2 * 1024 * 1024);

        String reason = validator.validate(apkFile, null, 3 * 1024 * 1024);

        Assert.assertNotNull(reason);
        Assert.assertTrue(reason.contains("expected size"));
    }

    @Test
    public void rejectsMissingFile() {
        File missing = new File(temporaryFolder.getRoot(), "missing.apk");

        String reason = validator.validate(missing, null, 0L);

        Assert.assertNotNull(reason);
        Assert.assertTrue(reason.contains("missing"));
    }

    private File writeApk(byte[] header, int totalSize) throws IOException {
        File apkFile = temporaryFolder.newFile("termux-app_universal_" + totalSize + "_" + header.length + ".apk");
        try (OutputStream outputStream = new FileOutputStream(apkFile)) {
            outputStream.write(header);
            int remaining = totalSize - header.length;
            if (remaining > 0) {
                outputStream.write(new byte[remaining]);
            }
        }
        return apkFile;
    }

    private String sha256Of(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
        byte[] hashBytes = digest.digest(content);
        StringBuilder builder = new StringBuilder();
        for (byte value : hashBytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
