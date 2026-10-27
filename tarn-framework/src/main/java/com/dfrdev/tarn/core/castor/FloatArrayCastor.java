package com.dfrdev.tarn.core.castor;

public class FloatArrayCastor extends AbstractCastor {
    private static FloatArrayCastor singleton = new FloatArrayCastor();

    public static FloatArrayCastor getInstance() {
        return singleton;
    }

    public boolean canCast(Class<?> type) {
        return (Float.class == type) || (Float.TYPE == type);
    }

    public Object cast(Object obj, Class<?> type) throws CastorNotFoundException {
        if (obj == null) {
            return null;
        }
        if ((obj instanceof float[]))
            return obj;
        if (obj.getClass().isArray()) {
            Object[] os = (Object[]) obj;
            float[] arr = new float[os.length];
            for (int i = 0; i < os.length; i++) {
                arr[i] = ((Float) FloatCastor.getInstance().cast(os[i], type)).floatValue();
            }
            return arr;
        }
        if (((obj instanceof String)) && (DoubleArrayCastor.isNumberArray(obj.toString()))) {
            double[] ds = DoubleArrayCastor.toDoubleArray(obj.toString());
            float[] arr = new float[ds.length];
            for (int i = 0; i < ds.length; i++) {
                arr[i] = new Double(ds[i]).floatValue();
            }
            return arr;
        }
        return new float[]{((Float) FloatCastor.getInstance().cast(obj, type)).floatValue()};
    }
}

