package com.dfrdev.tarn.render;

import com.dfrdev.tarn.core.Config;
import com.dfrdev.tarn.util.FileUtil;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by dfr on 14-11-6.
 */
public class FreeMarkerMultiDirFactory {

    private transient static Vector<TemplateLoader> multiDirPool = null;

    static {
        multiDirPool = new Vector();
    }


    public static boolean register(File file) {
        try {
            multiDirPool.add(new FileTemplateLoader(file));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean register(String dirPath) {
        return register(dirPath, false);
    }

    public static boolean register(Class t, String path) {
        multiDirPool.add(new ClassTemplateLoader(t, path));

        return true;
    }

    public static boolean register(String path, boolean contextRealPath) {
        if (contextRealPath) {
            path = Config.getContextRealPath() + path;
            if (!FileUtil.exists(path)) {
                throw new RenderException("path Not exist");
            }
        } else {
            if (!FileUtil.exists(path)) {
                throw new RenderException("path Not exist");
            }
        }

        return register(new File(FileUtil.normalizePath(path)));
    }

    protected static Vector<TemplateLoader> getMultiDirPool() {
        return multiDirPool;
    }

}
