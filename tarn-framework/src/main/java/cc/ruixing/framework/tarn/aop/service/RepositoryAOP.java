package cc.ruixing.framework.tarn.aop.service;

import cc.ruixing.framework.tarn.aop.RepositoryInject;
import cc.ruixing.framework.tarn.aop.annotation.ERepository;
import cc.ruixing.framework.tarn.collection.Mapx;
import cc.ruixing.framework.tarn.util.ClassUtil;
import cc.ruixing.framework.tarn.util.LogUtil;
import cc.ruixing.framework.tarn.util.xml.XMLElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by dfr on 15-3-1.
 */
public class RepositoryAOP implements LoadAOP {
    /**
     * 从目录加载映射
     *
     * @param dirs
     */
    @Override
    public boolean load(File dirs, XMLElement xmlElement,XMLElement writerXml) {
        ArrayList<String> list = new ArrayList<String>();
        for (File c : dirs.listFiles()) {
            String fn = c.getName();
            list.add(fn);
        }
        return process(list, xmlElement,writerXml, false);
    }

    /**
     * 从指定类集合加载映射
     *
     * @param packageAndClassList
     */
    @Override
    public boolean load(List<String> packageAndClassList, XMLElement xmlElement,XMLElement writerXml) {
        return process(packageAndClassList, xmlElement,writerXml, true);
    }

    @Override
    public boolean load(Set<Class<?>> packageAndClassList, XMLElement xmlElement, XMLElement writerXml) {
        Mapx mapx = xmlElement.getAttributes();
        for (Class<?> o : packageAndClassList) {
            if ( o.getName().indexOf("$") == -1) {
                boolean aann = o.isAnnotationPresent(ERepository.class);
                if (aann) {
                    ERepository ann = (ERepository) o.getAnnotation(ERepository.class);
                    RepositoryInject.setRepositoryImpl(ann.value(), o.getName());
                    writerXml.addElement("service").addAttribute("class", o.getName());
                    LogUtil.info("DAO Scanning success >> " + ClassUtil.getShortClassName(o.getName()));
                }
            }
        }
        return true;
    }

    private boolean process(List<String> packageAndClassList, XMLElement xmlElement, XMLElement writerXml, boolean isCompletePackageName) {
        Mapx mapx = xmlElement.getAttributes();
        for (String classAllName : packageAndClassList) {
            boolean status = isCompletePackageName ? true : (classAllName.indexOf(".class") != -1);
            if (status && classAllName.indexOf("$") == -1) {
                String pcn = (isCompletePackageName) ? classAllName : mapx.getString("package").replace('/', '.') + "." + classAllName.substring(0, classAllName.lastIndexOf("."));
                Class o = null;
                try {
                    o = Class.forName(pcn);
                    boolean aann = o.isAnnotationPresent(ERepository.class);
                    if (aann) {
                        ERepository ann = (ERepository) o.getAnnotation(ERepository.class);
                        RepositoryInject.setRepositoryImpl(ann.value(), pcn);
                        writerXml.addElement("service").addAttribute("class", pcn);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}
