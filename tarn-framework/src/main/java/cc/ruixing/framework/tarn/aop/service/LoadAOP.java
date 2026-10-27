package cc.ruixing.framework.tarn.aop.service;


import cc.ruixing.framework.tarn.util.xml.XMLElement;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by dfr on 15-3-1.
 */
public interface LoadAOP {

    /**
     * 从目录加载映射
     * @param dirs
     * @param xmlElement
     * */
    public boolean load(File dirs,XMLElement xmlElement,XMLElement writerXml);
    /**
     * 从指定类集合加载映射
     * @param packageAndClassList
     * @param xmlElement
     * */
    public boolean load(List<String> packageAndClassList,XMLElement xmlElement,XMLElement writerXml);
    public boolean load(Set<Class<?>> packageAndClassList, XMLElement xmlElement, XMLElement writerXml);
}
