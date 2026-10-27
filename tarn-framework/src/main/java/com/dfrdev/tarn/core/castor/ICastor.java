package com.dfrdev.tarn.core.castor;


import java.io.IOException;

public abstract interface ICastor {
    public abstract boolean canCast(Class<?> paramClass);

    public abstract Object cast(Object paramObject, Class<?> paramClass) throws CastorNotFoundException, IOException;
}

