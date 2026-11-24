package com.dfrdev.tarn.upload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 上传文件安全校验测试（S9）。
 */
public class UploadFileSecurityTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void rejectsScriptExtension() throws Exception {
        assertFalse(isSafe("a.jsp", new byte[]{1, 2, 3, 4}));
        assertFalse(isSafe("a.jpg.jsp", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertFalse(isSafe("a.html", new byte[]{'<', 'h', 't', 'm'}));
    }

    @Test
    public void rejectsExtensionNotInWhitelist() throws Exception {
        assertFalse(isSafe("a.exe", new byte[]{'M', 'Z', 0, 0}));
        assertFalse(isSafe("a.unknown", new byte[]{1, 2, 3, 4}));
    }

    @Test
    public void rejectsFakeImageByMagicNumber() throws Exception {
        assertFalse(isSafe("a.jpg", new byte[]{1, 2, 3, 4}));
        assertFalse(isSafe("a.png", new byte[]{1, 2, 3, 4}));
    }

    @Test
    public void acceptsValidImage() throws Exception {
        assertTrue(isSafe("a.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0}));
        assertTrue(isSafe("a.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
    }

    @Test
    public void acceptsTextWithoutMagic() throws Exception {
        assertTrue(isSafe("a.txt", new byte[]{'h', 'e', 'l', 'l', 'o'}));
    }

    private boolean isSafe(String originalName, byte[] content) throws Exception {
        File file = folder.newFile("stored_" + System.nanoTime());
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(content);
        } finally {
            out.close();
        }
        UploadFile uploadFile = new UploadFile("file", folder.getRoot().getAbsolutePath(),
                file.getName(), originalName, "application/octet-stream");
        return MultipartRequest.isSafeFile(uploadFile);
    }
}
