package com.dfrdev.tarn.core.castor;

import com.dfrdev.tarn.util.text.StringUtil;
import com.dfrdev.tarn.json.JsonKit;

import com.sun.xml.internal.fastinfoset.util.StringArray; // TODO(Tarn 2.0+迁移): JDK 内部 API，升级 JDK9+ 前需替换为 String[] 并同步修改 StringArrayCastor

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;

public class CastorService {
    private boolean innerCastorLoaded = false;

    public static CastorService getInstance() {
        return new CastorService();
    }

    public static Object toType(HttpServletRequest obj, Class<?> type) throws CastorNotFoundException, IOException {
        Object body = RequestBodyCastor.getInstance().cast(obj, type);
        if (body == null) {
            return null;
        }
        if (type == String.class) {
            return body.toString();
        } else {
            if (StringUtil.isEmpty(body.toString())) {
                return null;
            }
            // Tarn 2.0+: JSON 解析统一收口至 JsonKit（内部已做 safeMode 加固，便于后续整体升级引擎）
            return JsonKit.fromJson(body.toString(), type);
        }

    }

    public static Object toType(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            // 空值按目标类型返回对应默认值，避免 Long/Float/Double 被注入 Integer 造成类型不匹配
            if (type == Integer.class || type == Integer.TYPE) {
                return Integer.valueOf(0);
            }
            if (type == Long.class || type == Long.TYPE) {
                return Long.valueOf(0L);
            }
            if (type == Float.class || type == Float.TYPE) {
                return Float.valueOf(0F);
            }
            if (type == Double.class || type == Double.TYPE) {
                return Double.valueOf(0D);
            }
            if (type == Boolean.class || type == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            return obj;
        }


        if (type.isInstance(obj)) {
            return obj;
        }
        if (type == String.class) {
            return obj.toString();
        }
        if ((type == Integer.class) || (type == Integer.TYPE))
            return IntCastor.getInstance().cast(obj, type);
        if ((type == Long.class) || (type == Long.TYPE))
            return LongCastor.getInstance().cast(obj, type);
        if ((type == Float.class) || (type == Float.TYPE))
            return FloatCastor.getInstance().cast(obj, type);
        if ((type == Double.class) || (type == Double.TYPE))
            return DoubleCastor.getInstance().cast(obj, type);
        if ((type == Boolean.class) || (type == Boolean.TYPE))
            return BooleanCastor.getInstance().cast(obj, type);
        if (type == Date.class)
            return DateCastor.getInstance().cast(obj, type);
        if (type == java.sql.Date.class)
            return DateCastor.getInstance().cast(obj, type);
        if ((type == StringArray.class))
            return StringArrayCastor.getInstance().cast(obj, type);
        if ((type == long.class) || (type == Long.class))
            return LongArrayCastor.getInstance().cast(obj, type);
        if ((type == float.class) || (type == Float.class))
            return FloatArrayCastor.getInstance().cast(obj, type);
        if ((type == double.class) || (type == Double.class))
            return DoubleArrayCastor.getInstance().cast(obj, type);
        if ((type == boolean.class) || (type == Boolean.class)) {
            return BooleanArrayCastor.getInstance().cast(obj, type);
        }
//     for (ICastor castor : getInstance().getAll()) {
//       if (castor.canCast(type)) {
//         return castor.cast(obj, type);
//       }
//     }

        throw new CastorNotFoundException(type.getClass().getName());
    }
//
//    private void init() {
//        if (!this.innerCastorLoaded) {
//            register(IntCastor.getInstance());
//            register(LongCastor.getInstance());
//            register(FloatCastor.getInstance());
//            register(DoubleCastor.getInstance());
//            register(DateCastor.getInstance());
//            register(IntArrayCastor.getInstance());
//            register(LongArrayCastor.getInstance());
//            register(FloatArrayCastor.getInstance());
//            register(DoubleArrayCastor.getInstance());
//            register(BooleanCastor.getInstance());
//            register(BooleanArrayCastor.getInstance());
//            register(StringArrayCastor.getInstance());
//            register(GenricArrayCastor.getInstance());
//            this.innerCastorLoaded = true;
//        }
//    }
//
//    public void register(IExtendItem item) {
//        init();
////     super.register(item);
//    }
}

