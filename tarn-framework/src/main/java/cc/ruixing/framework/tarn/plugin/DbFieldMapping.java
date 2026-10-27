
package cc.ruixing.framework.tarn.plugin;

import java.util.HashMap;

public interface DbFieldMapping {

    /**
     *
     * */
    String invokeFieldName(Object targetObject, String dbFieldName);

    boolean invokeFieldORMToEntity(Object targetObject, String dbFieldName, Object value, Integer columnType, HashMap aopRecursiveField);
}


