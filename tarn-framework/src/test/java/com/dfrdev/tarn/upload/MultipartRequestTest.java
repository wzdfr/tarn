package com.dfrdev.tarn.upload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * MultipartRequest 保存目录安全解析测试（S2）。
 */
public class MultipartRequestTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void rejectsAbsolutePath() {
        String root = folder.getRoot().getAbsolutePath();
        assertRejected(root, "/opt/app/webroot");
    }

    @Test
    public void rejectsParentTraversal() {
        String root = folder.getRoot().getAbsolutePath();
        assertRejected(root, "../evil");
        assertRejected(root, "upload/../../evil");
    }

    @Test
    public void rejectsWindowsDrive() {
        String root = folder.getRoot().getAbsolutePath();
        assertRejected(root, "C:\\evil");
    }

    @Test
    public void resolvesRelativeDirectoryInsideRoot() {
        String root = folder.getRoot().getAbsolutePath();
        assertEquals(new File(root, "sub/dir").getAbsolutePath(),
                MultipartRequest.resolveSaveDirectory(root, "sub/dir"));
        assertEquals(new File(root).getAbsolutePath(),
                MultipartRequest.resolveSaveDirectory(root, ""));
    }

    private void assertRejected(String root, String dir) {
        try {
            MultipartRequest.resolveSaveDirectory(root, dir);
            throw new AssertionError("应拒绝非法保存目录: " + dir);
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
