package com.dfrdev.tarn.render;

import com.dfrdev.tarn.util.PathKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * FileRender 下载路径安全校验测试（S1）。
 */
public class FileRenderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String originalWebRoot;

    @Before
    public void setUp() throws Exception {
        originalWebRoot = PathKit.getWebRootPath();
        PathKit.setWebRootPath(folder.getRoot().getAbsolutePath());
    }

    @After
    public void tearDown() {
        if (originalWebRoot != null) {
            PathKit.setWebRootPath(originalWebRoot);
        }
    }

    @Test
    public void rejectsParentTraversal() {
        assertNull(FileRender.resolveDownloadFile("../../etc/passwd"));
        assertNull(FileRender.resolveDownloadFile("a/../../etc/passwd"));
    }

    @Test
    public void rejectsSensitiveDirectories() {
        assertNull(FileRender.resolveDownloadFile("/WEB-INF/web.xml"));
        assertNull(FileRender.resolveDownloadFile("/META-INF/context.xml"));
    }

    @Test
    public void resolvesRelativePathInsideWebRoot() {
        File expected = new File(folder.getRoot(), "uploads/a.txt");
        assertEquals(expected.getAbsolutePath(), FileRender.resolveDownloadFile("/uploads/a.txt").getAbsolutePath());
        assertEquals(expected.getAbsolutePath(), FileRender.resolveDownloadFile("uploads/a.txt").getAbsolutePath());
    }
}
